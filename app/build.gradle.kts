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
        versionName = rootProject.extra["versionName"] as String

        val shortVersionName = versionName.replace(".", "")
        val d = SimpleDateFormat("yyyyMMdd_HHmm").format(java.util.Date())
        setProperty("archivesBaseName", "${rootProject.extra["apkNamePrefix"]}_${shortVersionName}_${d}")
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

    flavorDimensions += "edition"

    productFlavors {
        create("quad3") {
            dimension = "edition"
        }
        create("quad5") {
            dimension = "edition"
        }
    }

    val publish = tasks.register("publishAll")
    applicationVariants.all { variant ->
        if (variant.buildType.name == "release") {
            variant.outputs.forEach { output ->
                if (output.outputFileName.endsWith(".apk")) {
                    val task = tasks.register("publish${variant.name.capitalize()}Apk", Copy::class) {
                        val srcDir = variant.packageApplicationProvider.get().outputDirectory.get().asFile
                        val srcPath = srcDir.resolve(output.outputFileName)
                        from(srcPath)
                        into(rootProject.extra["deployTo"] as String)
                        dependsOn(variant.assembleProvider.get())
                    }
                    publish.configure { dependsOn(task) }
                }
            }
        }
    }

    applicationVariants.all { variant ->
        if (variant.buildType.name == "release") {
            val flavorName0 = variant.productFlavors[0].name
            val buildTypeName = variant.buildType.name

            val task = tasks.register("bundlePublish${variant.name.capitalize()}", Copy::class) {
                val aabFilename = "${rootProject.extra["base.archivesName"]}-$flavorName0-$buildTypeName.aab"
                val path = "${layout.buildDirectory.get()}/outputs/bundle/${variant.name}/$aabFilename"
                println("${name}: $path -> ${rootProject.extra["deployTo"]}")
//                println("*** aab path = $path ($task) [$flavorName0] [$buildTypeName]")
                from(path)
                into(rootProject.extra["deployTo"] as String)
                dependsOn("bundle${variant.name.capitalize()}")
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
    android.signingConfigs.getByName("myConfig").storeFile = file(project.property("storeFile") as String)
}
if (project.hasProperty("storePassword")) {
    android.signingConfigs.getByName("myConfig").storePassword = project.property("storePassword") as String
}
if (project.hasProperty("keyAlias")) {
    android.signingConfigs.getByName("myConfig").keyAlias = project.property("keyAlias") as String
}
if (project.hasProperty("keyPassword")) {
    android.signingConfigs.getByName("myConfig").keyPassword = project.property("keyPassword") as String
}