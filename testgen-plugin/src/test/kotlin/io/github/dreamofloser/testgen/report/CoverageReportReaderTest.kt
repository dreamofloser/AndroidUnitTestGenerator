package io.github.dreamofloser.testgen.report

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class CoverageReportReaderTest {
    @Test
    fun readsJacocoXmlCounters() {
        val file = File.createTempFile("jacoco", ".xml").apply {
            writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <report name="sample">
                    <counter type="INSTRUCTION" missed="2" covered="8"/>
                    <counter type="LINE" missed="1" covered="3"/>
                    <package name="sample">
                        <counter type="LINE" missed="1" covered="1"/>
                    </package>
                </report>
                """.trimIndent(),
            )
            deleteOnExit()
        }

        val summary = CoverageReportReader().read(file)
        val line = summary.metric("LINE")

        assertEquals(1, line?.missed)
        assertEquals(3, line?.covered)
        assertEquals(4, line?.total)
        assertEquals(75.0, line?.percentage ?: 0.0, 0.01)
    }
}
