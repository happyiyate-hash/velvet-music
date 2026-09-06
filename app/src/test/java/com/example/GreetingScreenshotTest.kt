package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.SampleData
import com.example.ui.HomeFeedScreen
import com.example.ui.theme.VelvetTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      VelvetTheme {
        HomeFeedScreen(
          currentTrack = SampleData.trackAfterHours,
          isPlaying = false,
          allTracks = SampleData.recentlyPlayedTracks,
          mostPlayedTracks = SampleData.recentlyPlayedTracks,
          recentlyAddedTracks = SampleData.newReleases,
          onSelectTrack = {},
          onOpenSearch = {},
          onOpenSettings = {},
          onOpenNotifications = {},
          onTrackMenuClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
