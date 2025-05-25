// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  repositories {
    mavenCentral()
    google()
  }

  dependencies {
    classpath("com.android.tools.build:gradle:${libs.versions.agp.get()}")

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
  set("versionName", "2.2.4")
  set("versionCode", 28)

  set("compileSdkVersion", 35)
  set("buildToolsVersion", "35.0.0")
  set("targetSdkVersion", 35)
  set("minSdkVersion", 23)
}