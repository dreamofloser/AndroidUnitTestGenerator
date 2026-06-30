plugins {
    kotlin("jvm") version "2.2.0"
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.26.2")

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")
}

gradlePlugin {
    plugins {
        create("androidTestgen") {
            id = "com.codex.android-testgen"
            implementationClass = "com.codex.testgen.AndroidTestGenPlugin"
            displayName = "Android Unit Test Generator"
            description = "Generates Android local unit test skeletons from Java source code."
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
