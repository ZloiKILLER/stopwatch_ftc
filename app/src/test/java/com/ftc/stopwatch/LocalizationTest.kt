package com.ftc.stopwatch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizationTest {

    @Test
    fun `every shipped locale translates every string`() {
        val expectedKeys = stringsFor(DEFAULT_LOCALE).keys
        assertTrue("the default locale should declare some strings", expectedKeys.isNotEmpty())

        TRANSLATED_LOCALES.forEach { locale ->
            val translated = stringsFor(locale)
            assertEquals(
                "values-$locale is missing translations",
                emptySet<String>(),
                expectedKeys - translated.keys,
            )
            assertEquals(
                "values-$locale declares strings the default locale does not",
                emptySet<String>(),
                translated.keys - expectedKeys,
            )
            translated.forEach { (key, value) ->
                assertFalse("values-$locale/$key is blank", value.isBlank())
            }
        }
    }

    @Test
    fun `the resource qualifier actually resolves each locale at runtime`() {
        TRANSLATED_LOCALES.forEach { locale ->
            val declared = stringsFor(locale)
            RuntimeEnvironment.setQualifiers("+$locale")
            val context = ApplicationProvider.getApplicationContext<Context>()

            KEYS_ON_SCREEN.forEach { key ->
                val resourceId =
                    context.resources.getIdentifier(key, "string", context.packageName)
                assertEquals(
                    "$key should resolve to the values-$locale translation",
                    declared.getValue(key),
                    context.getString(resourceId),
                )
            }
        }
    }

    @Test
    fun `locales_config lists exactly the locales that ship`() {
        val declared =
            resourceDirectory()
                .resolve("xml/locales_config.xml")
                .let(::parse)
                .getElementsByTagName("locale")
                .let { nodes ->
                    (0 until nodes.length).map { index ->
                        nodes
                            .item(index)
                            .attributes
                            .getNamedItem("android:name")
                            .nodeValue
                    }
                }
                .toSet()

        assertEquals(TRANSLATED_LOCALES + DEFAULT_LOCALE_NAME, declared)
    }

    private fun stringsFor(locale: String): Map<String, String> {
        val directory = if (locale == DEFAULT_LOCALE) "values" else "values-$locale"
        val file = resourceDirectory().resolve("$directory/strings.xml")
        assertTrue("missing $directory/strings.xml", file.isFile)

        val entries = parse(file).getElementsByTagName("string")
        return (0 until entries.length).associate { index ->
            val node = entries.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    private fun parse(file: File) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement

    /** Unit tests run with the module directory as the working directory. */
    private fun resourceDirectory(): File =
        sequenceOf("src/main/res", "app/src/main/res")
            .map(::File)
            .firstOrNull(File::isDirectory)
            ?: error("could not locate the resource directory from ${File("").absolutePath}")

    private companion object {
        const val DEFAULT_LOCALE = "default"
        const val DEFAULT_LOCALE_NAME = "en"

        val TRANSLATED_LOCALES =
            setOf("de", "es", "fr", "hi", "it", "ja", "kk", "ru", "uk", "zh")

        /** The strings a user actually reads on the one screen this app has. */
        val KEYS_ON_SCREEN =
            listOf(
                "lap",
                "reset",
                "start",
                "pause",
                "header_lap",
                "header_lap_time",
                "header_total_time",
                "empty_laps_title",
                "empty_laps_desc",
                "tag_fastest",
                "tag_slowest",
                "elapsed_time",
            )
    }
}
