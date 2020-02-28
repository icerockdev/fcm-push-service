/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

group = "com.icerockdev.service"
version = "0.0.2"

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("maven-publish")
    id("java-library")
}

apply(plugin = "kotlin")

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

dependencies {
    // kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${properties["kotlin_version"]}")
    // logging
    implementation("ch.qos.logback:logback-classic:${properties["logback_version"]}")
    implementation("io.ktor:ktor-client-apache:${properties["ktor_version"]}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${properties["coroutines_version"]}")
    api("io.ktor:ktor-client-logging-jvm:${properties["ktor_version"]}")

    implementation("io.ktor:ktor-client-jackson:${properties["ktor_version"]}")

    // tests
    testImplementation("io.ktor:ktor-server-tests:${properties["ktor_version"]}")

    testImplementation("io.ktor:ktor-client-mock:${properties["ktor_version"]}")
    testImplementation("io.ktor:ktor-client-mock-jvm:${properties["ktor_version"]}")
    testImplementation("io.ktor:ktor-client-mock-js:${properties["ktor_version"]}")
    testImplementation("io.ktor:ktor-client-mock-native:${properties["ktor_version"]}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

repositories {
    mavenCentral()
}

publishing {
    repositories.maven("https://api.bintray.com/maven/icerockdev/backend/fcm-push-service/;publish=1") {
        name = "bintray"

        credentials {
            username = System.getProperty("BINTRAY_USER")
            password = System.getProperty("BINTRAY_KEY")
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}