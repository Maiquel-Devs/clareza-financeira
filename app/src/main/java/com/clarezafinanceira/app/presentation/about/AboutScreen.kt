package com.clarezafinanceira.app.presentation.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.clarezafinanceira.app.R

@Composable
fun AboutRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    AboutScreen(onBack = onBack, onProjectPage = { openProjectPage(context) })
}

@Composable
fun AboutScreen(onBack: () -> Unit, onProjectPage: () -> Boolean) {
    var linkFailed by remember { mutableStateOf(false) }
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    // Render the approved adaptive launcher icon, preserving its layers and mask.
    val icon = remember(resources, configuration) {
        requireNotNull(resources.getDrawable(R.mipmap.ic_launcher, null)).toBitmap(288, 288).asImageBitmap()
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 600.dp).fillMaxSize()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.about_back)) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(icon, contentDescription = null, modifier = Modifier.size(96.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.about_tagline), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.about_project_heading), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.about_project_description), style = MaterialTheme.typography.bodyLarge)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.about_developer_heading), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.about_developer_name), style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = { linkFailed = !onProjectPage() }) {
                        Text(stringResource(R.string.about_project_page))
                    }
                    if (linkFailed) Text(stringResource(R.string.about_link_failed),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.about_open_source), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
