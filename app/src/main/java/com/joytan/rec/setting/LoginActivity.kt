package com.joytan.rec.setting

//import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler
import kotlinx.android.synthetic.main.activity_login.*

/**
 * 設定画面
 */
class LoginActivity : AppCompatActivity() {

    private val ah = AnalyticsHandler()
    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //ツールバーの設定
        setSupportActionBar(toolbar_login)
        setTheme(R.style.AuthStyle)

        val lsab = supportActionBar
        lsab?.setHomeButtonEnabled(true)
        lsab?.setDisplayHomeAsUpEnabled(true)

        //Analytics
        ah.onCreate(this)

        btn_to_signup.setOnClickListener{
            val intent = Intent(this, SignupActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
        btn_do_login.setOnClickListener{ view ->
            tryLogin(view)
        }
    }
    fun tryLogin(view: View) {
        val emailText = login_email.text.toString()
        val pswText = login_psw.text.toString()

        if (!validateTextInput(emailText, pswText)) {
            return
        }

        showSnackBarMessage(view, "Authenticating...")
        mAuth.signInWithEmailAndPassword(emailText, pswText)
                .addOnCompleteListener(this, OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser
                        val uid = user!!.uid
                        showSnackBarMessage(view, "Successfully logged in.")
                        finish()
                    } else {
                        showSnackBarMessage(view, "Log in failed.")
                    }
                })

    }

    fun validateTextInput(emailText: String, pswText: String) : Boolean{
        if (emailText.isEmpty()) {
            Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_LONG).show()
            return false
        } else if (pswText.isEmpty()) {
            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_LONG).show()
            return false
        }
        if (pswText.length < 8) {
            Toast.makeText(this, "Please make sure your password is longer than 8 characters.",
                    Toast.LENGTH_LONG).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            Toast.makeText(this, "Please enter an valid email address.", Toast.LENGTH_LONG).show()
            return false
        }

        return true
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
