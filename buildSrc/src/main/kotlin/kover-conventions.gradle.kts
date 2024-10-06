plugins {
    id("org.jetbrains.kotlinx.kover")
}

koverReport {
    defaults {
        html {
            title = "DotEnv HTML Report"
            onCheck = false
            setReportDir(layout.buildDirectory.dir("kover-reports/html-result"))
        }

        verify {
            onCheck = true

            rule("Minimal line coverage rate in percents") {
                isEnabled = true

                entity = kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.APPLICATION

                bound {
                    minValue = 70

                    metric = kotlinx.kover.gradle.plugin.dsl.MetricType.LINE
                    aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                }
            }
        }
    }
}
