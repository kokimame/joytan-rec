package jp.bellware.echo.main

import android.Manifest
import android.app.ProgressDialog
//import android.support.v7.app.AppCompatActivity
//import android.databinding.DataBindingUtil
import android.media.AudioManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.AdapterView
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
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.MotionEventCompat
import androidx.databinding.DataBindingUtil
import com.google.firebase.storage.FirebaseStorage
import jp.bellware.echo.databinding.ActivityMainBinding
import org.json.JSONArray
import java.io.File
import kotlin.math.max
import kotlin.math.min

import org.json.JSONObject
import java.lang.Exception
import kotlin.math.abs


/**
 * メイン画面
 */
class MainActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    /**
     * 警告担当
     */
    private val wh = WarningHandler()
    /**
     * ボリュームの設定担当
     */
    private lateinit var audioManager: AudioManager
    private lateinit var mDetector: GestureDetectorCompat

    private lateinit var pd: ProgressDialog


    /**
     * アニメ担当
     */
    private val animator = QRecAnimator()

    private lateinit var projectsJson: JSONArray
    private var mainScripts = mutableListOf<String>()

    private val fStorage = FirebaseStorage.getInstance()
    // Create a storage reference from our app
    private val fStorageRef = fStorage.reference
    private val projectsRef = fStorageRef.child("projects_structure.json")
    private val tempProjectsFile = File.createTempFile("projects", "json")

    private var currentIndex = 0
    private var currentTotalIndex = 0
    private var currentWantedKey = "atop"
    private lateinit var currentDirname: String

    private val SWIPE_MIN_DISTANCE = 120
    private val SWIPE_MAX_OFF_PATH = 250
    private val SWIPE_THRESHOLD_VELOCITY = 200
    private val INFO_TAG = "joytan"

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
                index -= 1
                if (index < 0) index = mainScripts.size - 1
            }
            else if (direction == "right") {
                index += 1
                if (index > mainScripts.size - 1) index = 0
            }
            mainText.setText(mainScripts.get(index))
            updateIndex(index)
        }

        override fun getAudioOutputPath(): String {
            return "projects/${currentDirname}/${currentIndex}/${currentWantedKey}.wav"
        }

        override fun onUpdateVolume(volume: Float) {
            //視覚的ボリュームを更新する
            visualVolume.setVolume(volume)
        }

        override fun onShowWarningMessage(resId: Int) {
            //警告を表示する
            wh.show(resId)
        }

        override fun onShowProgress(message: String) {
            pd.setMessage(message)
            pd.setCancelable(false)
            pd.show()
            Handler().postDelayed({pd.dismiss()}, 500)
        }
        override fun onDismissProgress() {
            if (pd.isShowing) {
                pd.dismiss()
            }
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

        this.pd = ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT)
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER)

        this.mDetector = GestureDetectorCompat(this, this)

        //警告担当
        wh.onCreate(this)
        viewModel.onCreate()

        projectsRef.getFile(tempProjectsFile).addOnSuccessListener {
            val jsonString: String = tempProjectsFile.readText(Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)

            projectsJson = jsonObject.getJSONArray("projects")
            val projectsList = mutableListOf<String>()
            val initialEntries = projectsJson.getJSONObject(0).getJSONArray("entries")
            val wantedKey = projectsJson.getJSONObject(0).getString("wanted")

            for (i in 0 until projectsJson.length()) {
                var projectTitle = projectsJson.getJSONObject(i).getString("title")
                projectsList.add(projectTitle)
            }
            setupSpinner(projectsList)
            updateMainScript(initialEntries, wantedKey)

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
     * Update the main script
     */
    private fun updateMainScript(entriesJson: JSONArray, wantedKey: String) {
        val newMainScript = mutableListOf<String>()
        for (i in 0 until entriesJson.length()) {
            newMainScript.add(entriesJson.getJSONObject(i).getString(wantedKey))
        }
        mainScripts = newMainScript
        val mainText: TextView = findViewById(R.id.main_text)
        mainText.text = mainScripts[0]

        currentTotalIndex = mainScripts.size
        updateIndex(0)
    }

    private fun updateIndex(newIndex: Int) {
        val indexText = findViewById<TextView>(R.id.index_text)
        currentIndex = newIndex
        indexText.setText("${currentIndex + 1}/${currentTotalIndex}")
    }
    /*
     * Setup the project spinner
     */
    private fun setupSpinner(projectsList: List<String>) {
        val spinner: Spinner = findViewById(R.id.projects_spinner)

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
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val entriesJson = projectsJson.getJSONObject(position).getJSONArray("entries")
                currentWantedKey = projectsJson.getJSONObject(position).getString("wanted")
                currentDirname = projectsJson.getJSONObject(position).getString("dir_name")
                updateMainScript(entriesJson, currentWantedKey)
            } // to close the onItemSelected

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (mDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onShowPress(p0: MotionEvent?) {
//        Log.i(INFO_TAG, "onShowPress $p0")
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
//        Log.i(INFO_TAG, "onSingleTapUp $p0")
        return true
    }

    override fun onDown(p0: MotionEvent?): Boolean {
//        Log.i(INFO_TAG, "onDown $p0")
        return true
    }

    override fun onFling(e0: MotionEvent?, e1: MotionEvent?, vx: Float, vy: Float): Boolean {
        try {
            if (abs(e0!!.getY() - e1!!.getY()) > SWIPE_MAX_OFF_PATH) {
                return false
            } else if (e0!!.getX() - e1!!.getX() > SWIPE_MIN_DISTANCE &&
                    abs(vx) > SWIPE_THRESHOLD_VELOCITY) {
                Log.i(INFO_TAG, "swipe left!!")
                viewModel.onRightClicked()
                return true
            } else if (e1!!.getX() - e0!!.getX() > SWIPE_MIN_DISTANCE &&
                    abs(vx) > SWIPE_THRESHOLD_VELOCITY) {
                Log.i(INFO_TAG, "swipe right!!")
                viewModel.onLeftClicked()
                return true
            }
        } catch (e: Exception) {

        }
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
//        Log.i(INFO_TAG, "onScroll $p0, $p1")
        return true
    }

    override fun onLongPress(p0: MotionEvent?) {
//        Log.i(INFO_TAG, "$p0")
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
