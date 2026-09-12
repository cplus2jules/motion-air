package com.motionair.controller

import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Supply a fresh invitation for a disposable bridge with emulator-reachable hosts. */
@RunWith(AndroidJUnit4::class)
class BridgeIntegrationTest {
    @Test fun pairEnableMotionLockAndStopOnBackground() {
        val invitation = InstrumentationRegistry.getArguments().getString("pairingInvitation")
        assumeNotNull(invitation)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            device.wait(Until.findObject(By.text("Paste pairing code")), 5000)!!.click()
            val input = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 5000)!!
            input.text = invitation
            assertEquals(invitation, input.text)
            device.findObject(By.text("CONTINUE")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Pair with this computer?")), 5000))
            device.findObject(By.text("PAIR")).click()
            assertTrue("Controller acknowledged by bridge", device.wait(Until.hasObject(By.text("Enable Motion").enabled(true)), 15000))
            device.takeScreenshot(File(instrumentation.targetContext.filesDir, "qa-controller.png"))
            device.findObject(By.text("Enable Motion")).click()
            assertTrue("Motion acknowledged", device.wait(Until.hasObject(By.text("Disable Motion")), 5000))
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            assertTrue("A visible pause keeps the motion session", device.wait(Until.hasObject(By.text("Disable Motion")), 5000))
            device.findObject(By.text("Dance Lock")).click()
            assertTrue(device.wait(Until.hasObject(By.text("All moves.\nNo mis-taps.")), 5000))
            assertTrue("Emulator sensors reach the DSU receiver", device.wait(Until.hasObject(By.text("Sending motion")), 10000))
            device.takeScreenshot(File(instrumentation.targetContext.filesDir, "qa-dance-lock.png"))
            val unlock = device.findObject(By.desc("Unlock controls")).visibleCenter
            device.swipe(unlock.x, unlock.y, unlock.x, unlock.y, 400)
            assertTrue(device.wait(Until.hasObject(By.text("Disable Motion")), 5000))
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            assertTrue("Background stops the connection", device.wait(Until.hasObject(By.text("Disconnected")), 5000))
            assertTrue("Motion stays off after returning", device.wait(Until.hasObject(By.text("Enable Motion").enabled(false)), 5000))
            device.findObject(By.text("Reconnect")).click()
            assertTrue("Reconnect starts with motion off", device.wait(Until.hasObject(By.text("Enable Motion").enabled(true)), 15000))
            device.findObject(By.text("Enable Motion")).click()
            assertTrue("Motion can restart in a new session", device.wait(Until.hasObject(By.text("Disable Motion")), 5000))
            scenario.moveToState(Lifecycle.State.CREATED)
        }
    }
}
