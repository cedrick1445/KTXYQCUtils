package com.yqc.ktxdng.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.preference.PreferenceManager
import android.text.TextUtils
import com.yqc.ktxdng.InstallAddTask
import com.yqc.ktxdng.SplashLietener
import com.yqc.ktxdng.ui.NoNetworkActivity
import com.yqc.ktxdng.ui.WebViewActivity

class YQCUtils {
    private val isHaveShowSplash = false
    private var downDialog: Dialog? = null

    /**
     * 启动页调用
     * 注意：在启动页的onResume方法里调用，需要点击返回时能重新启动跳转
     */
    fun splashAction(activity: Activity, splashLietener: SplashLietener?) {
        if (!isNetworkConnected(activity)) {
            activity.startActivity(Intent(activity, NoNetworkActivity::class.java))
        } else {
            if (!PreferenceManager.getDefaultSharedPreferences(activity)
                    .getBoolean("haveInstallAddOneTimes", false)
            ) {
                InstallAddTask(activity)
            }
            KaiGuanTask(activity, object : CheckCallback() {
                fun rePluginUpdate(version: Int, downUrl: String?) {
//                    if(splashLietener != null){
//                        splashLietener.rePluginUpdate(version,downUrl);
//                    }
                }

                fun downLoad(downUrl: String?) {
                    // FIXME: 2019/3/6 屏蔽唤醒功能，因为当需要更换不同的APP下载时（如六合宝典），唤醒了就不去下载APP了
//                    PackageManager packageManager = activity.getPackageManager();
//                    Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage("com.cz.game.yqcp");
//                    if (launchIntentForPackage != null){
//                        activity.startActivity(launchIntentForPackage);
//                    }else {
//                    }
                    DownloadTool.download(activity, downUrl)
                }

                fun goToWeb(webUrl: String?) {
                    WebViewActivity.launch(activity, webUrl)
                    activity.finish()
                    PreferenceManager.getDefaultSharedPreferences(activity).edit()
                        .putBoolean("haveOpenH5OnceTime", true).commit()
                }

                fun otherResponse(version: Int, downUrl: String?, webUrl: String?) {
                    if (downDialog != null && downDialog!!.isShowing) {
                        downDialog!!.dismiss()
                        downDialog = null
                    }
                    if (PreferenceManager.getDefaultSharedPreferences(activity)
                            .getBoolean("haveOpenH5OnceTime", false) && !TextUtils.isEmpty(webUrl)
                    ) {
                        goToWeb(webUrl)
                    } else {
                        if (splashLietener != null) {
                            splashLietener.startMySplash(version, downUrl)
                        }
                    }
                }
            })
        }
    }

    fun isNetworkConnected(context: Context?): Boolean {
        if (context != null) {
            val mConnectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.activeNetworkInfo
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable
            }
        }
        return false
    }
}