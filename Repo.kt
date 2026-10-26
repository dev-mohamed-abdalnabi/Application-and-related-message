package com.mohamed.phone.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class ContactItem(
    val id: Long, val lookupKey: String, val name: String,
    val numbers: List<String>, val photo: String?, val starred: Boolean
)

data class CallEntry(
    val ids: List<Long>, val number: String, val type: Int, val date: Long,
    val duration: Long, val count: Int, val cachedName: String?
)

data class ThreadItem(val threadId: Long, val address: String, val snippet: String, val date: Long, val unread: Int)

data class SmsItem(val id: Long, val address: String, val body: String, val date: Long, val type: Int)

private class ContactAcc(val id: Long, val lookup: String, val name: String, val photo: String?, val starred: Boolean) {
    val numbers = ArrayList<String>()
}

private class ThreadAcc(val id: Long, val address: String, val snippet: String, val date: Long) {
    var unread = 0
}

object Repo {
    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = HashMap<String, Job>()
    private val nameCache = ConcurrentHashMap<String, String>()

    val contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val calls = MutableStateFlow<List<CallEntry>>(emptyList())
    val threads = MutableStateFlow<List<ThreadItem>>(emptyList())

    @Volatile private var byNumber: Map<String, ContactItem> = emptyMap()
    private var obsContacts = false
    private var obsCalls = false
    private var obsSms = false

    fun init(ctx: Context) { app = ctx.applicationContext }

    fun has(perm: String): Boolean =
        ::app.isInitialized && ContextCompat.checkSelfPermission(app, perm) == PackageManager.PERMISSION_GRANTED

    fun key(number: String): String {
        val d = number.filter { it.isDigit() }
        return if (d.length >= 9) d.takeLast(9) else d
    }

    // ---------------------------------------------------------------- start / observers
    fun start() {
        if (!::app.isInitialized) return
        if (has(Manifest.permission.READ_CONTACTS) && !obsContacts) {
            obsContacts = observe(ContactsContract.Contacts.CONTENT_URI, "c") { loadContacts() }
        }
        if (has(Manifest.permission.READ_CALL_LOG) && !obsCalls) {
            obsCalls = observe(CallLog.Calls.CONTENT_URI, "l") { loadCalls() }
        }
        if (has(Manifest.permission.READ_SMS) && !obsSms) {
            obsSms = observe(Telephony.Sms.CONTENT_URI, "s") { loadThreads() }
        }
        reloadAll()
    }

    fun reloadAll() {
        schedule("c", 0) { loadContacts() }
        schedule("l", 0) { loadCalls() }
        schedule("s", 0) { loadThreads() }
    }

    private fun observe(uri: Uri, k: String, load: () -> Unit): Boolean = try {
        val o = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { schedule(k, 350, load) }
        }
        app.contentResolver.registerContentObserver(uri, true, o)
        true
    } catch (e: Exception) { false }

    @Synchronized
    private fun schedule(k: String, wait: Long, load: () -> Unit) {
        jobs[k]?.cancel()
        jobs[k] = scope.launch { if (wait > 0) delay(wait); load() }
    }

    // ---------------------------------------------------------------- contacts
    private fun loadContacts() {
        if (!has(Manifest.permission.READ_CONTACTS)) return
        val acc = LinkedHashMap<Long, ContactAcc>()
        try {
            app.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf("contact_id", "lookup", "display_name", "data1", "photo_thumb_uri", "starred"),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val num = c.getString(3) ?: continue
                    val a = acc.getOrPut(id) {
                        ContactAcc(id, c.getString(1) ?: "", c.getString(2) ?: "", c.getString(4), c.getInt(5) == 1)
                    }
                    if (a.numbers.none { key(it) == key(num) }) a.numbers.add(num)
                }
            }
        } catch (e: Exception) { return }

        val collator = Collator.getInstance(Locale("ar"))
        val list = acc.values.map {
            ContactItem(it.id, it.lookup, it.name.ifBlank { it.numbers.firstOrNull() ?: "" }, it.numbers.toList(), it.photo, it.starred)
        }.sortedWith { a, b -> collator.compare(a.name, b.name) }

        val map = HashMap<String, ContactItem>()
        for (ct in list) for (n in ct.numbers) map.putIfAbsent(key(n), ct)
        byNumber = map
        nameCache.clear()
        contacts.value = list
    }

    fun lookup(number: String): ContactItem? = if (number.isBlank()) null else byNumber[key(number)]

    /** اسم الرقم (من الذاكرة، وإلا استعلام مباشر) */
    fun nameFor(number: String): String? {
        lookup(number)?.let { return it.name }
        if (number.isBlank() || !has(Manifest.permission.READ_CONTACTS)) return null
        val k = key(number)
        nameCache[k]?.let { return if (it.isEmpty()) null else it }
        var name = ""
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            app.contentResolver.query(uri, arrayOf("display_name"), null, null, null)?.use {
                if (it.moveToFirst()) name = it.getString(0) ?: ""
            }
        } catch (e: Exception) { }
        nameCache[k] = name
        return if (name.isEmpty()) null else name
    }

    fun setStarred(c: ContactItem, star: Boolean) {
        scope.launch {
            try {
                val v = ContentValues().apply { put("starred", if (star) 1 else 0) }
                app.contentResolver.update(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.id), v, null, null)
            } catch (e: Exception) { }
        }
    }

    fun deleteContact(c: ContactItem): Boolean = try {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, c.lookupKey)
        app.contentResolver.delete(uri, null, null) > 0
    } catch (e: Exception) { false }

    // ---------------------------------------------------------------- calls
    private fun loadCalls() {
        if (!has(Manifest.permission.READ_CALL_LOG)) return
        val out = ArrayList<CallEntry>()
        try {
            app.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf("_id", "number", "type", "date", "duration", "name"),
                null, null, "date DESC"
            )?.use { c ->
                while (c.moveToNext() && out.size < 700) {
                    val id = c.getLong(0)
                    val num = c.getString(1) ?: ""
                    val type = c.getInt(2)
                    val last = out.lastOrNull()
                    if (last != null && last.number == num && last.type == type) {
                        out[out.size - 1] = last.copy(ids = last.ids + id, count = last.count + 1)
                    } else {
                        out.add(CallEntry(listOf(id), num, type, c.getLong(3), c.getLong(4), 1, c.getString(5)))
                    }
                }
            }
        } catch (e: Exception) { return }
        calls.value = out
    }

    fun deleteCalls(ids: List<Long>): Boolean {
        if (ids.isEmpty()) return true
        return try {
            app.contentResolver.delete(CallLog.Calls.CONTENT_URI, "_id IN (${ids.joinToString(",")})", null)
            true
        } catch (e: Exception) { false }
    }

    fun clearCallLog(): Boolean = try {
        app.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null); true
    } catch (e: Exception) { false }

    // ---------------------------------------------------------------- sms
    private fun loadThreads() {
        if (!has(Manifest.permission.READ_SMS)) return
        val map = LinkedHashMap<Long, ThreadAcc>()
        try {
            app.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf("thread_id", "address", "body", "date", "type", "read"),
                null, null, "date DESC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val t = c.getLong(0)
                    val a = map.getOrPut(t) { ThreadAcc(t, c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3)) }
                    if (c.getInt(4) == 1 && c.getInt(5) == 0) a.unread++
                }
            }
        } catch (e: Exception) { return }
        threads.value = map.values.map { ThreadItem(it.id, it.address, it.snippet, it.date, it.unread) }
    }

    fun messages(threadId: Long, address: String): Flow<List<SmsItem>> = callbackFlow {
        val ps = this
        val o = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                ps.launch(Dispatchers.IO) { trySend(queryMessages(threadId, address)) }
            }
        }
        app.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, o)
        launch(Dispatchers.IO) { trySend(queryMessages(threadId, address)) }
        awaitClose { app.contentResolver.unregisterContentObserver(o) }
    }

    private fun queryMessages(threadId: Long, address: String): List<SmsItem> {
        val out = ArrayList<SmsItem>()
        if (!has(Manifest.permission.READ_SMS)) return out
        val sel: String
        val args: Array<String>
        if (threadId > 0) { sel = "thread_id=?"; args = arrayOf(threadId.toString()) }
        else { sel = "address LIKE ?"; args = arrayOf("%" + key(address)) }
        try {
            app.contentResolver.query(
                Telephony.Sms.CONTENT_URI, arrayOf("_id", "address", "body", "date", "type"),
                sel, args, "date ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    out.add(SmsItem(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3), c.getInt(4)))
                }
            }
        } catch (e: Exception) { }
        return out
    }

    fun threadIdFor(address: String): Long = try {
        Telephony.Threads.getOrCreateThreadId(app, address)
    } catch (e: Exception) { -1L }

    fun markRead(threadId: Long) {
        if (threadId <= 0) return
        scope.launch {
            try {
                val v = ContentValues().apply { put("read", 1); put("seen", 1) }
                app.contentResolver.update(Telephony.Sms.CONTENT_URI, v, "thread_id=? AND read=0", arrayOf(threadId.toString()))
            } catch (e: Exception) { }
        }
    }

    fun deleteThread(threadId: Long): Boolean = try {
        app.contentResolver.delete(Telephony.Sms.CONTENT_URI, "thread_id=?", arrayOf(threadId.toString())) >= 0
    } catch (e: Exception) { false }

    fun deleteMessage(id: Long): Boolean = try {
        app.contentResolver.delete(Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, id.toString()), null, null) >= 0
    } catch (e: Exception) { false }

    // ---------------------------------------------------------------- blocked numbers
    fun blocked(): List<Pair<Long, String>> {
        val out = ArrayList<Pair<Long, String>>()
        try {
            app.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ID, BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                null, null, null
            )?.use { c -> while (c.moveToNext()) out.add(c.getLong(0) to (c.getString(1) ?: "")) }
        } catch (e: Exception) { }
        return out
    }

    fun block(number: String): Boolean = try {
        val v = ContentValues().apply { put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number) }
        app.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, v) != null
    } catch (e: Exception) { false }

    fun unblock(id: Long): Boolean = try {
        app.contentResolver.delete(ContentUris.withAppendedId(BlockedNumberContract.BlockedNumbers.CONTENT_URI, id), null, null) > 0
    } catch (e: Exception) { false }

    fun isBlocked(number: String): Boolean = try {
        BlockedNumberContract.isBlocked(app, number)
    } catch (e: Exception) { false }
}
