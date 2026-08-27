package com.slashboard.keyboard

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import com.slashboard.keyboard.service.SinhalaIME

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SinhalaIMETest {
    @Test
    fun testOnCreateInputView() {
        val service = Robolectric.buildService(SinhalaIME::class.java).create().get()
        val view = service.onCreateInputView()
        println("View created successfully: $view")
    }
}
