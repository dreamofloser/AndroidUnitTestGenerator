package io.github.dreamofloser.testgen

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class TestGenExtension @Inject constructor(
    objects: ObjectFactory,
    layout: ProjectLayout,
) {
    val sourceDir: DirectoryProperty = objects.directoryProperty()
        .convention(layout.projectDirectory.dir("src/main/java"))

    val testOutputDir: DirectoryProperty = objects.directoryProperty()
        .convention(layout.projectDirectory.dir("src/test/java"))

    val reportOutputDir: DirectoryProperty = objects.directoryProperty()
        .convention(layout.buildDirectory.dir("reports/testgen"))

    val coverageReportFile: RegularFileProperty = objects.fileProperty()
        .convention(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))

    val packageIncludes: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())

    val packageExcludes: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())

    val minimumQualityScore: Property<Int> = objects.property(Int::class.java)
        .convention(50)

    val failOnSkippedClasses: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    val failOnFallbackMethods: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    val expectedTestTaskName: Property<String> = objects.property(String::class.java)
        .convention("")
}
