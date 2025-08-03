
package com.wmods.wppenhacer.xposed.downgrade;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;

import android.os.Build;

import com.wmods.wppenhacer.xposed.core.FeatureLoader;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Patch implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("android")) return;

        if (!FeatureLoader.getBoolean("downgrade", true)) return;

        Object hookDowngradeObject = new de.robv.android.xposed.XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null); // bypass downgrade check
            }
        };

        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.S: // Android 12
            case Build.VERSION_CODES.S_V2: // Android 12L
            case Build.VERSION_CODES.TIRAMISU: // Android 13
                hookMethod(
                    findMethodExactIfExists(
                        "com.android.server.pm.PackageManagerServiceUtils",
                        lpparam.classLoader,
                        "checkDowngrade",
                        "com.android.server.pm.parsing.pkg.ParsedPackage",
                        "android.content.pm.PackageInfoLite"
                    ),
                    hookDowngradeObject
                );
                break;

            case Build.VERSION_CODES.UPSIDE_DOWN_CAKE: // Android 14
            case Build.VERSION_CODES.VANILLA_ICE_CREAM: // Android 15
                Method checkMethod = findMethodExactIfExists(
                    "com.android.server.pm.PackageManagerServiceUtils",
                    lpparam.classLoader,
                    "checkDowngrade",
                    "com.android.server.pm.pkg.AndroidPackage",
                    "android.content.pm.PackageInfoLite"
                );
                if (checkMethod != null) {
                    hookMethod(checkMethod, hookDowngradeObject);
                } else {
                    de.robv.android.xposed.XposedBridge.log("W/WppEnhancer: checkDowngrade not found on Android 14+");
                }
                break;
        }
    }
}
