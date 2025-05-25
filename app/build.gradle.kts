import java.text.SimpleDateFormat
import java.util.Date

plugins {
  id("com.android.application")
  id("kotlin-android")
}

kotlin {
  jvmToolchain(11)
}

android {
  signingConfigs {
    create("myConfig")
  }

  namespace = "jp.takke.cpustats"

  compileSdk = rootProject.extra["compileSdkVersion"] as Int
  buildToolsVersion = rootProject.extra["buildToolsVersion"] as String

  defaultConfig {
    applicationId = "jp.takke.cpustats"
    targetSdk = rootProject.extra["targetSdkVersion"] as Int
    minSdk = rootProject.extra["minSdkVersion"] as Int
    versionCode = rootProject.extra["versionCode"] as Int
    val versionName = rootProject.extra["versionName"] as String
    this.versionName = versionName

    val shortVersionName = versionName.replace(".", "")
    val d = SimpleDateFormat("yyyyMMdd_HHmm").format(Date())
    setProperty(
      "archivesBaseName",
      "${rootProject.extra["apkNamePrefix"]}_${shortVersionName}_${d}"
    )
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

  val publish = tasks.register("publishAll")
  applicationVariants.all {
    if (buildType.name == "release") {
      outputs.forEach { output ->
        if (output.outputFile.name.endsWith(".apk")) {
          @Suppress("DEPRECATION")
          val variantName = name.capitalize()
          val task = tasks.register("publish${variantName}Apk", Copy::class) {
            val srcDir = packageApplicationProvider.get().outputDirectory.get().asFile
            val srcPath = srcDir.resolve(output.outputFile.name)
            from(srcPath)
            into(rootProject.extra["deployTo"] as String)
            dependsOn(assembleProvider.get())
          }
          publish.configure { dependsOn(task) }
        }
      }
    }
  }

  applicationVariants.all {
    if (buildType.name == "release") {
      val buildTypeName = buildType.name

      @Suppress("DEPRECATION")
      val variantName = name.capitalize()
      tasks.register("bundlePublish${variantName}", Copy::class) {
        val aabFilename = "${base.archivesName.get()}-${buildTypeName}.aab"
        val path = "${layout.buildDirectory.get()}/outputs/bundle/${variantName}/$aabFilename"
        println("${name}: $path -> ${rootProject.extra["deployTo"]}")
//                println("*** aab path = $path ($task) [$flavorName0] [$buildTypeName]")
        from(path)
        into(rootProject.extra["deployTo"] as String)
        dependsOn("bundle${variantName}")
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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

// load signing settings from gradle.properties
if (project.hasProperty("storeFile")) {
  android.signingConfigs.getByName("myConfig").storeFile =
    file(project.property("storeFile") as String)
}
if (project.hasProperty("storePassword")) {
  android.signingConfigs.getByName("myConfig").storePassword =
    project.property("storePassword") as String
}
if (project.hasProperty("keyAlias")) {
  android.signingConfigs.getByName("myConfig").keyAlias = project.property("keyAlias") as String
}
if (project.hasProperty("keyPassword")) {
  android.signingConfigs.getByName("myConfig").keyPassword =
    project.property("keyPassword") as String
}