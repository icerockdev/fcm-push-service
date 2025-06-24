/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */
import java.util.Base64
import org.jreleaser.model.Active

group = "com.icerockdev.service"
version = "2.2.0"

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin-kapt")
    id("maven-publish")
    id("java-library")
    id("signing")
    id("org.jreleaser") version "1.18.0"
}

apply(plugin = "kotlin")

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

dependencies {
    // logging
    implementation("ch.qos.logback:logback-classic:${properties["logback_version"]}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${properties["coroutines_version"]}")

    implementation("com.google.firebase:firebase-admin:${properties["firebase_admin_sdk_version"]}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

repositories {
    mavenCentral()
}

val publishRepositoryName = "maven-central-portal-deploy"
publishing {
    repositories.maven(layout.buildDirectory.dir(publishRepositoryName))
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
            pom {
                name.set("Fcm push service")
                description.set("Tools for sending fcm push by kotlin")
                url.set("https://github.com/icerockdev/fcm-push-service")
                licenses {
                    license {
                        url.set("https://github.com/icerockdev/fcm-push-service/blob/master/LICENSE.md")
                    }
                }

                developers {
                    developer {
                        id.set("YokiToki")
                        name.set("Stanislav")
                        email.set("skarakovski@icerockdev.com")
                    }

                    developer {
                        id.set("AlexeiiShvedov")
                        name.set("Alex Shvedov")
                        email.set("ashvedov@icerockdev.com")
                    }

                    developer {
                        id.set("oyakovlev")
                        name.set("Oleg Yakovlev")
                        email.set("oyakovlev@icerockdev.com")
                    }
                }

                scm {
                    connection.set("scm:git:ssh://github.com/icerockdev/fcm-push-service.git")
                    developerConnection.set("scm:git:ssh://github.com/icerockdev/fcm-push-service.git")
                    url.set("https://github.com/icerockdev/fcm-push-service")
                }
            }
        }

        signing {
            setRequired({ !properties.containsKey("libraryPublishToMavenLocal") })
            val signingKeyId: String? = System.getenv("SIGNING_KEY_ID")
            val signingPassword: String? = System.getenv("SIGNING_PASSWORD")
            val signingKey: String? = System.getenv("SIGNING_KEY")?.let { base64Key ->
                String(Base64.getDecoder().decode(base64Key))
            }
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            sign(publishing.publications["mavenJava"])
        }
    }
}

jreleaser {
    gitRootSearch = true
    release {
        generic {
            skipRelease = true
            skipTag = true
            changelog {
                enabled = false
            }
            token = "EMPTY"
        }
    }
    deploy {
        maven {
            mavenCentral.create("sonatype") {
                enabled = !properties.containsKey("libraryPublishToMavenLocal")
                applyMavenCentralRules = true
                sign = false
                active = Active.ALWAYS
                url = "https://central.sonatype.com/api/v1/publisher"
                stagingRepository(layout.buildDirectory.dir(publishRepositoryName).get().toString())
                setAuthorization("Basic")
                retryDelay = 60
                username = System.getenv("OSSRH_USER")
                password = System.getenv("OSSRH_KEY")
            }
        }
    }
}
