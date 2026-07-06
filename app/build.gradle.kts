import com.android.build.api.artifact.SingleArtifact
import java.text.SimpleDateFormat
import java.util.Date

plugins {
  id("com.android.application")
  alias(libs.plugins.kotlin.compose)
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

    // 日時は publish タスクの rename で埋め込むため、ここでは含めない(Configuration Cache 対応)
    val shortVersionName = versionName.replace(".", "")
    base.archivesName.set("${rootProject.extra["apkNamePrefix"]}_${shortVersionName}")
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
    compose = true
  }

  testOptions {
    // Robolectric からアプリリソースを参照できるようにする
    unitTests.isIncludeAndroidResources = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

// release バリアントに publish{Variant}Apk / bundlePublish{Variant} タスクを生成し、
// publishAll から一括起動できるようにする
//
// ファイル名の日時 (yyyyMMdd_HHmm) は Copy タスクの rename で埋め込む。
// archivesName を Configuration フェーズで決定(日時抜き)→ Execution フェーズの rename で日時を埋め込む、
// という分離により Configuration Cache を無効化せずファイル名のタイムスタンプを最新に保つ。
// 例: "CpuStats_225-release.apk" -> "CpuStats_225_20260706_1234-release.apk"
// ※rename のラムダは Configuration Cache のシリアライズ問題を避けるため何もキャプチャしないこと
val publish = tasks.register("publishAll")
androidComponents {
  onVariants(selector().withBuildType("release")) { variant ->
    val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }

    val apkTask = tasks.register("publish${variantName}Apk", Copy::class) {
      from(variant.artifacts.get(SingleArtifact.APK)) {
        include("*.apk")
      }
      into(rootProject.extra["deployTo"] as String)
      rename { fileName ->
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm").format(Date())
        val dashIdx = fileName.indexOf("-")
        if (dashIdx < 0) "${fileName}_$timestamp"
        else "${fileName.substring(0, dashIdx)}_$timestamp${fileName.substring(dashIdx)}"
      }
    }
    publish.configure { dependsOn(apkTask) }

    tasks.register("bundlePublish${variantName}", Copy::class) {
      from(variant.artifacts.get(SingleArtifact.BUNDLE))
      into(rootProject.extra["deployTo"] as String)
      rename { fileName ->
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm").format(Date())
        val dashIdx = fileName.indexOf("-")
        if (dashIdx < 0) "${fileName}_$timestamp"
        else "${fileName.substring(0, dashIdx)}_$timestamp${fileName.substring(dashIdx)}"
      }
    }
  }
}

dependencies {
  implementation(project(":modules:quad5"))

  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.annotation)
  implementation(libs.kotlinx.coroutines.android)

  // Compose
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  debugImplementation(libs.androidx.compose.ui.tooling)

  testImplementation(composeBom)
  testImplementation(libs.robolectric)
  testImplementation(libs.junit)
  testImplementation(libs.assertj.core)
}
