package com.joytan.rec.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler
import com.joytan.rec.databinding.FragmentSignupBinding
import kotlinx.android.synthetic.main.activity_signup.*
import android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS
import androidx.core.content.ContextCompat.getSystemService



/**
 * Fragment activity to sign up or move to login
 * (Landing screen for authentication)
 */
class SignupFragment : Fragment() {

    private lateinit var mContext: Context
    private val mAuth = FirebaseAuth.getInstance()
    private val ah = AnalyticsHandler()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentSignupBinding>(
                inflater, R.layout.fragment_signup, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_to_login.setOnClickListener{
            //            val intent = Intent(this, LoginActivity::class.java)
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            startActivity(intent)
//            finish()
        }
        btn_do_signup.setOnClickListener{
            trySignUp()
        }
        //Analytics
        ah.onCreate(activity!!)
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    /**
     * Close soft keyboard on a pause
     */
    override fun onPause() {
        val inputManager = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // check if no view has focus:
        val currentFocusedView = activity!!.currentFocus
        if (currentFocusedView != null) {
            inputManager.hideSoftInputFromWindow(currentFocusedView!!.windowToken, HIDE_NOT_ALWAYS)
        }

        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        ah.onResume()
    }

    fun trySignUp() {
        val emailText = signup_email.text.toString()
        val pswText = signup_psw.text.toString()
        val confirmPswText = confirm_password.text.toString()
        val unameText = signup_uname.text.toString()

        if (!validateTextInput(emailText, pswText, confirmPswText, unameText)) {
            return
        }

        mAuth.createUserWithEmailAndPassword(emailText, pswText)
                .addOnCompleteListener(activity!!, OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser!!

                        Toast.makeText(mContext, "Successfully registered :)",
                                Toast.LENGTH_LONG).show()

                        val profUpdates = UserProfileChangeRequest.Builder().
                                setDisplayName(unameText).build()
                        user.updateProfile(profUpdates).addOnCompleteListener(OnCompleteListener { task ->
                            if (task.isSuccessful) {
                            }
                        })
                        activity?.finish()
                    } else {
                        Toast.makeText(mContext, "Error registering, try again later :(",
                                Toast.LENGTH_LONG).show()
                    }
                })
    }

    fun validateTextInput(emailText: String, pswText: String,
                          confirmPswText: String, unameText: String) : Boolean{
        if (emailText.isEmpty()) {
            Toast.makeText(mContext, "Please enter your email address.", Toast.LENGTH_LONG).show()
            return false
        } else if (pswText.isEmpty()) {
            Toast.makeText(mContext, "Please enter your password.", Toast.LENGTH_LONG).show()
            return false
        } else if (unameText.isEmpty()) {
            Toast.makeText(mContext, "Please enter your username.", Toast.LENGTH_LONG).show()
            return false
        } else if (confirmPswText.isEmpty()) {
            Toast.makeText(mContext, "Please confirm your password.", Toast.LENGTH_LONG).show()
            return false
        } else if (confirmPswText.isEmpty()) {
            Toast.makeText(mContext, "Please confirm your password.", Toast.LENGTH_LONG).show()
            return false
        }
        if (pswText.length < 8) {
            Toast.makeText(mContext, "Please make your password longer than 8 characters.",
                    Toast.LENGTH_LONG).show()
            return false
        }
        if (unameText.length < 3) {
            Toast.makeText(mContext, "Please make your username longer than 3 characters.",
                    Toast.LENGTH_LONG).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            Toast.makeText(mContext, "Please enter an valid email address.", Toast.LENGTH_LONG).show()
            return false
        }
        if (pswText != confirmPswText) {
            Toast.makeText(mContext, "Confirmation password does not match.", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

}