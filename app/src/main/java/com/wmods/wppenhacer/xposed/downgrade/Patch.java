package com.wmods.wppenhacer.xposed.downgrade;

import android.os.Build;

import com.wmods.wppenhacer.xposed.core.FeatureLoader;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XSharedPreferences;

public class Patch {

    public static void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam, final XSharedPreferences prefs) {

        if (!lpparam.packageName.equals("android")) return;

        if (!prefs.getBoolean("downgrade", true)) return;

        XC_MethodHook hookDowngradeObject = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null); // bypass downgrade check
            }
        };

        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.S: // Android 12
            case Build.VERSION_CODES.S_V2: // Android 12L
            case Build.VERSION_CODES.TIRAMISU: // Android 13
                XposedHelpers.findAndHookMethod(
                    "com.android.server.pm.PackageManagerServiceUtils",
                    lpparam.classLoader,
                    "checkDowngrade",
                    "com.android.server.pm.parsing.pkg.ParsedPackage",
                    "android.content.pm.PackageInfoLite",
                    hookDowngradeObject
                );
                break;

            case Build.VERSION_CODES.UPSIDE_DOWN_CAKE: // Android 14
            case Build.VERSION_CODES.VANILLA_ICE_CREAM: // Android 15
                Method checkMethod = XposedHelpers.findMethodExactIfExists(
                    "com.android.server.pm.PackageManagerServiceUtils",
                    lpparam.classLoader,
                    "checkDowngrade",
                    "com.android.server.pm.pkg.AndroidPackage",
                    "android.content.pm.PackageInfoLite"
                );
                if (checkMethod != null) {
                    XposedBridge.hookMethod(checkMethod, hookDowngradeObject);
                } else {
                    XposedBridge.log("W/WppEnhancer: checkDowngrade not found on Android 14+");
                }
                break;
        }
    }
}
