package com.example.weighttracker.health

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

fun Context.launchHealthConnectInstall() {
    val playIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
        setPackage("com.android.vending")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        startActivity(playIntent)
    } catch (notFound: ActivityNotFoundException) {
        val webIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(webIntent)
    }
}

fun Context.launchHealthConnectSettings() {
    runCatching {
        val intent = HealthConnectClient.getHealthConnectManageDataIntent(this)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }.getOrElse {
        launchHealthConnectInstall()
    }
}
