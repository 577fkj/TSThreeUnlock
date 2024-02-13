import java.io.File
import java.util.Base64
import org.gradle.api.tasks.Exec

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
}

val rsaData = buildRSAData()

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "cn.fkj233.tsunlocker"
        minSdk = 27
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PUBLIC_KEY", "\"${rsaData.publicKeyBase64}\"")
        buildConfigField("String", "PRIVATE_KEY", "\"${rsaData.privateKeyBase64}\"")
        buildConfigField("String", "LANG_URL", "\"https://raw.githubusercontent.com/577fkj/TSThreeUnlock/main/app/src/main/assets/app_assets/lang_zh.xml\"")

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

tasks.register("restartApp") {
    val packageName = "com.teamspeak.ts3client"
    val activityName = "com.teamspeak.ts3client.StartGUIFragment"

    doLast {
        // 停止应用程序
        exec {
            commandLine("adb", "shell", "am", "force-stop", packageName)
        }

        // 启动应用程序
        exec {
            commandLine("adb", "shell", "am", "start", "-n", "${packageName}/${activityName}")
        }
    }
}

tasks.named("preBuild") {
    doLast {
        println("Generating config file")
        generateConfigFile(rsaData.publicKey)
    }
}

fun generateConfigFile(byteArray: ByteArray) {
    val sb = StringBuilder()
    var count = 0
    for (i in byteArray.indices) {
        sb.append("0x%02X".format(byteArray[i]))
        if (i != byteArray.size - 1) {
            sb.append(", ")
        }
        if (count == 15) {
            sb.append("\n")
            sb.append("    ")
            count = 0
        } else {
            count++
        }
    }

    val configFile = File("${project.projectDir}/src/main/cpp/native/config.h")
    configFile.parentFile.mkdirs()

    configFile.writeText("""#ifndef CONFIG_H
#define CONFIG_H

unsigned char rsa_key[] = {
    $sb
};

#endif""")
}

fun buildRSAData(): RSAData {
    val publicKey = getRSAKeyPublic()
    val privateKey = getRSAKeyPrivate()
    println("publicKey: $publicKey")
    println("privateKey: $privateKey")
    return RSAData(Base64.getDecoder().decode(publicKey), Base64.getDecoder().decode(privateKey), publicKey, privateKey)
}

fun processRSAKey(key: String): String {
    val replaceList = listOf("-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----", "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----")
    var b64 = key
    replaceList.forEach {
        b64 = b64.replace(it, "")
    }
    b64 = b64
        .replace("\n", "")
        .replace("\r", "")
    return b64
}

fun getRSAKeyPublic(): String {
    val file = File("${project.rootDir}/keys/public.pem")
    if (file.exists()) {
        return processRSAKey(file.readText())
    }
    throw Exception("public key not found")
}

fun getRSAKeyPrivate(): String {
    val file = File("${project.rootDir}/keys/private.pem")
    if (file.exists()) {
        return processRSAKey(file.readText())
    }
    throw Exception("private key not found")
}

data class RSAData(val publicKey: ByteArray, val privateKey: ByteArray, val publicKeyBase64: String, val privateKeyBase64: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RSAData

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!privateKey.contentEquals(other.privateKey)) return false
        if (publicKeyBase64 != other.publicKeyBase64) return false
        if (privateKeyBase64 != other.privateKeyBase64) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + publicKeyBase64.hashCode()
        result = 31 * result + privateKeyBase64.hashCode()
        return result
    }
}


dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    implementation("com.highcapable.yukihookapi:api:1.1.8")
    ksp("com.highcapable.yukihookapi:ksp-xposed:1.1.8")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}