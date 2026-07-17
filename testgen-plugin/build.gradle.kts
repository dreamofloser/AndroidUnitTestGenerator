plugins {
    kotlin("jvm") version "2.2.0"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.dreamofloser.testgen"
version = "0.1.0-SNAPSHOT"

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.26.2")
    implementation(kotlin("compiler-embeddable"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")
}

gradlePlugin {
    plugins {
        create("androidTestgen") {
            id = "io.github.dreamofloser.android-testgen"
            implementationClass = "io.github.dreamofloser.testgen.AndroidTestGenPlugin"
            displayName = "Android Unit Test Generator"
            description = "Generates Android local unit test skeletons from Java and Kotlin source code."
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
