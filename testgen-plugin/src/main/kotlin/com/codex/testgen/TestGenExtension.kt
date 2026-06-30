package com.codex.testgen

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
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

    val packageIncludes: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())

    val packageExcludes: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())
}
