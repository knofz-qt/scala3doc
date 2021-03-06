import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    id("java")
    id("maven-publish")
}


group = "org.jetbrains.dokka"
version = "0.1.1-SNAPSHOT"

val language_version: String by project

tasks.withType(KotlinCompile::class).all {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict -Xskip-metadata-version-check -Xopt-in=kotlin.RequiresOptIn."
        languageVersion = language_version
        apiVersion = language_version
        jvmTarget = "1.8"
    }
}

repositories {
    jcenter()
    mavenCentral()
    mavenLocal()
    maven("http://dl.bintray.com/virtuslab/dokka/")
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-core:1.4.0")
    implementation("org.jetbrains.dokka:dokka-base:1.4.0")
    implementation("com.virtuslab.dokka:dokka-site:0.1.7")
}

apply {
    plugin("org.jetbrains.kotlin.jvm")
    plugin("java")
}

// Gradle metadata
java {
    @Suppress("UnstableApiUsage")
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "scala3doc"
            artifactId = "dokka-java-api"
            version = "0.1.1-SNAPSHOT"

            from(components["java"])
        }
    }
}

// Workaround for https://github.com/bintray/gradle-bintray-plugin/issues/267
//  Manually disable bintray tasks added to the root project
tasks.whenTaskAdded {
    if ("bintray" in name) {
        enabled = false
    }
}
