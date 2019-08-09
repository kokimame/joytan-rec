package jp.bellware.echo.main

import android.Manifest
//import android.support.v7.app.AppCompatActivity
//import android.databinding.DataBindingUtil
import android.media.AudioManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView

import jp.bellware.echo.R

import jp.bellware.echo.setting.SettingActivity
import jp.bellware.util.BWU
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_control.*
import kotlinx.android.synthetic.main.main_status.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.firebase.storage.FirebaseStorage
import jp.bellware.echo.databinding.ActivityMainBinding
import java.io.File
import kotlin.math.max
import kotlin.math.min

import org.json.JSONObject


/**
 * メイン画面
 */
class MainActivity : AppCompatActivity() {
    /**
     * 警告担当
     */
    private val wh = WarningHandler()

    /**
     * ボリュームの設定担当
     */
    private lateinit var audioManager: AudioManager


    /**
     * アニメ担当
     */
    private val animator = QRecAnimator()

    private val mainScripts = mutableListOf<String>()

    private val fStorage = FirebaseStorage.getInstance()
    // Create a storage reference from our app
    private val fStorageRef = fStorage.reference
    private val projectsRef = fStorageRef.child("test_projects.json")
    private val tempFile = File.createTempFile("projects", "json")

    /**
     * メイン画面のビューモデル
     */
    private val viewModel = MainViewModel(this, object : MainViewModel.Listener {
        override fun onDeleteRecord() {
            animator.startDeleteAnimation(statusFrame)
            animator.startDeleteAnimation(delete)
            animator.startDeleteAnimation(replay)
            animator.startDeleteAnimation(share)
        }

        override fun onStartRecord() {
            //爆発エフェクト
            explosion.startRecordAnimation()
            //ステータスはフェードイン
            animator.fadeIn(statusFrame)
            //削除ボタンはフェードイン
            if (delete.visibility == View.INVISIBLE) {
                animator.fadeIn(delete)
            }
        }

        override fun onStopRecord() {
            //ステータスを表示
            animator.fadeIn(statusFrame)
            //サブコントロール表示
            animator.fadeIn(replay)
            animator.fadeIn(share)
        }

        override fun onScriptNavigation(direction: String) {
            val mainText = findViewById<TextView>(R.id.main_text)
            var index = mainScripts.indexOf(mainText.text)

            if (direction == "left") {
                index = max(0, index - 1)
            }
            else if (direction == "right") {
                index = min(mainScripts.size - 1, index + 1)
            }
            mainText.setText(mainScripts.get(index))

        }

        override fun onUpdateVolume(volume: Float) {
            //視覚的ボリュームを更新する
            visualVolume.setVolume(volume)
        }

        override fun onShowWarningMessage(resId: Int) {
            //警告を表示する
            wh.show(resId)
        }

    });


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BWU.log("MainActivity#onCreate")
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.viewModel = viewModel
        //ツールバー設定
        this.setSupportActionBar(toolbar)
        //広告の設定
//        setupAd()
        //ボリューム調整を音楽にする
        volumeControlStream = AudioManager.STREAM_MUSIC
        //AudioManagerを取得
        this.audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        //警告担当
        wh.onCreate(this)
        viewModel.onCreate()

        projectsRef.getFile(tempFile).addOnSuccessListener {
            val jsonString: String = tempFile.readText(Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)
            val projectsJson = jsonObject.getJSONArray("projects")
            val projectsList = mutableListOf<String>()

            for (i in 0 until projectsJson.length()) {
                projectsList.add(projectsJson.getJSONObject(i).getString("title"))
            }

            val entriesJson = projectsJson.getJSONObject(0).getJSONArray("entries")
            for (i in 0 until entriesJson.length()) {
                mainScripts.add(entriesJson.getJSONObject(i).getString("atop"))
            }

            setupSpinner(projectsList)
            setupMainScript()

        }.addOnFailureListener {
            val projectsList = listOf("Failed", "to", "load JSON", "from Firebase")
            setupSpinner(projectsList)
        }

        // Here, thisActivity is the current activity
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

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }
    }

    /**
     * Setup the main script
     */
    private fun setupMainScript() {
        val mainText: TextView = findViewById(R.id.main_text)
        mainText.text = mainScripts[0]
    }


    /*
     * Setup the project spinner
     */
    private fun setupSpinner(projectsList: List<String>) {
        val spinner: Spinner = findViewById(R.id.project_spinner)

        ArrayAdapter(
                this,
                R.layout.spinner_item,
                projectsList
        ).also {
            adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(R.layout.spinner_item)
            // Apply the adapter to the spinner
            spinner.setAdapter(adapter)
        }

    }

    override fun onStart() {
        super.onStart()
        BWU.log("MainActivity#onStart")
        viewModel.onStart()
    }

    override fun onResume() {
        super.onResume()
        BWU.log("MainActivity#onResume")
        wh.onResume()
        if (this.audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            wh.show(R.string.warning_volume)
        }
    }

    override fun onPause() {
        super.onPause()
        BWU.log("MainActivity#onPause")
        wh.onPause()
    }

    override fun onStop() {
        super.onStop()
        BWU.log("MainActivity#onStop")
        viewModel.onStop()
    }

    override fun onBackPressed() {
        if (viewModel.onBackPressed()) {
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BWU.log("MainActivity#onDestroy")
        viewModel.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.setting) {
            callSettingActivity()
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CODE_SETTING) {
            viewModel.onSettingUpdate()
        }
    }

    /**
     * 設定画面
     */
    private fun callSettingActivity() {
        val intent = Intent(this, SettingActivity::class.java)
        startActivityForResult(intent, CODE_SETTING)
    }

    /**
     * 広告を設定する
     */
    private fun setupAd() {
        //        AdView mAdView = (AdView) findViewById(R.id.ad);
        //        AdRequest adRequest = new AdRequest.Builder().build();
        //        mAdView.loadAd(adRequest);
    }

    companion object {
        private val CODE_SETTING = 1
    }
}
