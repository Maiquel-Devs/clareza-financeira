package com.clarezafinanceira.app.presentation.about

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProjectPageTest {
    @Test fun externalIntentContainsOnlyTheFixedHttpsUrlAndNoLocalData() {
        var launched: Intent? = null
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun startActivity(intent: Intent) { launched = intent }
        }
        assertTrue(openProjectPage(context))
        val intent = requireNotNull(launched)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://maiquel-devs.github.io/clareza-financeira-landing-page/", intent.dataString)
        assertEquals("https", intent.data?.scheme)
        assertNull(intent.data?.query)
        assertNull(intent.data?.fragment)
        assertNull(intent.extras)
        assertNull(intent.clipData)
        assertNull(intent.component)
        assertNull(intent.`package`)
        assertEquals(setOf(Intent.CATEGORY_BROWSABLE), intent.categories)
    }

    @Test fun absentBrowserReturnsFailureWithoutThrowing() {
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun startActivity(intent: Intent) { throw ActivityNotFoundException("No handler") }
        }
        assertFalse(openProjectPage(context))
    }

    @Test fun blockedExternalLaunchReturnsFailureWithoutThrowing() {
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun startActivity(intent: Intent) { throw SecurityException("Blocked") }
        }
        assertFalse(openProjectPage(context))
    }
}
