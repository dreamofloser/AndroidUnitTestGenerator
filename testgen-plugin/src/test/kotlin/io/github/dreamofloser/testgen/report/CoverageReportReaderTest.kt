package io.github.dreamofloser.testgen.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.SAXParseException
import java.io.File
import java.io.FileNotFoundException

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

    @Test(expected = FileNotFoundException::class)
    fun rejectsMissingReportFile() {
        val file = File.createTempFile("missing-jacoco", ".xml").apply {
            delete()
        }

        CoverageReportReader().read(file)
    }

    @Test(expected = SAXParseException::class)
    fun rejectsEmptyXmlReport() {
        CoverageReportReader().read(reportFile(""))
    }

    @Test(expected = SAXParseException::class)
    fun rejectsMalformedXmlReport() {
        CoverageReportReader().read(
            reportFile("<report><counter></report>"),
        )
    }

    @Test
    fun readsZeroValuedCounter() {
        val file = reportFile(
            """
        <report name="empty">
            <counter type="BRANCH" missed="0" covered="0"/>
        </report>
        """.trimIndent(),
        )

        val summary = CoverageReportReader().read(file)
        val branch = requireNotNull(summary.metric("BRANCH"))

        assertEquals(0, branch.missed)
        assertEquals(0, branch.covered)
        assertEquals(0, branch.total)
        assertEquals(0.0, branch.percentage, 0.01)
    }

    @Test
    fun readsBranchCoverageCounter() {
        val file = reportFile(
            """
        <report name="branch">
            <counter type="BRANCH" missed="2" covered="6"/>
        </report>
        """.trimIndent(),
        )

        val summary = CoverageReportReader().read(file)
        val branch = requireNotNull(summary.metric("BRANCH"))

        assertEquals(2, branch.missed)
        assertEquals(6, branch.covered)
        assertEquals(8, branch.total)
        assertEquals(75.0, branch.percentage, 0.01)
        assertTrue(summary.metrics.isNotEmpty())
    }
    private fun reportFile(content: String): File {
        return File.createTempFile("jacoco-case", ".xml").apply {
            writeText(content)
            deleteOnExit()
        }
    }
}
