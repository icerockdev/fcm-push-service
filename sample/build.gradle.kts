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
    maven { url = uri("https://dl.bintray.com/icerockdev/backend") }
}

application {
    mainClassName = "com.icerockdev.sample.Main"
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${properties["kotlin_version"]}")

//    implementation(project(":fcm-push-service"))
    implementation("com.icerockdev.service:fcm-push-service:0.0.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks.jar {
    archiveName = "sample.jar"
    destinationDir = file("${project.rootDir}/build")
    manifest {
        attributes(
                "Main-Class" to application.mainClassName,
                "Class-Path" to configurations.runtimeClasspath.get().files.joinToString(" ") { "libs/${it.name}" }
        )
    }
}
