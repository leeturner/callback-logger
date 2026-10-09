plugins {
  id("org.jetbrains.kotlin.jvm") version "2.4.21"
  id("org.jetbrains.kotlin.plugin.allopen") version "2.4.21"
  id("com.google.devtools.ksp") version "2.3.12"
  id("com.gradleup.shadow") version "9.6.1"
  id("io.micronaut.application") version "5.0.2"
  id("io.micronaut.aot") version "5.0.2"
}

version = "0.1"
group = "com.leeturner.callback_logger"

val kotlinVersion = providers.gradleProperty("kotlinVersion").get()

repositories {
  maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
    mavenContent { snapshotsOnly() }
  }
  mavenCentral()
}

dependencies {
  ksp("io.micronaut.serde:micronaut-serde-processor")
  implementation("io.micronaut.serde:micronaut-serde-jackson")
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

  testImplementation(platform("org.junit:junit-bom:6.1.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("io.strikt:strikt-core:0.35.1")
  testImplementation("io.micronaut.test:micronaut-test-rest-assured")
}

application { mainClass.set("com.leeturner.callback_logger.ApplicationKt") }

java {
  sourceCompatibility = JavaVersion.toVersion("25")
  targetCompatibility = JavaVersion.toVersion("25")
}

kotlin { jvmToolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }

graalvmNative.toolchainDetection.set(false)

micronaut {
  runtime("netty")
  testRuntime("junit5")
  processing {
    incremental(true)
    annotations("com.leeturner.callback_logger.*")
  }
  aot {
    // Please review carefully the optimizations enabled below
    // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
    optimizeServiceLoading.set(false)
    convertYamlToJava.set(false)
    precomputeOperations.set(true)
    cacheEnvironment.set(true)
    optimizeClassLoading.set(true)
    deduceEnvironment.set(true)
    optimizeNetty.set(true)
  }
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
  baseImage("eclipse-temurin:25-jre")
}


tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
  jdkVersion.set("25")
}
