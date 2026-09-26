package com.example

import android.graphics.Bitmap
import com.example.media.ArtworkColorExtractor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VelvetPaletteDebugTest {

    @Test
    fun `debug report exposes cluster coverage and selected roles`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(10_000) { index ->
            if (index < 8_000) {
                android.graphics.Color.rgb(42, 44, 48)
            } else {
                android.graphics.Color.rgb(55, 95, 180)
            }
        }
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        val debug = colors.debug

        assertNotNull("Palette debug report should be available", debug)
        assertTrue("At least two clusters should be reported", debug!!.clusters.size >= 2)
        assertTrue("Cluster coverage should be positive", debug.clusters.any { it.coverage > 0.0 })
        assertTrue("Exactly one dominant cluster should be selected", debug.clusters.count { it.selectedAsDominant } == 1)
        assertFalse("This artwork should not be classified as neutral", debug.artworkIsNeutral)
        assertTrue("Chromatic coverage should be measurable", debug.chromaticCoverage > 0.0)
        assertTrue("A meaningful accent should be selected", debug.clusters.any { it.selectedAsAccent })
    }

    @Test
    fun `mostly neutral artwork protects against small chromatic detail`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(10_000) { index ->
            if (index < 9_500) {
                android.graphics.Color.rgb(55, 55, 57)
            } else {
                android.graphics.Color.rgb(50, 100, 190)
            }
        }
        bitmap.setPixels(pixels, 0, 100, 0, 0, 100, 100)

        val colors = ArtworkColorExtractor.extractColorsFromBitmap(bitmap)
        val debug = colors.debug

        assertNotNull(debug)
        assertTrue("Artwork should be protected as neutral", debug!!.artworkIsNeutral)
        assertTrue("Neutral artwork should not select an accent", debug.accentClusterIndex == null)
        assertTrue("Dominant cluster should have the largest meaningful coverage", debug.clusters.first { it.selectedAsDominant }.coverage > 0.50)
    }
}
