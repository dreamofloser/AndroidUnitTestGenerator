package io.github.dreamofloser.testgen.report

import io.github.dreamofloser.testgen.model.GenerationSummary

internal object QualityScoreCalculator {
    fun calculate(summary: GenerationSummary): Int {
        return with(summary) {
            if (parsedClasses == 0) {
                return@with 0
            }

            val generationRatio =
                generatedClasses.size.toDouble() / parsedClasses.toDouble()
            val assertionRatio =
                generatedAssertions.toDouble() /
                        generatedClasses.size.coerceAtLeast(1).toDouble()
            val baseScore = (generationRatio * 60).toInt()
            val assertionScore = minOf(20, (assertionRatio * 8).toInt())
            val specializationScore =
                if (
                    composeTests +
                    roomDaoTests +
                    retrofitApiTests +
                    liveDataRules +
                    robolectricTests > 0
                ) {
                    15
                } else {
                    5
                }
            val coverageScore = if (coverage != null) 5 else 0
            val fallbackPenalty = minOf(20, fallbackMethods * 3)
            val skippedPenalty = minOf(20, skippedClasses.size * 5)

            (
                    baseScore +
                            assertionScore +
                            specializationScore +
                            coverageScore -
                            fallbackPenalty -
                            skippedPenalty
                    ).coerceIn(0, 100)
        }
    }
}
