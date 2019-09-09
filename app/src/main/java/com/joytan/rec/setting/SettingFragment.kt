package com.joytan.rec.setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.firebase.auth.FirebaseAuth

import com.joytan.rec.R


/**
 * 設定画面のフラグメント
 */
class SettingFragment : PreferenceFragment() {

    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.setting)
        Log.i("kohki", "Current User: " + mAuth.currentUser.toString())

        if (mAuth.currentUser == null) {
            run {
                val pref = findPreference(PREF_AUTH)
                pref.title = getString(R.string.title_auth)
                pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    callSignupActivity()
                    true
                }
            }
        } else {
            run {
                val pref = findPreference(PREF_AUTH)
                pref.title = getString(R.string.your_account)
                pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    callAccountActivity()
                    true
                }
            }
        }
        run {
            //Invitation
            val pref = findPreference(PREF_INVITE)
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                callInviteActivity()
                true
            }
        }
        run {
            //About this app
            val pref = findPreference(PREF_ABOUT)
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                callAboutActivity()
                true
            }
        }
        run {
            //Privacy Policy
            val pref = findPreference(PREF_PP)
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                callPP()
                true
            }
        }
        run {
            // Website link
            val pref = findPreference(PREF_WEB)
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                callWebsite()
                true
            }
        }
        run {
            val pref = findPreference(PREF_CRED)
            // Initialize summary text
            val manager = PreferenceManager.getDefaultSharedPreferences(pref.context)
            pref.summary = manager.getString(PREF_CRED, null)

            pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener {
                preference: Preference, value: Any ->
                pref.summary = value.toString()
                true
            }
        }
    }

    private fun callSignupActivity() {
        val intent = Intent(activity, SignupActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        this.activity.finish()
    }

    private fun callAccountActivity() {
        val intent = Intent(activity, AccountActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        this.activity.finish()
    }

    private fun callInviteActivity() {
        //ソースコード公開用に招待は無効
        val intent = AppInviteInvitation.IntentBuilder(getString(R.string.pref_invite))
                .setDeepLink(Uri.parse("https://cp999.app.goo.gl/cffF"))
                .build();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivityForResult(intent,1);
    }

    private fun callAboutActivity() {
        val intent = Intent(activity, AboutActivity::class.java)
        startActivity(intent)
    }

    private fun callOSS() {
        val intent = Intent(activity, HtmlViewerActivity::class.java)
        intent.putExtra(HtmlViewerActivity.EXTRA_URL, "file:///android_asset/license.txt")
        startActivity(intent)
    }

    private fun callPP() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://kokimame.github.io/joytan/privacy_policy_joytan_rec.html")
        startActivity(intent)
    }

    private fun callWebsite() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://joytan-rec-16ba2.web.app/")
        startActivity(intent)
    }

    companion object {
        private val CODE_SIGNUP = 2

        private val PREF_AUTH = "auth"

        private val PREF_SOUND_EFFECT = "soundEffect"

        private val PREF_ABOUT = "about"

        private val PREF_INVITE = "invite"

        private val PREF_OSS = "oss"

        private val PREF_PAGE = "page"

        private val PREF_PP = "pp"

        private val PREF_TW = "twitter"

        private val PREF_YT = "youtube"

        private val PREF_WEB = "website"

        private val PREF_CRED = "creditText"


        /**
         * 効果音設定を取得する
         * @param context
         * @return
         */
        fun isSoundEffect(context: Context): Boolean {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getBoolean(PREF_SOUND_EFFECT, true)
        }
        fun getCredit(context: Context): String? {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString(PREF_CRED, null)
        }
    }
}
