package com.motionair.controller

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WelcomeFlowTest {
    @Test fun practiceSurvivesRecreationAndFinishesAtPairing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences(MainActivity::class.java.name, 0)
        preferences.edit().putString("language", "en").putBoolean("welcomeComplete", false).commit()
        val device = UiDevice.getInstance(instrumentation)
        fun click(text: String) {
            if (!device.wait(Until.hasObject(By.text(text)), 1000) && device.hasObject(By.scrollable(true))) UiScrollable(UiSelector().scrollable(true)).scrollTextIntoView(text)
            device.wait(Until.findObject(By.text(text)), 5000)!!.click()
        }
        fun practice(key: String) {
            if (!device.hasObject(By.desc("Practice $key"))) UiScrollable(UiSelector().scrollable(true)).scrollIntoView(UiSelector().description("Practice $key"))
            device.wait(Until.findObject(By.desc("Practice $key")), 5000)!!.click()
        }
        fun screenshot(name: String) { device.takeScreenshot(File(context.filesDir, name)) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTrue(device.wait(Until.hasObject(By.text("Small screen.\nBig game.")), 5000))
            screenshot("qa-welcome.png")
            click("Let's try it")
            practice("A")
            assertTrue(device.wait(Until.hasObject(By.text("Selected. Nice one.")), 5000))
            practice("B")
            assertTrue(device.wait(Until.hasObject(By.text("And you're back.")), 5000))
            screenshot("qa-practice.png")
            scenario.recreate()
            assertTrue("Returning to the app keeps the current lesson", device.wait(Until.hasObject(By.text("Feels familiar.\nPlays your way.")), 5000))
            click("One more thing")
            click("Try Dance Lock")
            assertTrue(device.wait(Until.hasObject(By.text("Taps locked.\nMotion keeps going.")), 5000))
            screenshot("qa-motion-preview.png")
            click("Unlock preview")
            click("Pair my computer")
            assertTrue(device.wait(Until.hasObject(By.text("Scan computer QR")), 5000))
            assertTrue(preferences.getBoolean("welcomeComplete", false))
            click("Done")
            assertTrue(device.wait(Until.hasObject(By.text("Ready, player one?")), 5000))
            screenshot("qa-controller-offline.png")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            assertTrue("The welcome is shown only once", device.wait(Until.hasObject(By.text("Pair a computer")), 5000))
            device.findObject(By.desc("Settings")).click()
            click("Show welcome guide")
            assertTrue(device.wait(Until.hasObject(By.text("Small screen.\nBig game.")), 5000))
            click("Close")
            assertTrue(device.wait(Until.hasObject(By.text("Pair a computer")), 5000))
        }
    }

    @Test fun spanishWelcomeCanBeSkippedAndReplayed() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val preferences = instrumentation.targetContext.getSharedPreferences(MainActivity::class.java.name, 0)
        preferences.edit().putString("language", "es").putBoolean("welcomeComplete", false).commit()
        val device = UiDevice.getInstance(instrumentation)
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                assertTrue(device.wait(Until.hasObject(By.text("Pantalla pequeña.\nGran partida.")), 5000))
                device.takeScreenshot(File(instrumentation.targetContext.filesDir, "qa-welcome-es.png"))
                device.findObject(By.text("Omitir")).click()
                assertTrue(device.wait(Until.hasObject(By.text("Conectar un equipo")), 5000))
                device.findObject(By.desc("Ajustes")).click()
                device.wait(Until.findObject(By.text("Ver guía de bienvenida")), 5000)!!.click()
                assertTrue(device.wait(Until.hasObject(By.text("Vamos a probar")), 5000))
                device.findObject(By.text("Cerrar")).click()
            }
        } finally { preferences.edit().putString("language", "en").commit() }
    }
}
