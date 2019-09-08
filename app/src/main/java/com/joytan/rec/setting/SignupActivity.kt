package com.joytan.rec.setting

//import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler

import kotlinx.android.synthetic.main.activity_setting.*

/**
 * 設定画面
 */
class SignupActivity : AppCompatActivity() {

    private val ah = AnalyticsHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        //ツールバーの設定
        setSupportActionBar(toolbar)
//        setTheme(R.style.SettingsFragmentStyle)

        val lsab = supportActionBar
        lsab?.setHomeButtonEnabled(true)
        lsab?.setDisplayHomeAsUpEnabled(true)
        //フラグメントの追加
        //Analytics
        ah.onCreate(this)

        val loginBtn = findViewById<Button>(R.id.btn_to_login)
        loginBtn.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
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

    companion object {
        private val CODE_LOGIN = 3
    }
}
