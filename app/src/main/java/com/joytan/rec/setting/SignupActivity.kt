package com.joytan.rec.setting

//import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth

import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler

import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.activity_signup.*

/**
 * 設定画面
 */
class SignupActivity : AppCompatActivity() {

    private val ah = AnalyticsHandler()
    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        //ツールバーの設定
        setSupportActionBar(toolbar)
        setTheme(R.style.AuthStyle)

        val lsab = supportActionBar
        lsab?.setHomeButtonEnabled(true)
        lsab?.setDisplayHomeAsUpEnabled(true)
        //フラグメントの追加
        //Analytics
        ah.onCreate(this)

        val toLoginBtn = findViewById<Button>(R.id.btn_to_login)
        toLoginBtn.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        val doSignupBtn = findViewById<Button>(R.id.btn_do_signup)
        doSignupBtn.setOnClickListener{
            trySigningUp()
        }
    }

    fun trySigningUp() {
        Log.i("kohki", "Do registering")
        Log.i("kohki", mAuth.currentUser.toString())

        val emailText = findViewById<EditText>(R.id.signup_email).text.toString()
        val pswText = findViewById<EditText>(R.id.signup_psw).text.toString()
        val confirmPswText = findViewById<EditText>(R.id.confirm_password).text.toString()
        val unameText = findViewById<EditText>(R.id.signup_uname).text.toString()

        if (!validateTextInput(emailText, pswText, confirmPswText)) {
            return
        }


        mAuth.createUserWithEmailAndPassword(emailText, pswText)
                .addOnCompleteListener(this, OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser
                        val uid = user!!.uid
                        Toast.makeText(this, "Successfully registered :)",
                                Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error registering, try again later :(",
                                Toast.LENGTH_LONG).show()
                    }
                })
    }

    fun validateTextInput(emailText: String, pswText: String, confirmPswText: String) : Boolean{
        if (emailText.isEmpty()) {
            Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_LONG).show()
            return false
        } else if (pswText.isEmpty()) {
            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_LONG).show()
            return false
        } else if (confirmPswText.isEmpty()) {
            Toast.makeText(this, "Please confirm your password.", Toast.LENGTH_LONG).show()
            return false
        } else if (confirmPswText.isEmpty()) {
            Toast.makeText(this, "Please confirm your password.", Toast.LENGTH_LONG).show()
            return false
        }
        if (pswText.length < 8) {
            Toast.makeText(this, "Please make your password longer than 8 characters.",
                    Toast.LENGTH_LONG).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            Toast.makeText(this, "Please enter an valid email address.", Toast.LENGTH_LONG).show()
            return false
        }


        if (pswText != confirmPswText) {
            Toast.makeText(this, "Confirmation password does not match.", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }


    override fun onStart() {
        super.onStart()
        Log.i("kohki", mAuth.currentUser.toString())
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
