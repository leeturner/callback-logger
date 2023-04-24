plugins {
  id("org.jetbrains.kotlin.jvm") version "1.6.21"
  id("org.jetbrains.kotlin.kapt") version "1.6.21"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.6.21"
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("io.micronaut.application") version "3.7.8"
}

version = "0.1"

group = "com.leeturner.callback_logger"

val kotlinVersion = project.properties["kotlinVersion"]

repositories { mavenCentral() }

dependencies {
  kapt("io.micronaut.data:micronaut-data-processor")
  kapt("io.micronaut:micronaut-http-validation")
  kapt("io.micronaut.serde:micronaut-serde-processor")
  implementation("io.micronaut:micronaut-http-client")
  implementation("io.micronaut:micronaut-jackson-databind")
  implementation("io.micronaut.data:micronaut-data-jdbc")
  implementation("io.micronaut.serde:micronaut-serde-jackson")
  implementation("io.micronaut.kotlin:micronaut-kotlin-extension-functions")
  implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
  implementation("io.micronaut.sql:micronaut-jdbc-hikari")
  implementation("io.micronaut.views:micronaut-views-thymeleaf")
  implementation("io.micronaut:micronaut-validation")
  implementation("jakarta.annotation:jakarta.annotation-api")
  implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
  runtimeOnly("ch.qos.logback:logback-classic")
  implementation("com.h2database:h2")
  runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

  testImplementation("io.strikt:strikt-core:0.34.0")
}

application { mainClass.set("com.leeturner.callback.ApplicationKt") }

java { sourceCompatibility = JavaVersion.toVersion("17") }

tasks {
  compileKotlin { kotlinOptions { jvmTarget = "17" } }
  compileTestKotlin { kotlinOptions { jvmTarget = "17" } }
}

graalvmNative.toolchainDetection.set(false)

micronaut {
  runtime("netty")
  testRuntime("junit5")
  processing {
    incremental(true)
    annotations("com.leeturner.callback_logger.*")
  }
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("io.micronaut:micronaut-jackson-databind"))
        .using(module("io.micronaut.serde:micronaut-serde-jackson:1.5.2"))
  }
}
