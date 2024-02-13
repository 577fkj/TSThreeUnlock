package cn.fkj233.tsunlocker.hook

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.AssetManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.util.Base64
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import cn.fkj233.tsunlocker.BuildConfig
import cn.fkj233.tsunlocker.R
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.log.loggerE
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.io.File
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

    @SuppressLint("DiscouragedApi")
    fun Context.getIdentifier(name: String): Int {
        val names = name.split(".")
        return resources.getIdentifier(names[1], names[0], packageName)
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

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun downloadFile(context: Context, url: String, fileName: String) {
        val downloadId = context.getSystemService(DownloadManager::class.java).enqueue(DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("下载文件")
            setDescription("正在下载文件")
            setDestinationInExternalFilesDir(context, null, fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        })
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                val id = intent.getLongExtra(
                    DownloadManager.EXTRA_DOWNLOAD_ID,
                    -1
                )
                if (id == downloadId) {
                    Toast.makeText(
                        context,
                        "任务:$downloadId 下载完成!",
                        Toast.LENGTH_LONG
                    ).show()
                    context.unregisterReceiver(this)
                }
            }
        }
        context.registerReceiver(broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    @SuppressLint("SetTextI18n")
    override fun onHook() = encase {
        loadApp(name = "com.teamspeak.ts3client") {
            System.loadLibrary("native")

            val assetList = moduleAppResources.assets.list("app_assets")
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
                        val file = File(appContext?.getExternalFilesDir(null), "lang_zh.xml")
                        if (file.exists() && !BuildConfig.DEBUG) {
                            if (
                                runCatching {
                                    param.result = file.inputStream()
                                }.isSuccess
                            ) {
                                loggerD(msg = "use external lang_zh.xml")
                                return
                            }
                        }
                        param.result = moduleAppResources.assets.open(
                            "app_assets/lang_zh.xml",
                            AssetManager.ACCESS_STREAMING
                        )
                    } else if (path.startsWith("sound/female/")){
                        if (assetList?.contains(path) == true) {
                            param.result = moduleAppResources.assets.open(
                                "app_assets/$path",
                                AssetManager.ACCESS_STREAMING
                            )
                        }
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
                        if (assetList?.contains(path) == true) {
                            param.result = moduleAppResources.assets.openFd(
                                "app_assets/$path"
                            )
                        }
                    }
                }
            })

            findClass(name = "com.teamspeak.ts3client.settings.w").hook {
                injectMember {
                    method {
                        name = "f3"
                    }
                    beforeHook {
                        val view = instance.javaClass.field {
                            type = LinearLayout::class.java
                        }.get(instance).cast<LinearLayout>()!!
                        val context = view.context
                        val appResources = context.resources
                        view.addView(LinearLayout(context).apply {
                            setOnClickListener {
                                AlertDialog.Builder(context)
                                    .setTitle("关于")
                                    .setMessage("当前版本: ${BuildConfig.VERSION_NAME}\n作者：${moduleAppResources.getString(R.string.author)}")
                                    .setNegativeButton("更新汉化") { dialog, _ ->
                                        dialog.dismiss()
                                        Toast.makeText(context, "开始下载", Toast.LENGTH_SHORT).show()
                                        downloadFile(context, BuildConfig.LANG_URL, "lang_zh.xml")
                                    }
                                    .setPositiveButton("关闭", null)
                                    .show()
                            }
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(-1, -2)
                            setBackgroundResource(context.getIdentifier("drawable.state_menu"))
                            val dimensionPixelSize = appResources.getDimensionPixelSize(context.getIdentifier("dimen.settings_padding_lr"))
                            val dimensionPixelSize2 = appResources.getDimensionPixelSize(context.getIdentifier("dimen.settings_padding_ud"))
                            setPadding(
                                dimensionPixelSize,
                                dimensionPixelSize2,
                                dimensionPixelSize,
                                dimensionPixelSize2
                            )
                            addView(TextView(context).apply {
                                textSize = 20.0f
                                setTypeface(null, Typeface.BOLD)
                                setTextColor(Color.WHITE)
                                text = "TSThree设置"
                            })
                            addView(TextView(context).apply {
                                textSize = 10.0f
                                setTypeface(null, Typeface.ITALIC)
                                setTextColor(Color.GRAY)
                                text = "当前版本: ${BuildConfig.VERSION_NAME}"
                            })
                        })
                    }
                }
            }

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