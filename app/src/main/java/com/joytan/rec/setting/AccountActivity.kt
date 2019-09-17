package com.joytan.rec.setting

//import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler
import com.joytan.rec.main.MainActivity
import kotlinx.android.synthetic.main.activity_account.*

import java.lang.Exception

/**
 * 設定画面
 */
class AccountActivity : AppCompatActivity() {

    private val ah = AnalyticsHandler()
    private val mAuth = FirebaseAuth.getInstance()
    private val fDatabaseRef = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        //ツールバーの設定
        setSupportActionBar(toolbar_account)
        setTheme(R.style.AuthStyle)

        val lsab = supportActionBar
        lsab?.setHomeButtonEnabled(true)
        lsab?.setDisplayHomeAsUpEnabled(true)

        //Analytics
        ah.onCreate(this)

        btn_do_signout.setOnClickListener{ view ->
            trySignOut(view)
            finish()
        }
        uname_text.text = mAuth.currentUser!!.displayName
        Log.i(MainActivity.INFO_TAG, "Show account info of " + mAuth.currentUser!!.displayName)

        try {
            val ref = fDatabaseRef.child("users").
                    child(mAuth.uid!!).orderByChild("votes")
            ref.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.value is Map<*, *>) {
                        val contribs = p0.value as Map<String, Map<String, String>>
                        val audioCount = contribs.get("audio")
                        val voteCount = contribs.get("votes")

                        if (audioCount == null) {
                            upload_count.text = "0"
                        } else {
                            upload_count.text = audioCount.keys.size.toString()
                        }

                        if (voteCount == null) {
                            vote_count.text = "0"
                        } else {
                            vote_count.text = voteCount.keys.size.toString()
                        }
                    } else {
                        // If user does not have any record in the database
                        upload_count.text = "0"
                        vote_count.text = "0"
                    }
                }
            })
        } catch (e: Exception) {
            Log.i(MainActivity.INFO_TAG, "Exception: " + e.toString())
        }

    }
    fun trySignOut(view: View) {
        showSnackBarMessage(view, "Signing Out...")
        mAuth.signOut()
        // Use defaultUid when the user not signed in
        MainActivity.clientUid = MainActivity.defaultUid

        Toast.makeText(this, "Successfully signed out", Toast.LENGTH_LONG).show()
    }

    fun showSnackBarMessage(view: View, message: String){
        Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).setAction("Action", null).show()
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
