// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  repositories {
    mavenCentral()
    google()
  }

  dependencies {
    classpath(libs.gradle)

    // Kotlin
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
  }
}

allprojects {
  repositories {
    mavenCentral()
    google()
  }
}

tasks.register<Delete>("clean") {
  delete(rootProject.layout.buildDirectory)
}

ext {
  set("apkNamePrefix", "CpuStats")
  set("versionName", "3.0.1")
  set("versionCode", 30)

  set("compileSdkVersion", 37)
  set("targetSdkVersion", 36)
  set("minSdkVersion", 23)
}