package com.zj.virtualdemo

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.lody.virtual.GmsSupport
import com.lody.virtual.client.core.InstallStrategy
import com.lody.virtual.client.core.VirtualCore
import com.lody.virtual.helper.utils.DeviceUtil
import com.lody.virtual.remote.InstallResult
import com.zj.virtualdemo.adapters.AppListAdapter
import com.zj.virtualdemo.databinding.ActivityListAppBinding
import com.zj.virtualdemo.models.AppInfo
import com.zj.virtualdemo.models.AppInfoLite
import com.zj.virtualdemo.utils.HanziToPinyin

class ListAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityListAppBinding>(this, R.layout.activity_list_app)
        loadAppList(binding)
    }

    private fun loadAppList(binding: ActivityListAppBinding) {
        val localAppList = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        val hostPkg = VirtualCore.get().hostPkg
        val appList = localAppList.filter { packageInfo ->
            // ignore hostPkg package
            // ignore taichi package
            // ignore the System package
            hostPkg != packageInfo.packageName
                    && VirtualCore.TAICHI_PACKAGE != packageInfo.packageName
                    && !isSystemApplication(packageInfo)
        }.map { app ->
            appInfo(app)
        }.sortedBy {
            HanziToPinyin.getInstance().toPinyinString(it.name.toString().trim())
        }
        val appListAdapter = AppListAdapter(appList) { appInfo ->
            cloneApp(appInfo)
        }

        binding.rvAppList.adapter = appListAdapter
    }

    private fun cloneApp(info: AppInfo) {
        val appInfoLite = AppInfoLite(
            info.packageName,
            info.path,
            info.fastOpen,
            info.disableMultiVersion
        )

        try {
            val pkgInfo = packageManager.getPackageArchiveInfo(info.path, 0)
            pkgInfo?.apply {
                applicationInfo.sourceDir = info.path
                applicationInfo.publicSourceDir = info.path
            }

            val addVirtualApp = addVirtualApp(appInfoLite)
            if (addVirtualApp?.isSuccess == true) {
                Toast.makeText(this, "success", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
        }
    }

    fun addVirtualApp(info: AppInfoLite): InstallResult? {
        var flags = InstallStrategy.COMPARE_VERSION or InstallStrategy.SKIP_DEX_OPT
        info.fastOpen = false // disable fast open for compile.
        if (DeviceUtil.isMeizuBelowN()) {
            info.fastOpen = true
        }
        if (info.fastOpen) {
            flags = flags or InstallStrategy.DEPEND_SYSTEM_IF_EXIST
        }
        if (info.disableMultiVersion) {
            flags = flags or InstallStrategy.UPDATE_IF_EXIST
        }
        return VirtualCore.get().installPackage(info.path, flags)
    }

    private fun isSystemApplication(packageInfo: PackageInfo): Boolean {
        return (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                && !GmsSupport.isGmsFamilyPackage(packageInfo.packageName))
    }

    private fun appInfo(app: PackageInfo): AppInfo {
        val ai: ApplicationInfo = app.applicationInfo
        val path = if (ai.publicSourceDir != null) ai.publicSourceDir else ai.sourceDir
        val info = AppInfo()
        info.packageName = app.packageName
        info.fastOpen = true
        info.path = path
        info.icon = app.applicationInfo.loadIcon(packageManager)
        info.name = ai.loadLabel(packageManager)
        info.version = app.versionName
        val installedAppInfo = VirtualCore.get().getInstalledAppInfo(app.packageName, 0)
        if (installedAppInfo != null) {
            info.cloneCount = installedAppInfo.installedUsers.size
        }
        if (ai.metaData != null && ai.metaData.containsKey("xposedmodule")) {
            info.disableMultiVersion = true
            info.cloneCount = 0
        }
        return info
    }
}