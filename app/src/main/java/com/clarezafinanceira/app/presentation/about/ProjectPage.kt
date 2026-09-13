package com.clarezafinanceira.app.presentation.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

private const val PROJECT_PAGE_URL = "https://maiquel-devs.github.io/clareza-financeira-landing-page/"

/** Fixed public URL; no financial state, identifiers, query parameters or extras. */
internal fun openProjectPage(context: Context): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, PROJECT_PAGE_URL.toUri()).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    })
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}
