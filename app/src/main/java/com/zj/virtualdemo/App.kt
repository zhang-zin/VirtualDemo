package com.zj.virtualdemo

import android.app.Application
import android.content.Context
import android.os.Build
import com.lody.virtual.client.NativeEngine
import com.lody.virtual.client.core.VirtualCore
import com.lody.virtual.client.stub.VASettings

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NativeEngine.disableJit(Build.VERSION.SDK_INT)
        }
        VASettings.ENABLE_IO_REDIRECT = true
        VASettings.ENABLE_INNER_SHORTCUT = false
        try {
            VirtualCore.get().startup(base)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}