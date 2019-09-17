package com.joytan.rec.start

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
//import android.support.v4.app.ActivityCompat
//import android.support.v4.content.ContextCompat
//import android.support.v7.app.AlertDialog
//import android.support.v7.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.joytan.rec.R
import com.joytan.rec.main.ConnectionReceiver
import com.joytan.rec.main.MainActivity
import kotlinx.android.synthetic.main.activity_permission.*

/**
 * 実行時パーミッションActivity
 */
class PermissionActivity : AppCompatActivity() {
    private val connFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).apply {
        addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
    }
    private val connReceiver = ConnectionReceiver()

    private val CODE_PERMISSION = 1
    private val CODE_MAIN = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(connReceiver, connFilter)

        setContentView(R.layout.activity_permission)
        //ツールバー設定
        this.setSupportActionBar(toolbar)
        if (savedInstanceState == null) {
            //起動時のみ
            checkPermission()
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CODE_MAIN) {
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (CODE_PERMISSION == requestCode) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                callMainActivity()
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                finish()
            }
            return
        }
    }

    /**
     * 実行時パーミッションをチェックする
     */
    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                //拒否した場合は説明を表示
                showInformation(R.string.permission_audio)
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            }
            else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)){
                showInformation(R.string.permission_write)
            }
            else {
                //許可を求める
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        CODE_PERMISSION)
            }
            Log.i(MainActivity.INFO_TAG, "audio not granted")
        }
//        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            // Should we show an explanation?
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                //拒否した場合は説明を表示
//                showInformation(R.string.permission_write)
//                // Show an expanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//            } else {
//                //許可を求める
//                ActivityCompat.requestPermissions(this,
//                        arrayOf(Manifest.permission.RECORD_AUDIO,
//                                Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                        CODE_PERMISSION)
//            }
//            Log.i(MainActivity.INFO_TAG, "write not granted")
//        }
        else {
            //すでに許可されている
            callMainActivity()
            finish()
        }
    }

    /**
     * メイン画面を呼び出す
     */
    private fun callMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivityForResult(intent, CODE_MAIN)
    }

    /**
     * 実行時パーミッションの解説
     */
    private fun showInformation(message_id : Int) {
        val adb = AlertDialog.Builder(this, R.style.AlertDialog)
        adb.setTitle(R.string.title_permission)
        adb.setMessage(message_id)
        adb.setCancelable(false)
        adb.setPositiveButton(R.string.ok) { dialog, which ->
            callApplicationDetailActivity()
            finish()
        }.setNegativeButton(R.string.cancel) { dialog, which -> finish() }
        val dialog = adb.show()
    }

    /**
     * アプリ詳細を呼び出す
     */
    private fun callApplicationDetailActivity() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
