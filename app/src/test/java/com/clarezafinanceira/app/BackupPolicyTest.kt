package com.clarezafinanceira.app

import android.content.pm.ApplicationInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupPolicyTest {
    private val domains = setOf("root", "file", "database", "sharedpref", "external",
        "device_root", "device_file", "device_database", "device_sharedpref")

    private data class Rule(val section: String, val action: String, val domain: String?,
        val path: String?, val flags: String?)

    private fun rules(resource: Int): List<Rule> {
        val result = mutableListOf<Rule>()
        RuntimeEnvironment.getApplication().resources.getXml(resource).use { parser ->
            var section = ""
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                if (parser.name in setOf("full-backup-content", "cloud-backup", "device-transfer")) {
                    section = parser.name
                } else if (parser.name in setOf("include", "exclude")) {
                    result += Rule(section, parser.name, parser.getAttributeValue(null, "domain"),
                        parser.getAttributeValue(null, "path"), parser.getAttributeValue(null, "requireFlags"))
                }
            }
        }
        return result
    }

    @Test fun manifestConnectsBothPoliciesAndKeepsNativeTransferEnabled() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val app = manifest.getElementsByTagName("application").item(0) as org.w3c.dom.Element
        val android = "http://schemas.android.com/apk/res/android"
        assertEquals("@xml/backup_rules", app.getAttributeNS(android, "fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", app.getAttributeNS(android, "dataExtractionRules"))
        assertEquals("true", app.getAttributeNS(android, "allowBackup"))
        assertTrue(RuntimeEnvironment.getApplication().applicationInfo.flags and
            ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
    }

    @Test
    @Config(sdk = [26, 27])
    fun unsupportedLegacyVersionsExcludeAllDomains() {
        assertEquals(domains.map { Rule("full-backup-content", "exclude", it, ".", null) }.toSet(),
            rules(R.xml.backup_rules).toSet())
    }

    @Test
    @Config(sdk = [28, 30])
    fun legacyTransferRequiresD2dAndDoesNotAdmitFutureFiles() {
        assertEquals(listOf(Rule("full-backup-content", "include", "database",
            "clareza-financeira.db", "deviceToDeviceTransfer")), rules(R.xml.backup_rules))
    }

    @Test
    @Config(sdk = [31, 35])
    fun modernCloudExcludesAllDomainsAndTransferAllowsOnlyTheWholeDatabase() {
        val rules = rules(R.xml.data_extraction_rules)
        assertEquals(domains.map { Rule("cloud-backup", "exclude", it, ".", null) }.toSet(),
            rules.filter { it.section == "cloud-backup" }.toSet())
        assertEquals(listOf(Rule("device-transfer", "include", "database",
            "clareza-financeira.db", null)), rules.filter { it.section == "device-transfer" })
    }
}
