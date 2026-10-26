package com.mohamed.phone.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private class Part(val fill: Boolean, val w: Float, val b: PathBuilder.() -> Unit)

private fun outl(w: Float = 2f, b: PathBuilder.() -> Unit) = Part(false, w, b)
private fun solid(w: Float = 1.6f, b: PathBuilder.() -> Unit) = Part(true, w, b)

private fun ImageVector.Builder.addPart(p: Part) {
    path(
        fill = if (p.fill) SolidColor(Color.Black) else null,
        stroke = SolidColor(Color.Black), strokeLineWidth = p.w,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = p.b
    )
}

private fun vecM(name: String, mirror: Boolean, rot: Float, vararg parts: Part): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirror)
    if (rot != 0f) {
        b.group(rotate = rot, pivotX = 12f, pivotY = 12f) { for (p in parts) b.addPart(p) }
    } else {
        for (p in parts) b.addPart(p)
    }
    return b.build()
}

private fun vec(name: String, fill: Boolean = false, w: Float = 2f, mirror: Boolean = false, rot: Float = 0f, b: PathBuilder.() -> Unit): ImageVector =
    vecM(name, mirror, rot, Part(fill, w, b))

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
}

private fun PathBuilder.rr(x: Float, y: Float, w: Float, h: Float, r: Float) {
    moveTo(x + r, y); horizontalLineTo(x + w - r)
    arcTo(r, r, 0f, false, true, x + w, y + r); verticalLineTo(y + h - r)
    arcTo(r, r, 0f, false, true, x + w - r, y + h); horizontalLineTo(x + r)
    arcTo(r, r, 0f, false, true, x, y + h - r); verticalLineTo(y + r)
    arcTo(r, r, 0f, false, true, x + r, y); close()
}

private fun PathBuilder.handset() {
    moveTo(6.62f, 10.79f)
    curveTo(8.06f, 13.62f, 10.38f, 15.93f, 13.21f, 17.38f)
    lineTo(15.41f, 15.18f)
    curveTo(15.68f, 14.91f, 16.08f, 14.82f, 16.43f, 14.94f)
    curveTo(17.55f, 15.31f, 18.76f, 15.51f, 20f, 15.51f)
    curveTo(20.55f, 15.51f, 21f, 15.96f, 21f, 16.51f)
    verticalLineTo(20f)
    curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
    curveTo(10.61f, 21f, 3f, 13.39f, 3f, 4f)
    curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
    horizontalLineTo(7.5f)
    curveTo(8.05f, 3f, 8.5f, 3.45f, 8.5f, 4f)
    curveTo(8.5f, 5.25f, 8.7f, 6.45f, 9.07f, 7.57f)
    curveTo(9.18f, 7.92f, 9.1f, 8.31f, 8.82f, 8.59f)
    lineTo(6.62f, 10.79f)
    close()
}

object Ic {
    val Phone by lazy { vec("phone", fill = true, w = 1.4f) { handset() } }
    val PhoneEnd by lazy { vec("phone_end", fill = true, w = 1.4f, rot = 135f) { handset() } }
    val Keypad by lazy { vec("keypad", w = 3.6f) {
        for (y in listOf(5.5f, 12f, 18.5f)) for (x in listOf(5.5f, 12f, 18.5f)) { moveTo(x, y); lineTo(x + 0.01f, y) }
    } }
    val Backspace by lazy { vecM("backspace", false, 0f,
        outl { moveTo(9.2f, 5.5f); horizontalLineTo(19f); arcTo(2f, 2f, 0f, false, true, 21f, 7.5f); verticalLineTo(16.5f)
            arcTo(2f, 2f, 0f, false, true, 19f, 18.5f); horizontalLineTo(9.2f); lineTo(3.3f, 12f); close() },
        outl(2.2f) { moveTo(12.6f, 9.6f); lineTo(16.6f, 14.4f); moveTo(16.6f, 9.6f); lineTo(12.6f, 14.4f) }) }
    val Message by lazy { vec("message") {
        moveTo(7.5f, 4.5f); horizontalLineTo(16.5f); arcTo(4f, 4f, 0f, false, true, 20.5f, 8.5f); verticalLineTo(9f)
        arcTo(4f, 4f, 0f, false, true, 16.5f, 13f); horizontalLineTo(11.5f); lineTo(7.5f, 16.8f); verticalLineTo(13f)
        arcTo(4f, 4f, 0f, false, true, 3.5f, 9f); verticalLineTo(8.5f); arcTo(4f, 4f, 0f, false, true, 7.5f, 4.5f); close() } }
    val Send by lazy { vec("send", fill = true, w = 1.8f, mirror = true) {
        moveTo(20.3f, 4f); lineTo(14f, 20f); lineTo(10.8f, 13.2f); lineTo(4f, 10f); close() } }
    val Person by lazy { vecM("person", false, 0f,
        outl { circle(12f, 8.2f, 3.6f) },
        outl { moveTo(5f, 19.5f); curveTo(5f, 16.5f, 8f, 14.8f, 12f, 14.8f); curveTo(16f, 14.8f, 19f, 16.5f, 19f, 19.5f) }) }
    val Clock by lazy { vec("clock") { circle(12f, 12f, 8.5f); moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f) } }
    val Star by lazy { vec("star") {
        moveTo(12f, 3.8f); lineTo(14.4f, 8.9f); lineTo(20f, 9.6f); lineTo(15.9f, 13.5f); lineTo(17f, 19f)
        lineTo(12f, 16.3f); lineTo(7f, 19f); lineTo(8.1f, 13.5f); lineTo(4f, 9.6f); lineTo(9.6f, 8.9f); close() } }
    val StarFill by lazy { vec("star_fill", fill = true) {
        moveTo(12f, 3.8f); lineTo(14.4f, 8.9f); lineTo(20f, 9.6f); lineTo(15.9f, 13.5f); lineTo(17f, 19f)
        lineTo(12f, 16.3f); lineTo(7f, 19f); lineTo(8.1f, 13.5f); lineTo(4f, 9.6f); lineTo(9.6f, 8.9f); close() } }
    val Add by lazy { vec("add", w = 2.2f) { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Mic by lazy { vecM("mic", false, 0f,
        outl { rr(9f, 3.5f, 6f, 11f, 3f) },
        outl { moveTo(5.5f, 11.5f); arcTo(6.5f, 6.5f, 0f, false, false, 18.5f, 11.5f); moveTo(12f, 18f); lineTo(12f, 20.5f) }) }
    val MicOff by lazy { vecM("mic_off", false, 0f,
        outl { rr(9f, 3.5f, 6f, 11f, 3f) },
        outl { moveTo(5.5f, 11.5f); arcTo(6.5f, 6.5f, 0f, false, false, 18.5f, 11.5f); moveTo(12f, 18f); lineTo(12f, 20.5f) },
        outl(2.4f) { moveTo(4.5f, 4.5f); lineTo(19.5f, 19.5f) }) }
    val Speaker by lazy { vecM("speaker", false, 0f,
        outl { moveTo(4f, 9.5f); horizontalLineTo(7.2f); lineTo(11.5f, 6f); verticalLineTo(18f); lineTo(7.2f, 14.5f); horizontalLineTo(4f); close() },
        outl { moveTo(15.5f, 9.2f); arcTo(3.6f, 3.6f, 0f, false, true, 15.5f, 14.8f); moveTo(18f, 6.6f); arcTo(7.4f, 7.4f, 0f, false, true, 18f, 17.4f) }) }
    val Pause by lazy { vecM("pause", false, 0f, solid(2.4f) { rr(7.4f, 6.2f, 2.2f, 11.6f, 1.1f); rr(14.4f, 6.2f, 2.2f, 11.6f, 1.1f) }) }
    val Block by lazy { vec("block") { circle(12f, 12f, 8.5f); moveTo(6.2f, 6.2f); lineTo(17.8f, 17.8f) } }
    val Trash by lazy { vec("trash") {
        moveTo(5f, 7f); horizontalLineTo(19f); moveTo(9.5f, 7f); verticalLineTo(5f); horizontalLineTo(14.5f); verticalLineTo(7f)
        moveTo(6.8f, 7f); lineTo(7.6f, 17.8f); arcTo(2.2f, 2.2f, 0f, false, false, 9.8f, 19.8f); horizontalLineTo(14.2f)
        arcTo(2.2f, 2.2f, 0f, false, false, 16.4f, 17.8f); lineTo(17.2f, 7f) } }
    val Edit by lazy { vec("edit") {
        moveTo(4.5f, 19.5f); lineTo(5.3f, 15.6f); lineTo(15.4f, 5.5f); lineTo(18.5f, 8.6f); lineTo(8.4f, 18.7f); close()
        moveTo(13.4f, 7.5f); lineTo(16.5f, 10.6f) } }
    val Settings by lazy { vec("settings") {
        moveTo(4f, 8f); horizontalLineTo(11.5f); circle(14f, 8f, 2.5f); moveTo(16.5f, 8f); horizontalLineTo(20f)
        moveTo(4f, 16f); horizontalLineTo(7f); circle(9.5f, 16f, 2.5f); moveTo(12f, 16f); horizontalLineTo(20f) } }
    val Search by lazy { vec("search", w = 2.2f) { circle(10.5f, 10.5f, 6.3f); moveTo(15.2f, 15.2f); lineTo(20f, 20f) } }
    val Close by lazy { vec("close", w = 2.2f) { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) } }
    val Check by lazy { vec("check", w = 2.4f) { moveTo(5f, 12.5f); lineTo(9.8f, 17.2f); lineTo(19f, 7.2f) } }
    val Menu by lazy { vec("menu", w = 2.2f) { moveTo(4f, 7f); horizontalLineTo(20f); moveTo(4f, 12f); horizontalLineTo(20f); moveTo(4f, 17f); horizontalLineTo(20f) } }
    val More by lazy { vec("more", w = 3.4f) {
        moveTo(12f, 5f); lineTo(12.01f, 5f); moveTo(12f, 12f); lineTo(12.01f, 12f); moveTo(12f, 19f); lineTo(12.01f, 19f) } }
    val Back by lazy { vec("back", w = 2.2f, mirror = true) { moveTo(14.5f, 5.5f); lineTo(8f, 12f); lineTo(14.5f, 18.5f) } }
    val ArrowIn by lazy { vec("arrow_in", w = 2.2f) { moveTo(17f, 7f); lineTo(7f, 17f); moveTo(7f, 9.5f); lineTo(7f, 17f); lineTo(14.5f, 17f) } }
    val ArrowOut by lazy { vec("arrow_out", w = 2.2f) { moveTo(7f, 17f); lineTo(17f, 7f); moveTo(9.5f, 7f); lineTo(17f, 7f); lineTo(17f, 14.5f) } }
    val Moon by lazy { vec("moon") {
        moveTo(20f, 14.2f); arcTo(8f, 8f, 0f, true, true, 9.8f, 4f); arcTo(6.3f, 6.3f, 0f, false, false, 20f, 14.2f); close() } }
    val Copy by lazy { vecM("copy", false, 0f,
        outl { rr(9f, 9f, 10.5f, 10.5f, 3f) },
        outl { moveTo(14.5f, 6f); horizontalLineTo(8f); arcTo(3f, 3f, 0f, false, false, 5f, 9f); verticalLineTo(14.5f) }) }
    val Swap by lazy { vec("swap") {
        moveTo(8f, 5f); lineTo(8f, 19f); moveTo(4.5f, 8.5f); lineTo(8f, 5f); lineTo(11.5f, 8.5f)
        moveTo(16f, 19f); lineTo(16f, 5f); moveTo(12.5f, 15.5f); lineTo(16f, 19f); lineTo(19.5f, 15.5f) } }
    val Merge by lazy { vec("merge") { moveTo(6f, 4.5f); lineTo(12f, 12f); lineTo(18f, 4.5f); moveTo(12f, 12f); lineTo(12f, 19.5f) } }
    val Info by lazy { vec("info") { circle(12f, 12f, 8.5f); moveTo(12f, 11f); lineTo(12f, 16.2f); moveTo(12f, 7.8f); lineTo(12.01f, 7.8f) } }
    val Voicemail by lazy { vec("voicemail") { circle(7.5f, 12f, 3.2f); circle(16.5f, 12f, 3.2f); moveTo(7.5f, 15.2f); horizontalLineTo(16.5f) } }
}
