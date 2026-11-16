package com.project.bcv

import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.core.ui.TabViewModels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabViewModelsTest {
    @Test fun tabSwitchesReuseModelAndSessionClearDisposesIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        var created = 0
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                check(modelClass == ProbeModel::class.java)
                created++
                return ProbeModel() as T
            }
        }

        instrumentation.runOnMainSync {
            TabViewModels.clear()
            try {
                val first = TabViewModels.provider(context, factory)[ProbeModel::class.java]
                val afterSwitch = TabViewModels.provider(context, factory)[ProbeModel::class.java]
                assertSame(first, afterSwitch)
                assertEquals(1, created)
                assertFalse(first.cleared)

                TabViewModels.clear()
                assertTrue(first.cleared)
                assertTrue(first.clearedOnMainThread)
                assertEquals(1, first.clearCount)

                val afterSessionClear = TabViewModels.provider(context, factory)[ProbeModel::class.java]
                assertNotSame(first, afterSessionClear)
                assertEquals(2, created)
                assertFalse(afterSessionClear.cleared)
            } finally {
                TabViewModels.clear()
            }
        }
    }

    private class ProbeModel : ViewModel() {
        var cleared = false
        var clearedOnMainThread = false
        var clearCount = 0
        override fun onCleared() {
            cleared = true
            clearedOnMainThread = Looper.myLooper() == Looper.getMainLooper()
            clearCount++
        }
    }
}
