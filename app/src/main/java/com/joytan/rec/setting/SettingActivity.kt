package com.joytan.rec.setting

//import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler

import kotlinx.android.synthetic.main.activity_setting.*

/**
 * 設定画面
 */
class SettingActivity : AppCompatActivity() {

    private val ah = AnalyticsHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        //ツールバーの設定
        setSupportActionBar(toolbar)
        setTheme(R.style.SettingsFragmentStyle)

        val lsab = supportActionBar
        lsab?.setHomeButtonEnabled(true)
        lsab?.setDisplayHomeAsUpEnabled(true)
        //フラグメントの追加
        val ft = fragmentManager.beginTransaction()
        ft.replace(R.id.preference_frame, SettingFragment())

        ft.commit()
        val youtubeBtn = findViewById<ImageButton>(R.id.yt_link)
        youtubeBtn.setOnClickListener{
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.youtube.com/c/JoytanApp")
            startActivity(intent)
        }
        val twitterBtn = findViewById<ImageButton>(R.id.tw_link)
        twitterBtn.setOnClickListener{
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://twitter.com/JoytanApp")
            startActivity(intent)
        }

        //Analytics
        ah.onCreate(this)
    }

    override fun onResume() {
        super.onResume()
        ah.onResume()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return false
    }
}
