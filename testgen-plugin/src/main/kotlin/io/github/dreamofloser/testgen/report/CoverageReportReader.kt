package io.github.dreamofloser.testgen.report

import io.github.dreamofloser.testgen.model.CoverageMetric
import io.github.dreamofloser.testgen.model.CoverageSummary
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CoverageReportReader {
    fun read(reportFile: File): CoverageSummary {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val document = factory
            .newDocumentBuilder()
            .parse(reportFile)
        val counters = document.getElementsByTagName("counter")
        val metrics = buildList {
            for (index in 0 until counters.length) {
                val counter = counters.item(index)
                if (counter.parentNode != document.documentElement) {
                    continue
                }
                val attributes = counter.attributes
                val type = attributes.getNamedItem("type")?.nodeValue ?: continue
                val missed = attributes.getNamedItem("missed")?.nodeValue?.toIntOrNull() ?: 0
                val covered = attributes.getNamedItem("covered")?.nodeValue?.toIntOrNull() ?: 0

                add(CoverageMetric(type = type, missed = missed, covered = covered))
            }
        }

        return CoverageSummary(
            reportFile = reportFile,
            metrics = metrics
                .groupBy { it.type }
                .map { (type, values) ->
                    CoverageMetric(
                        type = type,
                        missed = values.sumOf { it.missed },
                        covered = values.sumOf { it.covered },
                    )
                }
                .sortedBy { it.type },
        )
    }
}
