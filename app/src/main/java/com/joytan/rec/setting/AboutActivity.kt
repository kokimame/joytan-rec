package com.joytan.rec.setting

//import android.databinding.DataBindingUtil
//import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil

import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler
import com.joytan.rec.databinding.ActivityAboutBinding

import kotlinx.android.synthetic.main.activity_about.*

/**
 * About the application, details, goals, version, etc
 */
class AboutActivity : AppCompatActivity() {

    private val ah = AnalyticsHandler()

    /**
     * ビューモデル
     */
    private val viewModel = AboutViewModel(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //データバインディング設定
        val binding = DataBindingUtil.setContentView<ActivityAboutBinding>(this, R.layout.activity_about)
        binding.viewModel = viewModel
        setSupportActionBar(toolbar)
        val lsab = supportActionBar
        lsab?.setHomeButtonEnabled(true)
        lsab?.setDisplayHomeAsUpEnabled(true)
        //Analytics
        ah.onCreate(this)
        //
        viewModel.onCreate()
    }

    override fun onResume() {
        super.onResume()
        ah.onResume()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return false
    }
}
