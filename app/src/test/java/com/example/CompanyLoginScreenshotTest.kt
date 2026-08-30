package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.KaptaViewModel
import com.example.ui.screens.CompanyLoginScreen
import com.example.ui.theme.MyApplicationTheme
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
class CompanyLoginScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun company_login_screenshot() {
    val viewModel = KaptaViewModel(ApplicationProvider.getApplicationContext())
    composeTestRule.setContent {
      MyApplicationTheme {
        CompanyLoginScreen(
          viewModel = viewModel,
          companyCode = "1001",
          onLoginToPosSuccess = {},
          onBackToRedirection = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/company_login.png")
  }
}
