package com.jojomango.expensetracker.ui.notification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private const val PREFS_NAME = "notification_permission"
private const val KEY_AUTO_REQUESTED = "auto_requested"

fun areNotificationsEnabled(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun needsRuntimePermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/** 通知是否開啟；每次畫面回到前景重新檢查一次（使用者可能剛從系統設定改過）。 */
@Composable
fun rememberNotificationsEnabled(): Boolean {
    val context = LocalContext.current

    // 新版的 LocalLifecycleOwner 在 lifecycle-runtime-compose，那個套件目前只是間接依賴；
    // 為了一個功能相同的 API 去正式加依賴，要先經過人類批准（CLAUDE.md 禁令 3），不值得。
    @Suppress("DEPRECATION")
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(areNotificationsEnabled(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) enabled = areNotificationsEnabled(context)
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled
}

/**
 * 回傳一個「幫使用者打開通知」的動作：還沒問過系統權限就跳權限對話框；已經被拒絕過
 * （Android 11+ 被拒兩次後系統不會再跳對話框）或是在較舊的 Android 上，就直接帶去這個
 * app 的系統通知設定頁，讓使用者自己打開。
 */
@Composable
fun rememberEnableNotifications(): () -> Unit {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) openAppNotificationSettings(context)
        }
    return {
        if (needsRuntimePermission(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openAppNotificationSettings(context)
        }
    }
}

/**
 * 備份提醒要靠系統通知才發得出去（SPEC.md §3.6），但一開 app 就跳權限對話框很打擾人，
 * 使用者也還搞不清楚為什麼要通知。所以等使用者**已經有一個錢包**（有資料值得備份）
 * 之後才問，而且只自動問這一次；之後想開，走設定頁的「開啟通知」。
 */
@Composable
fun AutoRequestNotificationPermissionOnce() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_AUTO_REQUESTED, false) && needsRuntimePermission(context)) {
            prefs.edit { putBoolean(KEY_AUTO_REQUESTED, true) }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
