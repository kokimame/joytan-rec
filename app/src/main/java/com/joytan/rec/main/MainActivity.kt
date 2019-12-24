package com.joytan.rec.main

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.*

import com.joytan.util.BWU
import com.joytan.rec.R

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.lang.Exception
import kotlin.math.abs

/**
 * Main screen
 */
class MainActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    companion object {
        val CODE_SETTING = 1
        var defaultUid = ""
        var clientUid = ""
        val INFO_TAG = "kohki"
        /**
         * Show progress of data transaction with Firebase server
         */
        lateinit var pd: ProgressDialog
    }

    /**
     * Detect swipe gesture etc
     */
    private lateinit var mDetector: GestureDetectorCompat
    /**
     * Toolbar and NavDrawer configuration
     */
    private lateinit var appBarConfiguration: AppBarConfiguration

    // Internet connection receiver/monitor
    private val connFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).apply {
        addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
    }
    private val connReceiver = ConnectionReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerReceiver(connReceiver, connFilter)

        // Get permission first before making myDones into the user's storage
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        10)
            }
        } else {
            // Permission has already been granted
        }
        BWU.log("MainActivity#onCreate")
        mDetector = GestureDetectorCompat(this, this)
        // Title in toolbar is empty by default
        // and to be changed to project name in the MainFragment
        toolbar_main.title = ""
        setSupportActionBar(toolbar_main)
        // Configure ad
        // setupAd()
        // Volume adjusted to music

        // DO NOT ALLOW USERS TO TOUCH BUTTON BEFORE LOADING CONTENT
        pd = ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT)
        // TODO Synchronize with the actual internet connection time
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        pd.setMessage("Loading data from server ...")
        pd.setCancelable(false)
        pd.show()

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
                setOf(R.id.nav_main, R.id.nav_share), drawer_layout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        nav_view.setupWithNavController(navController)

    }
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (mDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onShowPress(p0: MotionEvent?) {
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onLongPress(p0: MotionEvent?) {
    }

    override fun onFling(e0: MotionEvent?, e1: MotionEvent?, vx: Float, vy: Float) : Boolean{
        return true
    }



}
