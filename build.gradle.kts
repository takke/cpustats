// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  repositories {
    mavenCentral()
    google()
  }

  dependencies {
    classpath("com.android.tools.build:gradle:9.3.1")

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
  set("versionName", "3.0.0")
  set("versionCode", 29)

  set("compileSdkVersion", 37)
  set("targetSdkVersion", 35)
  set("minSdkVersion", 23)
}