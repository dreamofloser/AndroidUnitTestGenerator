plugins {
    java
    jacoco
    id("com.codex.android-testgen")
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.18.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

testGen {
    sourceDir.set(layout.projectDirectory.dir("src/main/java"))
    testOutputDir.set(layout.projectDirectory.dir("src/test/java"))
    reportOutputDir.set(layout.buildDirectory.dir("reports/testgen"))
    coverageReportFile.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
    packageIncludes.set(listOf("com.example.app"))
}
