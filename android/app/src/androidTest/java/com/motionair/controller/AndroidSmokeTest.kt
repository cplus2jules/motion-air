package com.motionair.controller

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AndroidSmokeTest {
    @Test fun homeProvidesScanningAndManualPairing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val text = mutableListOf<String>()
                fun visit(view: View) {
                    if (view is TextView) text += view.text.toString()
                    if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
                }
                visit(activity.window.decorView)
                assertTrue(text.any { it == "Scan computer QR" || it == "Escanear QR del equipo" })
                assertTrue(text.any { it == "Paste pairing code" || it == "Pegar código de conexión" })
                assertTrue(text.any { it.contains("Windows") })
            }
        }
    }
    @Test fun savedConnectionsAreEncryptedAndRemainReadableAfterRestart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = PairingStore(context)
        val id = UUID.randomUUID().toString()
        val token = "fixture_" + UUID.randomUUID().toString().replace("-", "")
        val computer = SavedComputer(id, "Test PC", listOf("192.168.1.2"), 3443, "ab".repeat(32), token, UUID.randomUUID().toString())
        try {
            store.save(computer)
            assertEquals(token, PairingStore(context).all().first { it.id == id }.token)
            val stored = context.getSharedPreferences("paired-computers", Activity.MODE_PRIVATE).all.toString()
            assertFalse(stored.contains(token)); assertFalse(stored.contains("192.168.1.2"))
        } finally { store.remove(id) }
    }
}
