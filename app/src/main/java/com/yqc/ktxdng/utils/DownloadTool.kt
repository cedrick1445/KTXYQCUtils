package com.yqc.ktxdng.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class DownloadTool {
    private var isFinishDownLoad = true

    fun download(activity: Activity, url: String) {
        ///复制到/data/data/package_name/files/目录下文件名
        val dir = activity.externalCacheDir!!.absolutePath + File.separator + "FileDownloader"
        val dirapk = dir + url.substring(url.lastIndexOf("/"))
        val apkfile = File(dirapk)
        if (apkfile.exists()) {
            install(activity, apkfile)
        } else {
            downloadapk(activity, url, dir)
        }
    }

    private fun downloadapk(activity: Activity, url: String, dir: String) {
        isFinishDownLoad = if (isFinishDownLoad) {
            false
        } else {
            return
        }

        // 1、创建Builder
        val builder: FileDownloadConfiguration.Builder = Builder(activity)

        // 2.配置Builder
        // 配置下载文件保存的文件夹
        builder.configFileDownloadDir(dir)
        // 配置同时下载任务数量，如果不配置默认为2
        builder.configDownloadTaskSize(3)
        // 配置失败时尝试重试的次数，如果不配置默认为0不尝试
        builder.configRetryDownloadTimes(5)
        // 开启调试模式，方便查看日志等调试相关，如果不配置默认不开启
        builder.configDebugMode(false)
        // 配置连接网络超时时间，如果不配置默认为15秒
        builder.configConnectTimeout(25000) // 25秒

        // 3、使用配置文件初始化FileDownloader
        val configuration: FileDownloadConfiguration = builder.build()
        FileDownloader.init(configuration)
        val downDialog = Dialog(activity, R.style.Theme_AppCompat_DayNight_DialogWhenLarge)
        val view = View.inflate(activity, R.layout.ui_yqc_download_progress1, null)
        //        View view = View.inflate(activity,R.layout.ui_yqc_download_progress,null);
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        downDialog.setCancelable(false) // 设置是否可以通过点击Back键取消
        downDialog.setCanceledOnTouchOutside(false) // 设置在点击Dialog外是否取消Dialog进度条
        downDialog.addContentView(view, params)
        downDialog.show()
        val pb = view.findViewById<View>(R.id.progress_bar) as ProgressBar
        val tvProgress = view.findViewById<View>(R.id.tv_progress) as TextView

        //设置对话框铺满屏幕
        val win = downDialog.window
        win!!.decorView.setPadding(0, 0, 0, 0)
        val lp = win.attributes
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        win.attributes = lp
        val mOnFileDownloadStatusListener: OnFileDownloadStatusListener =
            object : OnSimpleFileDownloadStatusListener() {
                fun onFileDownloadStatusRetrying(
                    downloadFileInfo: DownloadFileInfo?,
                    retryTimes: Int
                ) {
                    // 正在重试下载（如果你配置了重试次数，当一旦下载失败时会尝试重试下载），retryTimes是当前第几次重试
                }

                fun onFileDownloadStatusWaiting(downloadFileInfo: DownloadFileInfo?) {
                    // 等待下载（等待其它任务执行完成，或者FileDownloader在忙别的操作）
                }

                fun onFileDownloadStatusPreparing(downloadFileInfo: DownloadFileInfo?) {
                    // 准备中（即，正在连接资源）
                }

                fun onFileDownloadStatusPrepared(downloadFileInfo: DownloadFileInfo?) {
                    // 已准备好（即，已经连接到了资源）
                }

                fun onFileDownloadStatusDownloading(
                    downloadFileInfo: DownloadFileInfo,
                    downloadSpeed: Float,
                    remainingTime: Long
                ) {
                    // 正在下载，downloadSpeed为当前下载速度，单位KB/s，remainingTime为预估的剩余时间，单位秒
                    val totalSize = downloadFileInfo.getFileSizeLong() as Int
                    val downloaded = downloadFileInfo.getDownloadedSizeLong() as Int
                    val temp = downloaded.toFloat() / totalSize.toFloat()
                    val progress = (temp * 100).toInt()
                    pb.progress = progress
                    tvProgress.text = String.format("正在更新,请稍后... %s%%", progress)
                    //                downDialog.setProgress(progress);
                }

                fun onFileDownloadStatusPaused(downloadFileInfo: DownloadFileInfo?) {
                    // 下载已被暂停
                }

                fun onFileDownloadStatusCompleted(downloadFileInfo: DownloadFileInfo) {
                    // 下载完成（整个文件已经全部下载完成）
                    isFinishDownLoad = true
                    if (downDialog != null && downDialog.isShowing) {
                        downDialog.dismiss()
                        installApk(activity, downloadFileInfo.getFilePath())
                        //                    installAPK(activity, new File(downloadFileInfo.getFilePath()));
                    }
                }

                fun onFileDownloadStatusFailed(
                    url: String,
                    downloadFileInfo: DownloadFileInfo?,
                    failReason: FileDownloadStatusFailReason
                ) {
                    // 下载失败了，详细查看失败原因failReason，有些失败原因你可能必须关心
                    val failType: String = failReason.getType()
                    val failUrl: String =
                        failReason.getUrl() // 或：failUrl = url，url和failReason.getUrl()会是一样的
                    if (FileDownloadStatusFailReason.TYPE_URL_ILLEGAL.equals(failType)) {
                        // 下载failUrl时出现url错误
                    } else if (FileDownloadStatusFailReason.TYPE_STORAGE_SPACE_IS_FULL.equals(
                            failType
                        )
                    ) {
                        // 下载failUrl时出现本地存储空间不足
                    } else if (FileDownloadStatusFailReason.TYPE_NETWORK_DENIED.equals(failType)) {
                        // 下载failUrl时出现无法访问网络
                    } else if (FileDownloadStatusFailReason.TYPE_NETWORK_TIMEOUT.equals(failType)) {
                        // 下载failUrl时出现连接超时
                    } else {
                        // 更多错误....
                    }

                    // 查看详细异常信息
                    val failCause: Throwable =
                        failReason.getCause() // 或：failReason.getOriginalCause()

                    // 查看异常描述信息
                    val failMsg: String =
                        failReason.getMessage() // 或：failReason.getOriginalCause().getMessage()
                    if (failMsg.contains("Trust anchor for certification path not found")) {
//                    hygdload(checkData, canDismissDialog);
                        //https下载失败再重新下载，第二次就可以信任所有服务器成功
                        FileDownloader.start(url)
                    } else {
                        if (downDialog != null && downDialog.isShowing) {
                            downDialog.dismiss()
                        }
                        goToBrowser(activity, url, true)
                    }
                }
            }
        FileDownloader.registerDownloadStatusListener(mOnFileDownloadStatusListener)

        // 创建一个自定义保存路径和文件名称的下载
//        FileDownloader.detect(url, new OnDetectBigUrlFileListener() {
//            @Override
//            public void onDetectNewDownloadFile(String url, String fileName, String saveDir, long fileSize) {
//                // 如果有必要，可以改变文件名称fileName和下载保存的目录saveDir
//                FileDownloader.createAndStart(url, saveDir, fileName);
//            }
//
//            @Override
//            public void onDetectUrlFileExist(String url) {
//                // 继续下载，自动会断点续传（如果服务器无法支持断点续传将从头开始下载）
//                FileDownloader.start(url);
//            }
//
//            @Override
//            public void onDetectUrlFileFailed(String url, OnDetectBigUrlFileListener.DetectBigUrlFileFailReason failReason) {
//                // 探测一个网络文件失败了，具体查看failReason
//            }
//        });

//        OnDownloadFileChangeListener mOnDownloadFileChangeListener = new OnDownloadFileChangeListener() {
//            @Override
//            public void onDownloadFileCreated(DownloadFileInfo downloadFileInfo) {
//                // 一个新下载文件被创建，也许你需要同步你自己的数据存储，比如在你的业务数据库中增加一条记录
//            }
//            @Override
//            public void onDownloadFileUpdated(DownloadFileInfo downloadFileInfo, Type type) {
//                // 一个下载文件被更新，也许你需要同步你自己的数据存储，比如在你的业务数据库中更新一条记录
//            }
//            @Override
//            public void onDownloadFileDeleted(DownloadFileInfo downloadFileInfo) {
//                // 一个下载文件被删除，也许你需要同步你自己的数据存储，比如在你的业务数据库中删除一条记录
//            }
//        };
//        FileDownloader.registerDownloadFileChangeListener(mOnDownloadFileChangeListener);


        // 删除单个下载文件
        FileDownloader.delete(url, true, object : OnDeleteDownloadFileListener() {
            fun onDeleteDownloadFilePrepared(downloadFileInfo: DownloadFileInfo?) {}
            fun onDeleteDownloadFileSuccess(downloadFileInfo: DownloadFileInfo?) {
//                ToastUtil.showMessage("删除成功");
                FileDownloader.start(url)
            }

            fun onDeleteDownloadFileFailed(
                downloadFileInfo: DownloadFileInfo?,
                deleteDownloadFileFailReason: DeleteDownloadFileFailReason?
            ) {
//                ToastUtil.showMessage("删除失败" + deleteDownloadFileFailReason.getMessage());
                FileDownloader.start(url)
            }
        })
        // 第四步、下载文件和管理文件（FileDownloader API的简单使用）
        // 如果文件没被下载过，将创建并开启下载，否则继续下载，自动会断点续传（如果服务器无法支持断点续传将从头开始下载）
//        FileDownloader.start(url);
//        FileDownloader.reStart(url);
    }

    /**
     * 安装apk应用
     */
    private fun installAPK(activity: Activity, apkFile: File) {
        if (apkFile.isFile) {
//            String fileName = apkFile.getName();
//            String postfix = fileName.substring(fileName.length() - 4, fileName.length());
//            if (postfix.toLowerCase(CommonUtil.getLocale()).equals(".apk")) {
//
//            }
            try {
                val cmd = "chmod 755 " + apkFile.absolutePath
                try {
                    Runtime.getRuntime().exec(cmd)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val uri = Uri.fromFile(apkFile)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
                //判断是否是AndroidN以及更高的版本
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    val contentUri = FileProvider.getUriForFile(
                        activity,
                        activity.packageName + ".fileProvider",
                        apkFile
                    )
                    //                    Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", apkFile);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
                } else {
                    intent.setDataAndType(
                        Uri.fromFile(apkFile),
                        "application/vnd.android.package-archive"
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(activity, "安装文件出错！", Toast.LENGTH_LONG).show()
            }
        } else if (apkFile.isDirectory) {
            val files = apkFile.listFiles()
            val fileCount = files.size
            for (i in 0 until fileCount) {
                installAPK(activity, files[i])
            }
        }
    }

    private fun goToBrowser(activity: Activity, url: String, canDismissDialog: Boolean) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        activity.startActivity(intent)
        if (!canDismissDialog) {
            //强制更新前不能使用APP
//            ((Activity) mActivity).finish();
        }
    }

    /**
     * 安装apk
     * @param
     */
    fun installApk(context: Activity, saveFileName: String?) {
        val apkFile = File(saveFileName)
        if (!apkFile.exists()) {
            return
        }
        //有权限，开始安装应用程序
        install(context, apkFile)
    }

    @SuppressLint("ObsoleteSdkInt")
    fun install(context: Context, file: File?) {
        val apkUri: Uri
        apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context, context.packageName + ".fileProvider",
                file!!
            )
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        context.startActivity(intent)
    }

//    public static String getPackageName(Context context) {
//        //当前应用pid
//        int pid = android.os.Process.myPid();
//        //任务管理类
//        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        //遍历所有应用
//        List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
//        for (ActivityManager.RunningAppProcessInfo info : infos) {
//            if (info.pid == pid)//得到当前应用
//                return info.processName;//返回包名
//        }
//        return "";
//    }
}