package com.mohamed.phone.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import com.mohamed.phone.data.ContactItem

object Intents {
    fun addContact(ctx: Context, number: String? = null) {
        val i = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            if (!number.isNullOrEmpty()) putExtra(ContactsContract.Intents.Insert.PHONE, number)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { ctx.startActivity(i) } catch (e: Exception) { toast(ctx, "مفيش تطبيق جهات اتصال يضيف") }
    }

    fun editContact(ctx: Context, c: ContactItem) {
        val uri = ContactsContract.Contacts.getLookupUri(c.id, c.lookupKey)
        val i = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
            putExtra("finishActivityOnSaveCompleted", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try { ctx.startActivity(i) } catch (e: Exception) { toast(ctx, "مش قادر أفتح التعديل") }
    }

    fun copy(ctx: Context, text: String) {
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("text", text))
        toast(ctx, "اتنسخ")
    }
}
