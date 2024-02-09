import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "cn.fkj233.tsunlocker"
        minSdk = 27
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                targets("native")
                abiFilters("arm64-v8a")
                cppFlags("-std=c++17")
                cFlags("-std=gnu99")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isZipAlignEnabled = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.majorVersion
        freeCompilerArgs += "-Xno-param-assertions"
        freeCompilerArgs += "-Xno-call-assertions"
        freeCompilerArgs += "-Xno-receiver-assertions"
    }
    buildFeatures {
        viewBinding = true
    }
    lintOptions {
        isCheckReleaseBuilds = false
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
    androidResources {
        noCompress("libnative.so")
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/**"
            excludes += "/kotlin/**"
            excludes += "/*.txt"
            excludes += "/*.bin"
            excludes += "/*.json"
            excludes += "/bsh/**"
            excludes += "/okhttp3/**"
        }
        dex {
            useLegacyPackaging = true
        }
    }
    ndkVersion = "21.1.6352462"
    namespace = "cn.fkj233.tsunlocker"

    afterEvaluate {
        tasks.named("assembleRelease").configure {
            doLast {
                // 在这里添加你想要执行的命令
                exec {
                    commandLine("java", "-jar", "../tools/lspatch-336.jar", "../Teamspeak_3.4.5.apk", "-m", "release/app-release.apk", "--force")
                    val sourceFile = file(projectDir.absolutePath + "/Teamspeak_3.4.5-336-lspatched.apk")
                    val destinationFile = file(projectDir.absolutePath + "/release/Teamspeak_3.4.5-336-lspatched.apk")
                    if (sourceFile.exists()) {
                        sourceFile.renameTo(destinationFile)
                        sourceFile.delete()
                    } else {
                        println("修补后的文件不存在！")
                    }
                }
            }
        }
    }
}


dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    implementation("com.highcapable.yukihookapi:api:1.1.8")
    ksp("com.highcapable.yukihookapi:ksp-xposed:1.1.8")
    implementation("com.github.duanhong169:drawabletoolbox:1.0.7")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}