package com.yqc.ktxdng.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.view.KeyEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.webkit.WebView.HitTestResult
import android.webkit.WebView.WebViewTransport
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.werb.permissionschecker.PermissionChecker
import com.yqc.ktxdng.R
import org.jetbrains.annotations.Nullable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date


class WebViewActivity : AppCompatActivity() {
    val URL = "url"
    val WEB_TITLE = "webTitle"

    var mPbLoading: ProgressBar? = null
    var mWvContent: WebView? = null

    //    private GestureDetector gestureDetector;
    //    private int downX, downY;
    private var imgurl: String? = null

    val PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private var permissionChecker: PermissionChecker? = null

    fun launch(from: Activity, url: String?) {
        val intent = Intent(from, WebViewActivity::class.java)
        intent.putExtra(URL, url)
        from.startActivity(intent)
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mPbLoading = findViewById<View>(R.id.pb_loading) as ProgressBar
        mWvContent = findViewById<View>(R.id.wv_content) as WebView
        permissionChecker = PermissionChecker(this)
        //        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
//            @Override
//            public void onLongPress(MotionEvent e) {
//                downX = (int) e.getX();
//                downY = (int) e.getY();
//            }
//        });
        initData()
        initListener()
    }

    private fun showToast(str: String) {
        Toast.makeText(this@WebViewActivity, str, Toast.LENGTH_LONG).show()
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initData() {
        mWvContent!!.settings.javaScriptEnabled = true //允许执行javascript语句
        mWvContent!!.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        mWvContent!!.settings.defaultTextEncodingName = "UTF-8"
        mWvContent!!.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        mWvContent!!.settings.setSupportMultipleWindows(true) // 多窗口
        mWvContent!!.settings.useWideViewPort = true // 设置此属性，可任意比例缩放
        mWvContent!!.settings.pluginState = WebSettings.PluginState.ON
        mWvContent!!.settings.domStorageEnabled = true
        //支持屏幕缩放
        mWvContent!!.settings.setSupportZoom(false)
        mWvContent!!.settings.builtInZoomControls = false
        //自适应屏幕
        mWvContent!!.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        //        mWvContent.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);//自适应屏幕
        mWvContent!!.settings.loadWithOverviewMode = true
        //        mWvContent.getSettings().setLoadsImagesAutomatically(true);
        val webseting = mWvContent!!.settings
        webseting.domStorageEnabled = true
        webseting.setAppCacheMaxSize((1024 * 1024 * 8).toLong()) //设置缓冲大小，我设的是8M
        val appCacheDir = this.applicationContext.getDir("cache", MODE_PRIVATE).path
        webseting.setAppCachePath(appCacheDir)
        webseting.allowFileAccess = true
        webseting.setAppCacheEnabled(true)
        webseting.cacheMode = WebSettings.LOAD_DEFAULT
        //解决Android5.0后版本的web图片显示不了的问题
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
//            mWvContent.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
//        }
        mWvContent!!.settings.blockNetworkImage = true

        // 支持获取手势焦点
        mWvContent!!.requestFocusFromTouch()

        // 先不要自动加载图片，等页面finish后再发起图片加载
        mWvContent!!.settings.loadsImagesAutomatically = true
        mWvContent!!.setDownloadListener(MyWebViewDownLoadListener())
        mWvContent!!.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                mPbLoading!!.progress = newProgress
                if (newProgress == 100) {
                    mWvContent!!.settings.blockNetworkImage = false
                }
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                val newWebView = WebView(this@WebViewActivity)
                //                view.addView(newWebView);
                val transport = resultMsg.obj as WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        mWvContent!!.loadUrl(url)
                        return true
                    }
                }
                return true
            }
        }
        val url = intent.getStringExtra(URL)
        mWvContent!!.loadUrl(url!!)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initListener() {
        val settings = mWvContent!!.settings
        settings.javaScriptEnabled = true
        mWvContent!!.setOnLongClickListener(OnLongClickListener { v ->
            val result = (v as WebView).hitTestResult
            val type = result.type
            if (type == HitTestResult.UNKNOWN_TYPE) return@OnLongClickListener false
            if (type == HitTestResult.EDIT_TEXT_TYPE) {
                //let TextViewhandles context menu return true;
            }
            when (type) {
                HitTestResult.PHONE_TYPE -> {}
                HitTestResult.EMAIL_TYPE -> {}
                HitTestResult.GEO_TYPE -> {}
                HitTestResult.SRC_ANCHOR_TYPE -> {}
                HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {}
                HitTestResult.IMAGE_TYPE -> {
                    imgurl = result.extra
                    //通过GestureDetector获取按下的位置，来定位PopWindow显示的位置
                    dialogList()
                }
                else -> {}
            }
            true
        })
        mWvContent!!.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
                mPbLoading!!.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                mPbLoading!!.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return if (url.startsWith("http") || url.startsWith("https")) { //http和https协议开头的执行正常的流程
                    super.shouldOverrideUrlLoading(view, url)
                } else { //其他的URL则会开启一个Acitity然后去调用原生APP
                    try {
                        val `in` = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(`in`)
                    } catch (e: Exception) {
                        showToast("跳转失败！")
                    }
                    true
                }
            }
        }
        mWvContent!!.setOnKeyListener(View.OnKeyListener { view, i, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (i == KeyEvent.KEYCODE_BACK && mWvContent!!.canGoBack()) {  //表示按返回键
                    mWvContent!!.goBack() //后退
                    return@OnKeyListener true //已处理
                }
            }
            false
        })
    }

    private class MyWebViewDownLoadListener : DownloadListener {
        override fun onDownloadStart(
            url: String, userAgent: String,
            contentDisposition: String, mimetype: String, contentLength: Long
        ) {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

    }

    private fun dialogList() {
        val items = arrayOf("保存图片", "取消")
        val builder = AlertDialog.Builder(this, 3)
        //        builder.setIcon(R.mipmap.ic_launcher);
        // 设置列表显示，注意设置了列表显示就不要设置builder.setMessage()了，否则列表不起作用。
        builder.setItems(items) { dialog, which ->
            dialog.dismiss()
            if ("保存图片" == items[which]) {
                if (permissionChecker!!.isLackPermissions(PERMISSIONS)) {
                    permissionChecker!!.requestPermissions()
                } else {
                    //检查是否有权限未被获取，有则引导去请求获取，没有则进行我们获取到相应权限后的业务逻辑
                    SaveImage().execute() // Android 4.0以后要使用线程来访问网络
                }
            }
        }
        builder.create().show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionChecker.PERMISSION_REQUEST_CODE -> if (permissionChecker!!.hasAllPermissionsGranted(
                    grantResults
                )
            ) {
                //此处是我们获取权限后的业务逻辑
                SaveImage().execute()
            } else {
                permissionChecker!!.showDialog()
            }
        }
    }

    private class SaveImage : AsyncTask<String?, Void?, String>() {
        protected override fun doInBackground(vararg params: String): String {
            var result = ""
            try {
                val sdcard = Environment.getExternalStorageDirectory().toString()
                var file = File("$sdcard/Download")
                if (!file.exists()) {
                    file.mkdirs()
                }
                val idx: Int = imgurl.lastIndexOf(".")
                val ext: String = imgurl.substring(idx)
                file = File(sdcard + "/Download/" + Date().time + ext)
                var inputStream: InputStream? = null
                val url = URL(imgurl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 20000
                if (conn.responseCode == 200) {
                    inputStream = conn.inputStream
                }
                val buffer = ByteArray(4096)
                var len = 0
                val outStream = FileOutputStream(file)
                while (inputStream!!.read(buffer).also { len = it } != -1) {
                    outStream.write(buffer, 0, len)
                }
                outStream.close()
                //                result = "图片已保存至：" + file.getAbsolutePath();
                result = "保存成功"
            } catch (e: Exception) {
                result = "保存失败！" + e.localizedMessage
            }
            return result
        }

        override fun onPostExecute(result: String) {
            showToast(result)
        }
    }

    private var exitTime: Long = 0
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            if (mWvContent!!.canGoBack()) {
                mWvContent!!.goBack()
            } else {
                if (System.currentTimeMillis() - exitTime > 2000) //System.currentTimeMillis()无论何时调用，肯定大于2000
                {
                    Toast.makeText(applicationContext, "再按一次退出程序", Toast.LENGTH_SHORT).show()
                    exitTime = System.currentTimeMillis()
                } else {
                    finish()
                    System.exit(0)
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}