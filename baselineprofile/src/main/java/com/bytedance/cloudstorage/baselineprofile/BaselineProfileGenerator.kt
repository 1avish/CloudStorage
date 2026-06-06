package com.bytedance.cloudstorage.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startupOpenFileTabAndScroll() {
        baselineProfileRule.collect(
            packageName = "com.bytedance.cloudstorage",
            profileBlock = {
                pressHome()
                startActivityAndWait()

                val fileTab = device.wait(
                    Until.findObject(By.res("com.bytedance.cloudstorage:id/main_tab_files")),
                    5_000
                ) ?: return@collect
                fileTab.click()

                val fileList = device.wait(
                    Until.findObject(By.res("com.bytedance.cloudstorage:id/file_list")),
                    5_000
                ) ?: return@collect
                repeat(6) {
                    fileList.fling(Direction.DOWN)
                    device.waitForIdle()
                    fileList.fling(Direction.UP)
                    device.waitForIdle()
                }
            }
        )
    }
}
