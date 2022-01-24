import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.6.10"
}

group = "me.bartlomiejszal"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val cassandraDriverVersion = "4.13.0"

dependencies {
    kapt("com.datastax.oss:java-driver-mapper-processor:$cassandraDriverVersion")
    implementation("com.datastax.oss", "java-driver-mapper-runtime", cassandraDriverVersion)
    implementation("io.github.microutils", "kotlin-logging-jvm", "2.1.20")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}