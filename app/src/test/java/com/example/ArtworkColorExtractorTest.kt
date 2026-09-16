package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.example.media.ArtworkColorExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArtworkColorExtractorTest {

    @Test
    fun `pure white bitmap generates ash palette not red`() {
        val pixels = IntArray(100 * 100) { android.graphics.Color.WHITE }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colors.dominant.toArgb(), hsv)
        assertTrue("Ash dominant saturation should be low, got ${hsv[1]}", hsv[1] <= 0.20f)
        val bgHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colors.bgTop.toArgb(), bgHsv)
        assertTrue("Ash bg saturation should be low, got ${bgHsv[1]}", bgHsv[1] <= 0.20f)
    }

    @Test
    fun `pure black bitmap generates ash palette not red`() {
        val pixels = IntArray(100 * 100) { android.graphics.Color.BLACK }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colors.dominant.toArgb(), hsv)
        assertTrue("Ash dominant saturation should be low, got ${hsv[1]}", hsv[1] <= 0.20f)
    }

    @Test
    fun `black and white bitmap generates ash palette`() {
        val pixels = IntArray(100 * 100) { index ->
            val x = index % 100
            val y = index / 100
            if (x in 20..80 && y in 20..80) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colors.dominant.toArgb(), hsv)
        assertTrue("Ash dominant saturation should be low, got ${hsv[1]}", hsv[1] <= 0.20f)
        assertEquals(Color(0xFF0E0F12), colors.darkBackground)
    }

    @Test
    fun `monochrome with slight jpeg noise generates ash palette`() {
        val pixels = IntArray(100 * 100) { index ->
            val x = index % 100
            val y = index / 100
            val isWhite = (x + y) % 2 == 0
            // Add slight noise (channel differences under 20)
            val base = if (isWhite) 240 else 20
            val r = (base + (x % 5)).coerceIn(0, 255)
            val g = (base + (y % 5)).coerceIn(0, 255)
            val b = (base).coerceIn(0, 255)
            android.graphics.Color.rgb(r, g, b)
        }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colors.dominant.toArgb(), hsv)
        assertTrue("Ash dominant saturation should be low, got ${hsv[1]}", hsv[1] <= 0.20f)
    }

    @Test
    fun `vibrant blue bitmap preserves blue palette`() {
        val blue = android.graphics.Color.rgb(20, 120, 240)
        val pixels = IntArray(100 * 100) { blue }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colors.dominant.toArgb(), hsv)
        assertTrue("Blue hue expected, got ${hsv[0]}", hsv[0] in 190f..230f)
        assertTrue("Saturation should be high for blue, got ${hsv[1]}", hsv[1] > 0.40f)
    }

    @Test
    fun `vibrant crimson bitmap preserves crimson palette`() {
        val crimson = android.graphics.Color.rgb(220, 20, 60)
        val pixels = IntArray(100 * 100) { crimson }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colors.dominant.toArgb(), hsv)
        assertTrue("Crimson hue expected (around 340-360 or 0-15), got ${hsv[0]}", hsv[0] > 330f || hsv[0] < 20f)
        assertTrue("Saturation should be high for crimson, got ${hsv[1]}", hsv[1] > 0.40f)
    }

    @Test
    fun `drawable resources in app extract colors successfully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val colors = ArtworkColorExtractor.getColorsForDrawable(context, R.drawable.art_after_hours)
        assertTrue("Theme colors should have positive alpha", colors.dominant.alpha > 0f)
        assertTrue("Dark background should be valid", colors.darkBackground.alpha > 0f)
    }
}
