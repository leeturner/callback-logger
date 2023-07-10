plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.22"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.8.22"
  id("com.google.devtools.ksp") version "1.8.22-1.0.11"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("io.micronaut.application") version "4.0.0-M8"
}

version = "0.1"

group = "com.leeturner.callback_logger"

val kotlinVersion = project.properties["kotlinVersion"]

repositories { mavenCentral() }

dependencies {
  implementation("io.micronaut:micronaut-jackson-databind")
  implementation("io.micronaut.data:micronaut-data-jdbc")
  implementation("io.micronaut.kotlin:micronaut-kotlin-extension-functions")
  implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
  implementation("io.micronaut.sql:micronaut-jdbc-hikari")
  implementation("io.micronaut.views:micronaut-views-thymeleaf")
  implementation("jakarta.annotation:jakarta.annotation-api")
  implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
  runtimeOnly("ch.qos.logback:logback-classic")
  implementation("com.h2database:h2")

  testImplementation(platform("org.junit:junit-bom:5.9.3"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("io.strikt:strikt-core:0.34.1")
  testImplementation("io.micronaut.test:micronaut-test-rest-assured")
}

application { mainClass.set("com.leeturner.callback_logger.ApplicationKt") }

java { 
  sourceCompatibility = JavaVersion.toVersion("17")
  targetCompatibility = JavaVersion.toVersion("17")
}

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
