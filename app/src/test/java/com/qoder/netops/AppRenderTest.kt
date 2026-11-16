package com.qoder.netops

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.qoder.netops.ui.NetOpsApp
import com.qoder.netops.ui.theme.NetOpsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class AppRenderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renderLightTabs() {
        composeRule.setContent {
            NetOpsTheme(darkTheme = false) {
                NetOpsApp()
            }
        }
        composeRule.waitForIdle()
        capture("01-ping-light", 0xFFF8F8F6.toInt())

        composeRule.onNodeWithTag("tab_ports").performClick()
        composeRule.waitForIdle()
        capture("02-ports-light", 0xFFF8F8F6.toInt())

        composeRule.onNodeWithTag("tab_lan").performClick()
        composeRule.waitForIdle()
        capture("03-lan-light", 0xFFF8F8F6.toInt())
    }

    @Test
    fun renderDarkTabs() {
        composeRule.setContent {
            NetOpsTheme(darkTheme = true) {
                NetOpsApp()
            }
        }
        composeRule.waitForIdle()
        capture("04-ping-dark", 0xFF0B0B0D.toInt())

        composeRule.onNodeWithTag("tab_ports").performClick()
        composeRule.waitForIdle()
        capture("05-ports-dark", 0xFF0B0B0D.toInt())

        composeRule.onNodeWithTag("tab_lan").performClick()
        composeRule.waitForIdle()
        capture("06-lan-dark", 0xFF0B0B0D.toInt())
    }

    private fun capture(name: String, backgroundColor: Int) {
        composeRule.waitForIdle()
        val activity = composeRule.activity
        val content = activity.findViewById<FrameLayout>(android.R.id.content)
        val view: View = if (content.childCount > 0) content.getChildAt(0) else content

        val metrics = activity.resources.displayMetrics
        val width = if (view.width > 0) view.width else metrics.widthPixels
        val height = if (view.height > 0) view.height else metrics.heightPixels
        if (view.width == 0 || view.height == 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, width, height)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(backgroundColor)
        view.draw(Canvas(bitmap))

        val directory = File("build/ui-screenshots").apply { mkdirs() }
        FileOutputStream(File(directory, "$name.png")).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}