package com.yqc.ktxdng

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Looper
import android.preference.PreferenceManager
import android.text.TextUtils
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class InstallAddTask {

    fun InstallAddTask(activity: Activity) {
        Thread { getCheckInfo(activity) }.start()
    }

    private fun getCheckInfo(activity: Activity) {
        Looper.prepare()
        try {
            val url = URL(String.format("http://%s/jeesite/f/guestbook/install?", getURL(activity)))
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connectTimeout = 5000
            urlConnection.readTimeout = 5000
            urlConnection.doOutput = true
            urlConnection.doInput = true
            urlConnection.useCaches = false
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            urlConnection.setRequestProperty("Connection", "Keep-Alive")
            urlConnection.setRequestProperty("Charset", "UTF-8")
            urlConnection.connect()
            val os = urlConnection.outputStream
            os.write(getParams(activity).toByteArray())
            os.flush()
            os.close()
            val code = urlConnection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val inputStream = urlConnection.inputStream
                val reader = InputStreamReader(inputStream, "UTF-8")
                val bufferedReader = BufferedReader(reader)
                val buffer = StringBuilder()
                var temp: String? = null
                while (bufferedReader.readLine().also { temp = it } != null) {
                    buffer.append(temp)
                }
                bufferedReader.close()
                reader.close()
                inputStream.close()
                val respontStr = buffer.toString()
                if (!TextUtils.isEmpty(respontStr)) {
                    val jsonObject = JSONObject(buffer.toString())
                    if (jsonObject.getInt("httpCode") == 200) {
                        PreferenceManager.getDefaultSharedPreferences(activity).edit()
                            .putBoolean("haveInstallAddOneTimes", true).apply()
                    }
                }
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Looper.loop()
    }

    private fun getParams(activity: Activity): String {
        var appInfo: ApplicationInfo? = null
        try {
            appInfo = activity.applicationContext.packageManager
                .getApplicationInfo(
                    activity.applicationContext.packageName,
                    PackageManager.GET_META_DATA
                )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        assert(appInfo != null)
        return appInfo!!.metaData.getString("YQCID")!!.trim { it <= ' ' }
    }

    private fun getURL(activity: Activity): String? {
        var appInfo: ApplicationInfo? = null
        try {
            appInfo = activity.applicationContext.packageManager
                .getApplicationInfo(
                    activity.applicationContext.packageName,
                    PackageManager.GET_META_DATA
                )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        assert(appInfo != null)
        return appInfo!!.metaData.getString("YQCU")
    }

}