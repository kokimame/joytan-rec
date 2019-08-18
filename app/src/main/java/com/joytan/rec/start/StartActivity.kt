package com.joytan.rec.start

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.joytan.rec.R
import com.joytan.rec.main.MainActivity

import net.khirr.android.privacypolicy.PrivacyPolicyDialog

/**
 * 開始アクティビティ。多重起動を防ぐためにsingleInstanceになっている
 */
class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, PermissionActivity::class.java)
        val ppd = PrivacyPolicyDialog(this,
                "https://kokimame.github.io/joytan/tutorial.html",
                "https://kokimame.github.io/joytan/tutorial.html")



        ppd.onClickListener = (object : PrivacyPolicyDialog.OnClickListener {
            override fun onAccept(isFirstTime: Boolean) {
                startActivity(intent)
                finish()
            }

            override fun onCancel() {
                finish()
            }
        })
        ppd.addPoliceLine("This is crowdvoicing app for free education. Voice you send through this app will be used on Joytan App (YouTube channel)")
        ppd.addPoliceLine("Your recordings are licensed under Public Domain or Creative Commons 2.0.")
        ppd.addPoliceLine("We will never sell your recordings to others but we want to make them open to everyone, " +
                "including students, educators, and scientists for free.")

        ppd.acceptButtonColor = (ContextCompat.getColor(this, R.color.primary))
        ppd.linkTextColor = (ContextCompat.getColor(this, R.color.primary))
        ppd.show()
    }
}
