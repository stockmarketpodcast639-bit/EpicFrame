package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testStringResource() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Epic Frame", appName)
  }

  @Test
  fun testSplashActivityLaunch() {
    val controller = Robolectric.buildActivity(SplashActivity::class.java).setup()
    assertNotNull(controller.get())
  }

  @Test
  fun testMainActivityLaunch() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    assertNotNull(controller.get())
  }

  @Test
  fun testPlayerActivityLaunch() {
    val controller = Robolectric.buildActivity(PlayerActivity::class.java).setup()
    assertNotNull(controller.get())
  }
}
