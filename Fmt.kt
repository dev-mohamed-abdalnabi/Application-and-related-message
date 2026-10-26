package com.mohamed.phone.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Fmt {
    private val loc: Locale = Locale.forLanguageTag("ar-EG-u-nu-latn")

    fun time(ms: Long): String = SimpleDateFormat("h:mm a", loc).format(Date(ms))

    /** اليوم -> الساعة، أمس، اسم اليوم خلال أسبوع، وإلا التاريخ */
    fun day(ms: Long): String {
        val now = Calendar.getInstance()
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val startToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayMs = 24L * 60 * 60 * 1000
        return when {
            ms >= startToday -> time(ms)
            ms >= startToday - dayMs -> "أمس"
            ms >= startToday - 6 * dayMs -> SimpleDateFormat("EEEE", loc).format(Date(ms))
            c.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> SimpleDateFormat("d MMM", loc).format(Date(ms))
            else -> SimpleDateFormat("d/M/yyyy", loc).format(Date(ms))
        }
    }

    fun full(ms: Long): String = SimpleDateFormat("EEEE d MMMM · h:mm a", loc).format(Date(ms))

    fun dayHeader(ms: Long): String {
        val startToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayMs = 24L * 60 * 60 * 1000
        return when {
            ms >= startToday -> "النهارده"
            ms >= startToday - dayMs -> "أمس"
            else -> SimpleDateFormat("EEEE d MMMM", loc).format(Date(ms))
        }
    }

    fun sameDay(a: Long, b: Long): Boolean {
        val x = Calendar.getInstance().apply { timeInMillis = a }
        val y = Calendar.getInstance().apply { timeInMillis = b }
        return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.DAY_OF_YEAR) == y.get(Calendar.DAY_OF_YEAR)
    }

    /** مدة مكالمة: ٣ د ٢٠ ث */
    fun duration(sec: Long): String {
        if (sec <= 0) return "لم يتم الرد"
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return buildString {
            if (h > 0) append("$h س ")
            if (m > 0) append("$m د ")
            if (s > 0 || (h == 0L && m == 0L)) append("$s ث")
        }.trim()
    }

    /** عدّاد المكالمة 00:00 */
    fun clock(sec: Long): String {
        val s: Long = if (sec < 0) 0L else sec
        val h = s / 3600
        val m = (s % 3600) / 60
        val ss = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, ss) else String.format(Locale.US, "%02d:%02d", m, ss)
    }

    fun initials(name: String): String {
        val t = name.trim()
        if (t.isEmpty()) return ""
        val parts = t.split(" ").filter { it.isNotEmpty() }
        val first = parts.first().first()
        if (!first.isLetter()) return ""
        val second = if (parts.size > 1 && parts[1].first().isLetter()) parts[1].first().toString() else ""
        return (first.toString() + second).uppercase()
    }

    /** حرف التجميع في قايمة جهات الاتصال */
    fun section(name: String): String {
        val c = name.trim().firstOrNull() ?: return "#"
        return when {
            c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ' -> "ا"
            c == 'ة' -> "ه"
            c == 'ى' -> "ي"
            c.isLetter() -> c.uppercaseChar().toString()
            else -> "#"
        }
    }
}
