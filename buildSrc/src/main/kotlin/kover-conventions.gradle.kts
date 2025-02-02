import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    id("org.jetbrains.kotlinx.kover")
}


kover {
    reports.total.html {
        title = "DotEnv HTML Report"
        onCheck = false
        htmlDir = layout.buildDirectory.dir("kover-reports/html-result")
    }

    reports.verify.rule("Minimal line coverage rate in percents") {
        bound {
            minValue = 70

            coverageUnits = CoverageUnit.LINE
            aggregationForGroup = AggregationType.COVERED_PERCENTAGE
        }
    }
}
