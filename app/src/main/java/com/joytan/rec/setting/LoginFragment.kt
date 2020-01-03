package com.joytan.rec.setting

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.joytan.rec.R
import com.joytan.rec.handler.AnalyticsHandler
import android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.joytan.rec.databinding.FragmentLoginBinding
import com.joytan.rec.main.MainActivity
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Fragment activity to sign up or move to login
 * (Landing screen for authentication)
 */
/**
 * FIXME
 * Make a base AuthFragment to
 */
class LoginFragment : Fragment() {

    private lateinit var mContext: Context
    private val mAuth = FirebaseAuth.getInstance()
    private val ah = AnalyticsHandler()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentLoginBinding>(
                inflater, R.layout.fragment_login, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_to_signup.setOnClickListener{
//            findNavController().navigate(R.id.action_nav_login_to_nav_signup)
            // Choose authentication providers
            val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.PhoneBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.FacebookBuilder().build(),
                    AuthUI.IdpConfig.TwitterBuilder().build())

// Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(), MainActivity.RC_SIGN_IN)
        }
        btn_do_login.setOnClickListener{ view ->
            tryLogin(view)
        }
        //Analytics
        ah.onCreate(activity!!)
        activity!!.drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    /**
     * Close soft keyboard on a pause
     */
    fun hideSoftKeyboard() {
        val inputManager = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // check if no view has focus:
        val currentFocusedView = activity!!.currentFocus
        if (currentFocusedView != null) {
            inputManager.hideSoftInputFromWindow(currentFocusedView!!.windowToken, HIDE_NOT_ALWAYS)
        }
    }
    override fun onPause() {
        hideSoftKeyboard()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        ah.onResume()
    }

    fun tryLogin(view: View) {
        hideSoftKeyboard()

        val emailText = login_email.text.toString()
        val pswText = login_psw.text.toString()

        if (!validateTextInput(emailText, pswText)) {
            return
        }

        showSnackBarMessage(view, "Authenticating...")
        mAuth.signInWithEmailAndPassword(emailText, pswText)
                .addOnCompleteListener(activity!!, OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser!!
                        Toast.makeText(mContext, "Successfully logged in :)",
                                Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    } else {
                        showSnackBarMessage(view, "Log in failed.")
                    }
                })
    }

    fun validateTextInput(emailText: String, pswText: String) : Boolean{
        if (emailText.isEmpty()) {
            Toast.makeText(mContext, "Please enter your email address.", Toast.LENGTH_LONG).show()
            return false
        } else if (pswText.isEmpty()) {
            Toast.makeText(mContext, "Please enter your password.", Toast.LENGTH_LONG).show()
            return false
        }
        if (pswText.length < 8) {
            Toast.makeText(mContext, "Please make sure your password is longer than 8 characters.",
                    Toast.LENGTH_LONG).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            Toast.makeText(mContext, "Please enter an valid email address.", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }
    fun showSnackBarMessage(view: View, message: String){
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).setAction("Action", null).show()
    }
}