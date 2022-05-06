/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    application
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("idea")
}

group = "com.icerockdev"
version = "0.0.1"

apply(plugin = "java")
apply(plugin = "kotlin")

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.icerockdev.sample.Main")
}


dependencies {
    implementation(project(":fcm-push-service"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

tasks.jar {
    archiveFileName.set("sample.jar")
    destinationDirectory.set(file("${project.rootDir}/build"))
    manifest {
        attributes(
            "Main-Class" to application.mainClass,
            "Class-Path" to configurations.runtimeClasspath.get().files.joinToString(" ") { "libs/${it.name}" }
        )
    }
}
