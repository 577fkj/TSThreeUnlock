package cn.fkj233.tsunlocker.hook

import android.content.res.AssetManager
import android.util.Base64
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerE
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec


@InjectYukiHookWithXposed
class HookEntry : IYukiHookXposedInit {
    override fun onInit() = configs {
        debugLog {
            tag = "TS_Hook"
        }
    }

    data class LVL(val code: Int, val nonce: Int, val packName: String, val version: Int, val userId: String, val timestamp: Long = System.currentTimeMillis(), val extra: HashMap<String, String>) {
        fun getSignData(): String {
            val sb = StringBuilder()
            sb.append("$code")
            sb.append("|$nonce")
            sb.append("|$packName")
            sb.append("|$version")
            sb.append("|$userId")
            sb.append("|$timestamp")
            if (extra.isNotEmpty()) {
                sb.append(":")
                val list = arrayListOf<String>()
                extra.forEach {
                    list.add("${it.key}=${it.value}")
                }
                sb.append(list.joinToString("&"))
            }
            return sb.toString()
        }

        fun getSign(encodedKey: String): String {
            val decodedKey = Base64.decode(encodedKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance("RSA")
            val private = keyFactory.generatePrivate(PKCS8EncodedKeySpec(decodedKey))
            val sig = Signature.getInstance("SHA1withRSA")
            sig.initSign(private)
            sig.update(getSignData().toByteArray())
            return Base64.encodeToString(sig.sign(), Base64.DEFAULT)
        }
    }

    private val lvl = LVL(0, 114514, "com.teamspeak.ts3client", 299, "114514", extra = hashMapOf(
        "GR" to "10",
        "VT" to "4102372799000", // 2099-12-31 11:59:59
        "GT" to "4102372799000",
    ))

    private val privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDAA6QaaVOSyCZpGxofrH9ebmUncsIg+vXwEh4Npurbx/Oo/ByYmUih/20/pDyiWjRAFLI095F6+lVZQt742Z/uqfTxKBCmZALJp/iha5eP6rgAflwmE9FtHjWUueaSV/wBbIQ13SF57S9UjF9qDl9toXN0hIbOk+cSDzjVpBc5ApJOwHLeLphjpSGDeFnln188E0NerUnD6JtHlp7mzpfmoEq7Xp41hzEddDpGmK6pGRckM+woBM6nqNgyXDVrzoITU8JahZ6GhoDi1etcq8bM5zsJVUarmDw6F+7o37KAnq09UWVSPBQsQ6PXJS4f0xxrd7YoswywbxHdmgpR2HLXAgMBAAECggEAZWKGx5tNkXc1MNIqnQbLyeSobIffkOJx8WzfiX3VDG8rbNKRltF41HC2EqB5k4/lfuBuqCEAI9sCLbttWCAwnigHOT5abEDqZ+fVFAZnZIoe1cPijAhy0BbfW8Q8xpDGc0oFFRvJjUN7dj0YNDuaj5xsBd3kADeADr0S2VtnF6YkkRzwxJ86KbZpiUJAFC4HxxMohesq3jWzzTGLrk5YIditmD3I4jyb20FQi0xrT4pcktHsvYndaBbBDkbZz1q4rBQAkq6uMb6ZTZjlTz4+3gGWohPENVwYtTO0USgsw/R/wgvsWJjb4IerlTibc3rkLYAbNgILgG3IemrczjcLYQKBgQD1zsh+LOQ9mqFIRJv92gq54k3BLkFW2lLCo7w6ToVjzZ14vN0i1gYo09rcE2eRfuTJK3r0xW4l0gEJQBZQ02DOQMMBowgFQhZK8P1BrCmB8lnWQkPmzerS5K4wc1kFIhrjq4whrvqBx947pRE6ReRK0Grm0a+4nAtfzsgRP2/zlQKBgQDH+djAgqdP29xg5cLP0hFgVYSlXbMcmOnXykDknFrK7S3pChxE5OS95PK8tk6CT3pRe/lVBJ1ieuVSU7hTC76TuWCwzldUbu0vQWcgaVBcfu6oXjxEEhyxBnz/QlQPjelohMChUm59KhpzI1b5LV1oDFsEJVkxcFJ3YCTOeyoxuwKBgGJtykUULtUoTTsGFjzhifgDUcVwg0OHKyq7rAzhEnLzjAIkBp0DfPXuV65WqttNJ8oSeir3v/KvFDEjE4yMCDCvKCdIpFaOrySVOaSJjxl09VphsJUEkeqfHc1S8yumC4RtVjDKw48ifK//mlVuVUtMB/KjtqzC958Gy4B0mZy9AoGAWHF23MWsK9SoZi6X94QH7VxFO0Hyqo4yth6cjr9cJG27pVq0X///7sBoXvAeTuHJzmoPPvu4g8qoy719QzVphzowumG4G/6nZgP+EUaipRX6hXqQGucLy3t8IwT52ptABNrZuz+S4EaTxiJ2H+RhcFgulOnTxdrQjj12ad/tLGsCgYATHcyce4Lp+RVYOEFoqtbulgd05NvEBgV1SYa3F7WTv+9GTBU8aNOvRv0Hpot+SPFgfsQJ4IcLdq+UJmCOsyOPZ5qYJ0SCEH+PoADgQlmcNA8xW/ViHlorUvT0/WosKFqrJKToNvIduiaBPuyHdSbv/iQdNrASRvXsPAU3Ln3mZg=="
    private val publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwAOkGmlTksgmaRsaH6x/Xm5lJ3LCIPr18BIeDabq28fzqPwcmJlIof9tP6Q8olo0QBSyNPeRevpVWULe+Nmf7qn08SgQpmQCyaf4oWuXj+q4AH5cJhPRbR41lLnmklf8AWyENd0hee0vVIxfag5fbaFzdISGzpPnEg841aQXOQKSTsBy3i6YY6Uhg3hZ5Z9fPBNDXq1Jw+ibR5ae5s6X5qBKu16eNYcxHXQ6RpiuqRkXJDPsKATOp6jYMlw1a86CE1PCWoWehoaA4tXrXKvGzOc7CVVGq5g8Ohfu6N+ygJ6tPVFlUjwULEOj1yUuH9Mca3e2KLMMsG8R3ZoKUdhy1wIDAQAB"

    override fun onHook() = encase {
        loadApp(name = "com.teamspeak.ts3client") {
            System.loadLibrary("native")

            // Wtf, use yuki api appear bug
            XposedHelpers.findAndHookMethod(AssetManager::class.java, "open", String::class.java, Int::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val path = param.args[0].toString()
                    loggerD(msg = "open: $path")
                    if (path.startsWith("app_assets")) {
                        return
                    }
                    if (path == "lang/lang_eng.xml") {
                        loggerD(msg = "replace lang_eng.xml to lang_zh.xml")
                        param.result = moduleAppResources.assets.open(
                            "app_assets/lang_zh.xml",
                            AssetManager.ACCESS_STREAMING
                        )
                    } else if (path.startsWith("sound/female/")){
                        param.result = moduleAppResources.assets.open(
                            "app_assets/$path",
                            AssetManager.ACCESS_STREAMING
                        )
                    }
                }
            })

            XposedHelpers.findAndHookMethod(AssetManager::class.java, "openFd", String::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val path = param.args[0].toString()
                    loggerD(msg = "openFd: $path")
                    if (path.startsWith("app_assets")) {
                        return
                    }
                    if (path.startsWith("sound/female/")){
                        param.result = moduleAppResources.assets.openFd(
                            "app_assets/$path"
                        )
                    }
                }
            })

            findClass(name = "k3.m").hook {
                injectMember {
                    method {
                        name = "f"
                    }.onNoSuchMethod {
                        loggerE(msg = "error:", e = it)
                    }
                    replaceUnit {
                        args[0]?.javaClass?.method {
                            name = "a"
                        }?.get(args[0])?.call(256)
                        args[0]?.javaClass?.method {
                            name = "c"
                        }?.get(args[0])?.call(lvl.getSignData(), lvl.getSign(privateKey), lvl.code, lvl.nonce, lvl.version.toString())
                    }
                }.onHooked { member ->
                    loggerD(msg = "$member has hooked")
                }
                injectMember {
                    method {
                        name = "j"
                    }
                    beforeHook {
                        args[0] = publicKey
                    }
                }
            }
        }
    }
}