package com.zj.virtualdemo.delegate;

import android.app.Application;

import com.lody.virtual.client.core.VirtualCore;

/**
 * @author weishu
 * @date 2019/2/25.
 */
public class MyVirtualInitializer extends BaseVirtualInitializer {
    public MyVirtualInitializer(Application application, VirtualCore core) {
        super(application, core);
    }

    @Override
    public void onMainProcess() {

        super.onMainProcess();
    }

    @Override
    public void onVirtualProcess() {

        // For Crash statics

        super.onVirtualProcess();

        // Override
        virtualCore.setCrashHandler(new MyCrashHandler());
    }
}
