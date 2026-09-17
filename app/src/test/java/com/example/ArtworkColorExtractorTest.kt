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
    fun `pure white bitmap extracts valid palette`() {
        val pixels = IntArray(100 * 100) { android.graphics.Color.WHITE }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        assertTrue("Theme dominant color should be non-null and have positive alpha", colors.dominant.alpha > 0f)
        assertTrue("Background top should have positive alpha", colors.bgTop.alpha > 0f)
    }

    @Test
    fun `pure black bitmap extracts valid fallback palette`() {
        val pixels = IntArray(100 * 100) { android.graphics.Color.BLACK }
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        assertTrue("Dominant should have positive alpha", colors.dominant.alpha > 0f)
    }

    @Test
    fun `null bitmap extracts default velvet palette`() {
        val colors = ArtworkColorExtractor.extractColorsFromBitmap(null)
        assertTrue("Default palette should have positive alpha", colors.dominant.alpha > 0f)
        assertTrue("Dark background should have positive alpha", colors.darkBackground.alpha > 0f)
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
