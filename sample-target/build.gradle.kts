plugins {
    java
    id("com.codex.android-testgen")
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

testGen {
    sourceDir.set(layout.projectDirectory.dir("src/main/java"))
    testOutputDir.set(layout.projectDirectory.dir("src/test/java"))
    reportOutputDir.set(layout.buildDirectory.dir("reports/testgen"))
    packageIncludes.set(listOf("com.example.app"))
}
