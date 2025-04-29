plugins {
  id("com.android.library")
  id("kotlin-android")
}

android {
  namespace = "jp.takke.cpustats.quad5"
  compileSdk = rootProject.ext.get("compileSdkVersion") as Int

  defaultConfig {
    minSdk = rootProject.ext.get("minSdkVersion") as Int
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = "11"
  }
}

dependencies {

}