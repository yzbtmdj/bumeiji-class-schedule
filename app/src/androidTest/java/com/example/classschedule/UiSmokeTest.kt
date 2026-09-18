package com.example.classschedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class UiSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstLaunchShowsNativeChineseSetup() {
        rule.onNodeWithText("不美鸡课表").assertIsDisplayed()
        rule.onNodeWithText("学期名称").assertIsDisplayed()
        rule.onNodeWithText("导入课表文件").assertIsDisplayed()
    }
}
