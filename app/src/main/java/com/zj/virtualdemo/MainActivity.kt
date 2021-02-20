package com.zj.virtualdemo

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.UserHandle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.lody.virtual.client.core.VirtualCore
import com.lody.virtual.client.ipc.VActivityManager
import com.lody.virtual.client.ipc.VPackageManager
import com.lody.virtual.os.VUserManager
import com.lody.virtual.remote.InstalledAppInfo
import com.lody.virtual.server.pm.parser.VPackage
import com.zj.virtualdemo.adapters.CloneAppsAdapter
import com.zj.virtualdemo.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.flAdd.setOnClickListener {
            startActivity(Intent(this, ListAppActivity::class.java))
        }
        initData(binding)
    }

    private fun initData(binding: ActivityMainBinding) {
        val allApps = loadAllApps()
        val cloneAppsAdapter = CloneAppsAdapter(allApps) { it, v ->
            startAppShortcutOrInfoActivity(it, v)
        }
        binding.rvAllApp.adapter = cloneAppsAdapter
    }

    //region start App
    private fun startAppShortcutOrInfoActivity(
        launcherActivityInfo: LauncherActivityInfo,
        v: View
    ) {
        val icon = launcherActivityInfo.getIcon(DisplayMetrics.DENSITY_DEFAULT)
        var left = 0
        var top = 0
        var width = v.measuredWidth
        var height = v.measuredHeight
        if (icon != null) {
            val bounds = icon.bounds
            left = (width - bounds.width()) / 2
            top = v.paddingTop
            width = bounds.width()
            height = bounds.height()
        }

        val success: Boolean = startActivitySafely(launcherActivityInfo)
//        getUserEventDispatcher().logAppLaunch(
//            v,
//            intent,
//            item.user
//        ) // TODO for discovered apps b/35802115
//        if (success && v is BubbleTextView) {
//            mWaitingForResume = v as BubbleTextView
//            mWaitingForResume.setStayPressed(true)
//        }
    }

    fun startActivitySafely(launcherActivityInfo: LauncherActivityInfo): Boolean {
        val intent: Intent = makeLaunchIntent(launcherActivityInfo)
        val pkg = intent.`package`
        val userId =
            mirror.android.os.UserHandle.getIdentifier.call(launcherActivityInfo.user)

        try {
            // 如果已经在运行了，那么直接拉起，不做任何检测。
            var uiRunning = false
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val runningAppProcesses = am.runningAppProcesses


            for (runningAppProcess in runningAppProcesses) {
                val appProcessName =
                    VActivityManager.get().getAppProcessName(runningAppProcess.pid)
                if (TextUtils.equals(appProcessName, pkg)) {
                    uiRunning = true
                    break
                }
            }
            if (uiRunning) {
                launchActivity(intent, userId)
                return true
            }
        } catch (ignored: Throwable) {
            ignored.printStackTrace()
        }
        checkAndLaunch(intent, userId, launcherActivityInfo.label.toString())
        return false
    }

    private fun launchActivity(intent: Intent, userId: Int) {
        try {
            VActivityManager.get().startActivity(intent, userId)
        } catch (e: Throwable) {
            Toast.makeText(
                applicationContext,
                "打开应用失败",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun checkAndLaunch(intent: Intent, userId: Int, label: String) {
        val RUNTIME_PERMISSION_API_LEVEL = Build.VERSION_CODES.M
        if (Build.VERSION.SDK_INT < RUNTIME_PERMISSION_API_LEVEL) {
            // the device is below Android M, the permissions are granted when install, start directly
            launchActivityWithDelay(intent, userId)
            return
        }

        // The device is above android M, support runtime permission.
        val packageName: String = intent.`package`.toString()
        val name: String = label

        // analyze permission
        try {
            val applicationInfo = VPackageManager.get().getApplicationInfo(packageName, 0, 0)
            val targetSdkVersion = applicationInfo.targetSdkVersion

            if (targetSdkVersion >= RUNTIME_PERMISSION_API_LEVEL) {

                launchActivityWithDelay(intent, userId)
            } else {
                val packageInfo = VPackageManager.get()
                    .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS, 0)
                val requestedPermissions = packageInfo.requestedPermissions
                val dangerousPermissions: MutableSet<String> = HashSet()
                for (requestedPermission in requestedPermissions) {
                    if (VPackage.PermissionComponent.DANGEROUS_PERMISSION.contains(
                            requestedPermission
                        )
                    ) {
                        // dangerous permission, check it
                        if (ContextCompat.checkSelfPermission(
                                this,
                                requestedPermission
                            ) !== PackageManager.PERMISSION_GRANTED
                        ) {
                            dangerousPermissions.add(requestedPermission)
                        } else {

                        }
                    }
                }
                if (dangerousPermissions.isEmpty()) {

                    // all permission are granted, launch directly.
                    launchActivityWithDelay(intent, userId)
                } else {
                    // tell user that this app need that permission

                    val alertDialog: AlertDialog =
                        AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
                            .setTitle("重要提示:")
                            .setMessage("不支持动态申请权限, 您必须提前赋予它需要的所有必要权限, 请在它接下来的权限请求中全部选择允许，否则他可能无法正常运行。")
                            .setPositiveButton("我知道了") { dialog, which ->
                                val permissionToRequest =
                                    dangerousPermissions.toTypedArray()
                                try {
                                    ActivityCompat.requestPermissions(
                                        this,
                                        permissionToRequest,
                                        100
                                    )
                                } catch (ignored: Throwable) {
                                }
                            }
                            .create()
                    try {
                        alertDialog.show()
                    } catch (ignored: Throwable) {
                        // BadTokenException.
                        finish()
                        Toast.makeText(
                            this,
                            "失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: Throwable) {
            launchActivityWithDelay(intent, userId)
        }
    }

    private fun launchActivityWithDelay(intent: Intent, userId: Int) {
        val MAX_WAIT = 1000
//        val delta: Long = SystemClock.elapsedRealtime() - start
//        val waitTime = MAX_WAIT - delta
//        if (waitTime <= 0) {
        launchActivity(intent, userId)
//        } else {
//            loadingView.postDelayed({ launchActivity(intent, userId) }, waitTime)
    }
}

private fun makeLaunchIntent(info: LauncherActivityInfo): Intent {
    return makeLaunchIntent(info.componentName)
}

private fun makeLaunchIntent(cn: ComponentName?): Intent {
    return Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(cn)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
}

//endregion

//region load App
private fun loadAllApps(): ArrayList<LauncherActivityInfo> {
    val userManager = VUserManager.get()
    val mVirtualCore = VirtualCore.get()
    val vUserInfo = userManager.users[0]
//        val call = mirror.android.os.UserHandle.getIdentifier.call(vUserInfo)
    val result: ArrayList<LauncherActivityInfo> = ArrayList()

    val installedApps: List<InstalledAppInfo> =
        mVirtualCore.getInstalledAppsAsUser(vUserInfo.id, 0)
    Log.e("zhang", "" + installedApps.size)
    for (installedApp in installedApps) {
        val activityListForPackage = getActivityListForPackage(installedApp.packageName)!!
        result.addAll(activityListForPackage)
    }
    return result
}

private fun getActivityListForPackage(packageName: String): List<LauncherActivityInfo> {
    val result: MutableList<LauncherActivityInfo> = ArrayList()
    for (vUserInfo in VUserManager.get().users) {
        result.addAll(getActivityListForPackageAsUser(packageName, vUserInfo.id))
    }
    return result
}

private fun getActivityListForPackageAsUser(
    packageName: String,
    vuid: Int
): List<LauncherActivityInfo> {
    val result: MutableList<LauncherActivityInfo> = ArrayList()
    val mVirtualCore = VirtualCore.get()
    val context: Context = mVirtualCore.context
    val pm = VPackageManager.get()
    val intentToResolve = Intent(Intent.ACTION_MAIN)
    intentToResolve.addCategory(Intent.CATEGORY_INFO)
    intentToResolve.setPackage(packageName)
    var ris =
        pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, vuid)

    // Otherwise, try to find a main launcher activity
    if (ris == null || ris.size <= 0) {
        intentToResolve.removeCategory(Intent.CATEGORY_INFO)
        intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER)
        intentToResolve.setPackage(packageName)
        ris = pm.queryIntentActivities(
            intentToResolve,
            intentToResolve.resolveType(context),
            0,
            vuid
        )
    }
    if (ris == null || ris.size == 0) {
        return result
    }

    // remove the alias-activity
    val first = ris[0]
    val iterator = ris.iterator()
    while (iterator.hasNext()) {
        val next = iterator.next()
        if (next.activityInfo.targetActivity != null) {
            // alias, remove it
            iterator.remove()
            continue
        }
        if (!next.activityInfo.enabled) {
            // disabled component ,remove it
            iterator.remove()
        }
    }

    // if it is all alias, keep one.
    if (ris.size == 0) {
        ris.add(first)
    }
    for (resolveInfo in ris) {
        try {
            val userHandle: UserHandle = mirror.android.os.UserHandle.ctor.newInstance(vuid)
            makeLauncherActivityInfo(
                context,
                resolveInfo,
                userHandle
            )?.let {
                result.add(
                    it
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    return result
}

private fun makeLauncherActivityInfo(
    context: Context,
    resolveInfo: ResolveInfo,
    userHandle: UserHandle
): LauncherActivityInfo? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val constructor =
                LauncherActivityInfo::class.java.getDeclaredConstructor(
                    Context::class.java,
                    ActivityInfo::class.java,
                    UserHandle::class.java
                )
            constructor.isAccessible = true
            constructor.newInstance(context, resolveInfo.activityInfo, userHandle)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val constructor =
                LauncherActivityInfo::class.java.getDeclaredConstructor(
                    Context::class.java,
                    ResolveInfo::class.java,
                    UserHandle::class.java,
                    Long::class.javaPrimitiveType
                )
            constructor.isAccessible = true
            constructor.newInstance(
                context,
                resolveInfo,
                userHandle,
                System.currentTimeMillis()
            )
        } else {
            throw RuntimeException("can not construct launcher activity info")
        }
    } catch (e: Throwable) {
        throw RuntimeException(e)
    }
}
//endregion
