import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import java.net.URI

val kotlinCoroutineVersion: String by extra { "1.1.0" }

buildscript {
    repositories {
        mavenCentral()
        maven("https://repo.spring.io/milestone")
    }

    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.0.0.RELEASE")
    }
}

apply {
    plugin("org.springframework.boot")
}

plugins {
    val kotlinVersion = "1.3.11"
    application

    id("org.jetbrains.kotlin.jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("io.spring.dependency-management") version "1.0.4.RELEASE"
}


group = "de.e2"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("http://repo.spring.io/milestone")
    maven { url = URI("http://dl.bintray.com/kotlin/kotlin-eap") }
    maven { url = URI("https://dl.bintray.com/kotlin/kotlinx") }
}

dependencies {
    compile("org.springframework.boot:spring-boot-starter-webflux")

    compile("org.jetbrains.kotlin:kotlin-stdlib")
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("org.jetbrains.kotlin:kotlin-reflect")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutineVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinCoroutineVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$kotlinCoroutineVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutineVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin")
    compile("io.github.microutils:kotlin-logging:1.4.4")
    compile("org.glassfish.jersey.core:jersey-client:2.26")
    compile("org.glassfish.jersey.inject:jersey-hk2:2.26")
    compile("org.glassfish.jersey.media:jersey-media-json-jackson:2.26")
    compile("com.jayway.jsonpath:json-path:2.3.0")
    compile("io.dropwizard.metrics:metrics-core:4.0.0")
    compile("io.reactivex.rxjava2:rxjava:2.2.2")

    testCompile("org.springframework.boot:spring-boot-starter-test")
    testCompile("io.projectreactor:reactor-test")
    testImplementation("io.mockk:mockk:1.8.12.kotlin13")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    testRuntimeOnly("net.bytebuddy:byte-buddy:1.9.3")
}

application {
    mainClassName = "de.e2.coroutine.reactor.ReactorApplication"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = "1.3"
    kotlinOptions.languageVersion = "1.3"
    kotlinOptions.freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
}