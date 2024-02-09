package cn.fkj233.tsunlocker.hook

import android.content.res.AssetManager
import android.util.Base64
import cn.fkj233.tsunlocker.BuildConfig
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
                        }?.get(args[0])?.call(lvl.getSignData(), lvl.getSign(BuildConfig.PRIVATE_KEY), lvl.code, lvl.nonce, lvl.version.toString())
                    }
                }.onHooked { member ->
                    loggerD(msg = "$member has hooked")
                }
                injectMember {
                    method {
                        name = "j"
                    }
                    beforeHook {
                        args[0] = BuildConfig.PUBLIC_KEY
                    }
                }
            }
        }
    }
}