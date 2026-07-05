import com.android.build.api.artifact.SingleArtifact
import java.text.SimpleDateFormat
import java.util.Date

plugins {
  id("com.android.application")
}

kotlin {
  jvmToolchain(11)
}

android {
  namespace = "jp.takke.cpustats"

  compileSdk = rootProject.extra["compileSdkVersion"] as Int

  defaultConfig {
    applicationId = "jp.takke.cpustats"
    targetSdk = rootProject.extra["targetSdkVersion"] as Int
    minSdk = rootProject.extra["minSdkVersion"] as Int
    versionCode = rootProject.extra["versionCode"] as Int
    val versionName = rootProject.extra["versionName"] as String
    this.versionName = versionName

    val shortVersionName = versionName.replace(".", "")
    val d = SimpleDateFormat("yyyyMMdd_HHmm").format(Date())
    base.archivesName.set("${rootProject.extra["apkNamePrefix"]}_${shortVersionName}_${d}")
  }

  signingConfigs {
    create("myConfig") {
      // gradle.properties(存在すれば)から署名情報を注入
      if (project.hasProperty("storeFile")) {
        storeFile = file(project.property("storeFile") as String)
      }
      if (project.hasProperty("storePassword")) {
        storePassword = project.property("storePassword") as String
      }
      if (project.hasProperty("keyAlias")) {
        keyAlias = project.property("keyAlias") as String
      }
      if (project.hasProperty("keyPassword")) {
        keyPassword = project.property("keyPassword") as String
      }
    }
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("myConfig")
    }
    getByName("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "d"
    }
  }

  buildFeatures {
    buildConfig = true
    aidl = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

// release バリアントに publish{Variant}Apk / bundlePublish{Variant} タスクを生成し、
// publishAll から一括起動できるようにする
val publish = tasks.register("publishAll")
androidComponents {
  onVariants(selector().withBuildType("release")) { variant ->
    val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }

    val apkTask = tasks.register("publish${variantName}Apk", Copy::class) {
      from(variant.artifacts.get(SingleArtifact.APK)) {
        include("*.apk")
      }
      into(rootProject.extra["deployTo"] as String)
    }
    publish.configure { dependsOn(apkTask) }

    tasks.register("bundlePublish${variantName}", Copy::class) {
      from(variant.artifacts.get(SingleArtifact.BUNDLE))
      into(rootProject.extra["deployTo"] as String)
    }
  }
}

dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

  implementation(project(":modules:quad5"))

  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.annotation)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.preference.ktx)

  testImplementation(libs.robolectric)
  testImplementation(libs.junit)
  testImplementation(libs.assertj.core)

  implementation(libs.kotlin.stdlib.jdk7)
}
