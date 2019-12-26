package com.joytan.rec.setting

//import android.databinding.ObservableField
import android.content.Context
import android.content.pm.PackageManager
import androidx.databinding.ObservableField
import com.joytan.rec.R

/**
 * ViewModel for "About" screen
 */
class AboutViewModel(private val context : Context){
    val versionText = ObservableField<String>()

    fun onCreate(){
        // Set version text
        try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageInfo(context.packageName, 0)
            versionText.set("${context.getString(R.string.version)} ${packageInfo.versionName}")
        } catch (e: PackageManager.NameNotFoundException) {
            // Nothing happens
        }

    }

}