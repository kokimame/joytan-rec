package com.joytan.rec.main

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log

import com.joytan.util.BWU
import com.joytan.rec.R

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import android.net.Uri
import com.google.android.gms.appinvite.AppInviteInvitation
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.joytan.rec.setting.AboutActivity
import com.joytan.rec.setting.ProjectActivity
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import kotlinx.android.synthetic.main.content_main.*


/**
 * Main screen
 */
class MainActivity : AppCompatActivity() {

    companion object {
        val CODE_SETTING = 1
        const val RC_SIGN_IN = 123
        val DEBUG_TAG = "print_debug"
        /**
         * Show progress of data transaction with Firebase server
         */
        lateinit var pd: ProgressDialog
    }

    /**
     * Toolbar and NavDrawer configuration
     */
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

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
        // Title in toolbar is empty by default
        // and to be changed to project name in the MainFragment
        toolbar_main.title = ""
        setSupportActionBar(toolbar_main)
        // Configure ad
        // setupAd()
        // Volume adjusted to music

        // DO NOT ALLOW USERS TO TOUCH BUTTON BEFORE LOADING CONTENT
        pd = ProgressDialog(this, R.style.AlertDialog)
        // TODO Synchronize with the actual internet connection time
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        pd.setMessage("Loading data from server ...")
        pd.setCancelable(false)
        pd.show()

        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
                setOf(R.id.nav_main), drawer_layout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        nav_view.setupWithNavController(navController)
//        nav_view.itemIconTintList = null
        nav_view.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_signup -> {
                    // Choose authentication providers
                    val providers = arrayListOf(
                            AuthUI.IdpConfig.TwitterBuilder().build(),
                            AuthUI.IdpConfig.FacebookBuilder().build(),
                            AuthUI.IdpConfig.GoogleBuilder().build(),
                            AuthUI.IdpConfig.EmailBuilder().build()
//                            AuthUI.IdpConfig.GitHubBuilder().build()
                    )

                    // Create and launch sign-in intent
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setTheme(R.style.AppTheme_AppBarOverlay)
                                    .setAvailableProviders(providers)
                                    .setLogo(R.mipmap.ic_launcher_foreground)
                                    .build(), RC_SIGN_IN)
                }
                R.id.nav_slack -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("http://slack-invite.joytan.pub/")
                    startActivity(intent)
                }
                R.id.nav_pub -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://joytan.pub")
                    startActivity(intent)
                }
                R.id.nav_youtube -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://youtube.com/c/JoytanApp")
                    startActivity(intent)
                }
                R.id.nav_share -> {
                    val intent = AppInviteInvitation.IntentBuilder(getString(R.string.invite_title))
                            .setDeepLink(Uri.parse("https://cp999.app.goo.gl/cffF"))
                            .build();
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivityForResult(intent,1);
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    MainFragment.clientUid = MainFragment.defaultUid
                    this.recreate()
                }
                R.id.nav_about -> {
                    val intent = Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_project -> {
                    val intent = Intent(this, ProjectActivity::class.java)
                    startActivityForResult(intent, MainFragment.PROJECT_STARTUP_CODE)
                }
                else -> {
                    navController.navigate(it.itemId)
                }
            }
            drawer_layout.closeDrawers()
            true
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == resultCode || requestCode == RC_SIGN_IN) {
            Log.e(DEBUG_TAG, "<< onActivityResult >>")
            this.recreate()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}