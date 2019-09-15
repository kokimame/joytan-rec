package com.joytan.rec.main

import android.Manifest
import android.app.ProgressDialog
//import android.support.v7.app.AppCompatActivity
//import android.databinding.DataBindingUtil
import android.media.AudioManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog

import com.joytan.rec.R

import com.joytan.rec.setting.SettingActivity
import com.joytan.util.BWU
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_control.*
import kotlinx.android.synthetic.main.main_status.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.joytan.rec.databinding.ActivityMainBinding
import com.joytan.rec.setting.SettingFragment
import kotlinx.android.synthetic.main.main_script.*
import kotlinx.android.synthetic.main.main_script_dummy.*
import org.json.JSONArray
import java.io.File

import org.json.JSONObject
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception
import kotlin.math.abs


/**
 * メイン画面
 */
class MainActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    /**
     * Warning
     */
    private val wh = WarningHandler()
    /**
     * Audio volume
     */
    private lateinit var audioManager: AudioManager
    /**
     * Detect swipe gesture etc
     */
    private lateinit var mDetector: GestureDetectorCompat
    /**
     * Show progress of data transaction with Firebase server
     */
    private lateinit var pd: ProgressDialog

    /**
     * Handling custom animation for buttion, explosion etc
     */
    private val animator = QRecAnimator()


    private var projectsJson = JSONArray()
    private var mainScripts = mutableListOf<String>()
    // List of upper note (e.g. transliteration, roma-ji)
    private var upnScripts = mutableListOf<String>()
    // List of lower note (e.g. English translation)
    private var lonScripts = mutableListOf<String>()

    /**
     * TODO: More persistent ID demanded
     * Unique ID of users. Used to manage user contribution, credits etc
     */
    val defaultUid = FirebaseInstanceId.getInstance().getToken()?.substring(0, 22)
    lateinit var clientUid: String
    private val mAuth = FirebaseAuth.getInstance()

    // Create a storage reference from our app
    private val fStorageRef = FirebaseStorage.getInstance().reference
    // Create a RealTime database reference from our app
    private val fDatabaseRef = FirebaseDatabase.getInstance().reference

    private val projectsRef = fStorageRef.child("projects_structure.json")
    // User inFOrmation
    private val tempProjectsFile = File.createTempFile("projects", "json")

    private var currentIndex = 0
    private var currentTotalIndex = 0
    // Key for main script
    private var currentWantedKey = "atop"
    // Key for upper note
    private var currentUpn = ""
    // Key for lower note
    private var currentLon = ""
    private var currentProjectsIndex = 0
    private var currentDirname = ""
    private var projectDirnames = mutableListOf<String>()
    private lateinit var padapter : ProjectsArrayAdapter
    // Map to save user progress record
    private var progressDB = mutableMapOf<String, MutableList<Int>>()

    // Internet connection receiver/monitor
    private val connFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION).apply {
        addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
    }
    private val connReceiver = ConnectionReceiver()

    private val SWIPE_MIN_DISTANCE = 120
    private val SWIPE_MAX_OFF_PATH = 250
    private val SWIPE_THRESHOLD_VELOCITY = 200
    private val INFO_TAG = "kohki"

    companion object {
        private val CODE_SETTING = 1
        var defaultUid = ""
        var clientUid = ""
    }

    /**
     * ViewModel for Main screen
     */
    private val viewModel = MainViewModel(this, object : MainViewModel.Listener {
        override fun onDeleteRecord() {
            animator.startDeleteAnimation(statusFrame)
            animator.startDeleteAnimation(delete)
            animator.startDeleteAnimation(replay)
            animator.startDeleteAnimation(share)
        }


        override fun onStartRecord() {
            // Explostion effect on the start of recording
            explosion.startRecordAnimation()
            // Fade-in status message
            animator.fadeIn(statusFrame)
            // Fade-in delete button
            if (delete.visibility == View.INVISIBLE) {
                animator.fadeIn(delete)
            }
        }

        override fun onStopRecord() {
            // Display status
            animator.fadeIn(statusFrame)
            // Sub-control
            animator.fadeIn(replay)
            animator.fadeIn(share)
        }

        override fun onScriptNavigation(direction: String) {
            val deleteLayout = findViewById<LinearLayout>(R.id.script_layout_dummy)
            checkbox_dummy.visibility = checkbox.visibility
            main_text_dummy.text = main_text.text
            upper_note_dummy.text = upper_note.text
            lower_note_dummy.text = lower_note.text

            // FIXME: Error will happen on duplicate entries
            var index = mainScripts.indexOf(main_text.text)

            if (direction == "left") {
                index -= 1
                if (index < 0) index = mainScripts.size - 1
            }
            else if (direction == "right") {
                index += 1
                if (index > mainScripts.size - 1) index = 0
            }

            main_text.text = mainScripts.get(index)
            if (currentUpn != "") {
                upper_note.text = upnScripts[index]
            }
            if (currentLon != "") {
                lower_note.text = lonScripts[index]
            }
            updateIndex(index)

            animator.fadeOut(script_layout_dummy, direction)
            animator.fadeIn(script_layout, direction)
        }

        override fun onGetAudioPath(): String {
            return "projects/${currentDirname}/" +
                    (currentIndex + 1).toString().padStart(5, '0') +
                    "/${currentWantedKey}/${MainActivity.clientUid}/${currentWantedKey}.wav"
        }

        override fun onUpdateVolume(volume: Float) {
            //Update volume in the volume visualizer
            visualVolume.setVolume(volume)
        }

        override fun onShowWarningMessage(resId: Int) {
            wh.show(resId)
        }

        override fun onShowProgress(message: String) {
            updateProgressDB()
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

        override fun onShowGrid() {
            showGrid()
        }

        override fun onCallSetting() {
            callSettingActivity()
        }

    });


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(connReceiver, connFilter)

        // Get permission first before making progressDB into the user's storage
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

        // DO NOT ALLOW USERS TO TOUCH BUTTON BEFORE LOADING CONTENT
        this.pd = ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT)
        // TODO Synchronize with the actual internet connection time
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        pd.setMessage("Loading data from server ...")
        pd.setCancelable(false)
        pd.show()
        Handler().postDelayed({pd.dismiss()}, 2000)

        // If authorized, use the auth UID.
        if (mAuth.currentUser != null) {
            clientUid = mAuth.currentUser!!.uid
        } else {
            clientUid = defaultUid!!
        }
        MainActivity.clientUid = clientUid
        MainActivity.defaultUid = defaultUid!!

        // Detect gestures e.g. swipe
        this.mDetector = GestureDetectorCompat(this, this)

        //警告担当
        wh.onCreate(this)
        viewModel.onCreate()

        try {
            // New way to load progress from Real Time DB
            val entryRef = fDatabaseRef.child("users/$clientUid/audio/projects/")
            Log.i(INFO_TAG, "Try loading progress from ... users/$clientUid/audio/projects/")
            entryRef.addListenerForSingleValueEvent((object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.value is Map<*, *>) {
                        val pData = p0.value as Map<String, Map<String, Map<String, String>>>
                        for (projectName in pData.keys) {
                            var myDoneList = pData[projectName]!!.map { it.key.toInt() }
                            myDoneList = myDoneList.map { it - 1 }.toMutableList()
                            progressDB[projectName] = myDoneList
                        }
                    }
                }

            }))
        } catch (e: Exception) {
            Log.i(INFO_TAG, "Exception while loading progressDB ... " + e.toString())
        }

        projectsRef.getFile(tempProjectsFile).addOnSuccessListener {
            val jsonString: String = tempProjectsFile.readText(Charsets.UTF_8)
            val tempProjectsJson = JSONObject(jsonString)

            for (projectName in tempProjectsJson.keys()) {
                projectsJson.put(tempProjectsJson.getJSONObject(projectName))
            }

            val projectsList = mutableListOf<String>()
            val initialEntries = projectsJson.getJSONObject(0).getJSONArray("entries")
            val wantedKey = projectsJson.getJSONObject(0).getString("wanted")
            val upnKey = projectsJson.getJSONObject(0).getString("upn")
            val lonKey = projectsJson.getJSONObject(0).getString("lon")

            currentDirname = projectsJson.getJSONObject(0).getString("dirname")

            for (i in 0 until projectsJson.length()) {
                val projectTitle = projectsJson.getJSONObject(i).getString("title")
                val projectDirname = projectsJson.getJSONObject(i).getString("dirname")
                val flagEmoji = projectsJson.getJSONObject(i).getString("flags")
                val doneJson = projectsJson.getJSONObject(i).getJSONArray("done")
                val doneList = mutableListOf<Int>()

                projectsList.add(flagEmoji + projectTitle)
                projectDirnames.add(projectDirname)

                // Import entries finished by community
                for (i in 0 until doneJson.length()) {
                    doneList.add(doneJson.getInt(i))
                }

                // If progress of a project not found, intialize with empty or a list
                if (!progressDB.containsKey(projectDirname))
                    progressDB.put(projectDirname, doneList)
                else {
                    val updatedIndices = progressDB.get(projectDirname)!!
                    for (i in 0 until doneList.size) {
                        if (doneList[i] !in updatedIndices) {
                            updatedIndices.add(doneList[i])
                        }
                    }
                    progressDB.put(projectDirname, updatedIndices)
                }
            }


            setupSpinner(projectsList)
            updateMainScript(initialEntries, wantedKey, upnKey, lonKey)

        }.addOnFailureListener {
            val projectsList = listOf("Unknown error occurred!", "Please restart the app :(")
            setupSpinner(projectsList)
        }
    }

    /**
     * Update the main script
     */
    private fun updateMainScript(entriesJson: JSONArray,
                                 wantedKey: String, upnKey: String, lonKey: String) {
        val newMainScripts = mutableListOf<String>()
        val newUpnScripts = mutableListOf<String>()
        val newLonScripts = mutableListOf<String>()

        var nextIndex: Int

        for (i in 0 until entriesJson.length()) {
            newMainScripts.add(entriesJson.getJSONObject(i).getString(wantedKey))
        }
        if (currentUpn != "") {
            for (i in 0 until entriesJson.length()) {
                newUpnScripts.add(entriesJson.getJSONObject(i).getString(upnKey))
            }
        }
        if (currentLon != "") {
            for (i in 0 until entriesJson.length()) {
                newLonScripts.add(entriesJson.getJSONObject(i).getString(lonKey))
            }
        }
        mainScripts = newMainScripts
        upnScripts = newUpnScripts
        lonScripts = newLonScripts

        try {
            val unfinishedIndices =
                    (0 .. newMainScripts.size - 1).filter { it !in progressDB[currentDirname]!! }
            nextIndex = unfinishedIndices.shuffled().take(1)[0]
        } catch (e: Exception) {
            nextIndex = 0
        }

        updateIndex(nextIndex)
    }

    private fun updateIndex(newIndex: Int) {
        val indexText = findViewById<TextView>(R.id.index_text)
        val checkBox = findViewById<ImageView>(R.id.checkbox)
        currentIndex = newIndex
        currentTotalIndex = mainScripts.size

        main_text.text = mainScripts[newIndex]
        if (currentUpn != "") {
            upper_note.text = upnScripts[newIndex]
        }
        if (currentLon != "") {
            lower_note.text = lonScripts[newIndex]
        }
        indexText.setText("${currentIndex + 1}/${currentTotalIndex}")

        if (newIndex in progressDB[currentDirname]!!) {
            checkBox.visibility = View.VISIBLE
        } else {
            checkBox.visibility = View.INVISIBLE
        }
    }

    private fun updateProgressDB(){
        if (currentIndex !in progressDB[currentDirname]!!)
            progressDB[currentDirname]?.add(currentIndex)
        val checkBox = this.findViewById<ImageView>(R.id.checkbox)
        checkBox.visibility = View.VISIBLE
    }
    /*
     * Setup the project spinner
     */
    private fun setupSpinner(projectsList: List<String>) {
        val spinner: Spinner = findViewById(R.id.projects_spinner)

        padapter = ProjectsArrayAdapter(
                this, R.layout.spinner_item, projectsList, currentProjectsIndex
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
                currentDirname = projectsJson.getJSONObject(position).getString("dirname")
                currentProjectsIndex = projectDirnames.indexOf(currentDirname)
                currentUpn = projectsJson.getJSONObject(position).getString("upn")
                currentLon = projectsJson.getJSONObject(position).getString("lon")
                padapter.setCurrentIndex(currentProjectsIndex)
                updateMainScript(entriesJson, currentWantedKey, currentUpn, currentLon)
            } // to close the onItemSelected
            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun showGrid() {
        val gridView = layoutInflater.inflate(R.layout.grid_view, null) as GridView//GridView(this)
        val builder = AlertDialog.Builder(this, R.style.GridDialog);
        val mList = mutableListOf<Int>()
        val titleView = layoutInflater.inflate(R.layout.grid_title, null)

        try {
            for (i in 1 until currentTotalIndex + 1) {
                mList.add(i);
            }

            GridBaseAdapter(
                    this, mList, mainScripts, progressDB, currentDirname
            ).also { adapter ->
                gridView.setAdapter(adapter)
            }

            builder.setTitle("Jump to")
            builder.setView(gridView)
            builder.setCustomTitle(titleView)
            val ad = builder.show()

            gridView.onItemClickListener = object : AdapterView.OnItemClickListener {
                override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    updateIndex(position)
                    ad.dismiss()
                }
            }
            gridView.setSelection(currentIndex)
        } catch (e: Exception) {
            Log.i(INFO_TAG, "Grid initialization failed but ignored")
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
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(e0: MotionEvent?, e1: MotionEvent?, vx: Float, vy: Float): Boolean {
        try {
            if (abs(e0!!.getY() - e1!!.getY()) > SWIPE_MAX_OFF_PATH) {
                return false
            } else if (e0!!.getX() - e1!!.getX() > SWIPE_MIN_DISTANCE &&
                    abs(vx) > SWIPE_THRESHOLD_VELOCITY) {
                viewModel.onRightClicked()
                return true
            } else if (e1!!.getX() - e0!!.getX() > SWIPE_MIN_DISTANCE &&
                    abs(vx) > SWIPE_THRESHOLD_VELOCITY) {
                viewModel.onLeftClicked()
                return true
            }
        } catch (e: Exception) {

        }
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return true
    }

    override fun onLongPress(p0: MotionEvent?) {
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
     * Setting menu
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

    /**
     * 実行時パーミッションの解説
     */
    private fun showInformation() {
        val adb = AlertDialog.Builder(this)
        adb.setTitle(R.string.title_permission)
        adb.setMessage(R.string.permission_audio)
        adb.setCancelable(false)
        adb.setPositiveButton(R.string.ok) { dialog, which ->
            callApplicationDetailActivity()
            finish()
        }.setNegativeButton(R.string.cancel) { dialog, which -> finish() }
        val dialog = adb.show()
    }

    /**
     * アプリ詳細を呼び出す
     */
    private fun callApplicationDetailActivity() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}
