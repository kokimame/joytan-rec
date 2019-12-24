package com.joytan.rec.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.GridView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GestureDetectorCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.joytan.rec.R
import com.joytan.rec.databinding.FragmentMainBinding
import com.joytan.rec.setting.SettingActivity
import com.joytan.util.BWU
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.main_control.*
import kotlinx.android.synthetic.main.main_script.*
import kotlinx.android.synthetic.main.main_script_dummy.*
import kotlinx.android.synthetic.main.main_status.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import kotlin.math.abs


class MainFragment : Fragment(){

    private lateinit var mContext: Context
    /**
     * Warning
     */
    private val wh = WarningHandler()
    /**
     * Audio volume
     */
    private lateinit var audioManager: AudioManager

    /**
     * Handling custom animation for buttion, explosion etc
     */
    private val animator = QRecAnimator()

    /**
     * Tooltip
     */
    var loginTip: Tooltip? = null

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
    private var myDones = mutableMapOf<String, MutableList<Int>>()
    private var adminDones = mutableMapOf<String, MutableList<Int>>()

    private val adminUid = "3fG1zIUGn1hAf8JkDGd500uNuIi1"

    private val SWIPE_MIN_DISTANCE = 120
    private val SWIPE_MAX_OFF_PATH = 250
    private val SWIPE_THRESHOLD_VELOCITY = 200
    private val TIP_GRAVITY_TOP = Tooltip.Gravity.valueOf("TOP")
    private val TIP_GRAVITY_BOTTOM = Tooltip.Gravity.valueOf("BOTTOM")

    /**
     * ViewModel for Main screen
     */
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        viewModel = MainViewModel(activity!!, object : MainViewModel.Listener {
            override fun onDeleteRecord() {
                animator.startDeleteAnimation(statusFrame)
                animator.startDeleteAnimation(fab_delete)
                animator.startDeleteAnimation(fab_replay)
                animator.startDeleteAnimation(fab_share)
            }

            override fun onStartRecord() {
                // Explosion effect on the start of recording
                explosion.startRecordAnimation()
                // Fade-in status message
                animator.fadeIn(statusFrame)
                // Fade-in delete button
                if (fab_delete.visibility == View.INVISIBLE) {
                    animator.fadeIn(fab_delete)
                }
            }

            override fun onStopRecord() {
                // Display status
                animator.fadeIn(statusFrame)
                // Sub-control
                animator.fadeIn(fab_replay)
                animator.fadeIn(fab_share)
            }

            override fun onScriptNavigation(direction: String) {
                checkbox_dummy.visibility = checkbox.visibility
                main_text_dummy.text = main_text.text
                upper_note_dummy.text = upper_note.text
                lower_note_dummy.text = lower_note.text

                var index = currentIndex

                while (true) {
                    if (direction == "left") {
                        index -= 1
                        if (index < 0) index = mainScripts.size - 1
                    }
                    else if (direction == "right") {
                        index += 1
                        if (index > mainScripts.size - 1) index = 0
                    }
                    // Skip the finalized entries while swipe navigation
                    // TODO: Change bahavior when all completed
                    //  (Likely to not happen because such projects will be archived by admin)
                    if (adminDones[currentDirname]!!.indexOf(index) == -1) {
                        break
                    }
                }

                updateIndex(index)

                animator.fadeOut(script_layout_dummy, direction)
                animator.fadeIn(script_layout, direction)
            }

            override fun onGetAudioPath(): String {
                return "projects/${currentDirname}/" +
                        (currentIndex + 1).toString().padStart(5, '0') +
                        "/${MainActivity.clientUid}/${currentWantedKey}.wav"
            }

            override fun onUpdateVolume(volume: Float) {
                //Update volume in the volume visualizer
                visualVolume.setVolume(volume)
            }

            override fun onShowWarningMessage(resId: Int) {
                wh.show(resId)
            }

            override fun onShowProgress(message: String) {
                updateMyDones()
                MainActivity.pd.setMessage(message)
                MainActivity.pd.setCancelable(false)
                MainActivity.pd.show()
            }
            override fun onDismissProgress() {
                if (MainActivity.pd.isShowing) {
                    MainActivity.pd.dismiss()
                }
            }

            override fun onShowGrid() {
                showGrid()
            }

            override fun onCallSetting() {
                callSettingActivity()
            }

        });
        mAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                clientUid = mAuth.currentUser!!.uid
            } else {
                clientUid = defaultUid!!
            }
            MainActivity.clientUid = clientUid
            MainActivity.defaultUid = defaultUid!!

            // Load personal progress history based on the Realtime DB
            try {
                val entryRef = fDatabaseRef.child("users/$clientUid/audio/projects/")
                entryRef.addListenerForSingleValueEvent((object: ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.value is Map<*, *>) {
                            val pData = p0.value as Map<String, Map<String, String>>
                            for (projectName in pData.keys) {
                                var myDoneList = pData[projectName]!!.map { it.key.toInt() }
                                myDoneList = myDoneList.map { it - 1 }.toMutableList()
                                myDones[projectName] = myDoneList
                            }
                        }
                    }

                }))
            } catch (e: Exception) {
            }
        }
        viewModel.onCreate()
        val binding = DataBindingUtil.inflate<FragmentMainBinding>(
                inflater, R.layout.fragment_main, container, false
        )
        binding.viewModel = viewModel

        // Load the admin progress history based on the specific DB records
        try {
            // New way to load progress from Real Time DB
            val adminRef = fDatabaseRef.child("users/$adminUid/done/projects/")
            adminRef.addListenerForSingleValueEvent((object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.value is Map<*, *>) {
                        val pData = p0.value as Map<String, Map<String, Map<String, String>>>
                        for (projectName in pData.keys) {
                            var adminDoneList = pData[projectName]!!.map { it.key.toInt() }
                            adminDoneList = adminDoneList.map { it - 1 }.toMutableList()
                            adminDones[projectName] = adminDoneList
                        }
                    }
                }

            }))
        } catch (e: Exception) {
        }

        projectsRef.getFile(tempProjectsFile).addOnSuccessListener {
            val jsonString: String = tempProjectsFile.readText(Charsets.UTF_8)
            val tempProjectsJson = JSONObject(jsonString)

            for (projectName in tempProjectsJson.keys()) {
                projectsJson.put(tempProjectsJson.getJSONObject(projectName))
            }

            val projectsList = mutableListOf<String>()
            var closedProjectPos = mutableListOf<Int>()

            for (i in projectsJson.length() - 1 downTo 0) {
                val projectState = projectsJson.getJSONObject(i).getString("state")
                if (projectState == "close") {
                    closedProjectPos.add(i)
                }
            }
            closedProjectPos.forEach {
                projectsJson.remove(it)
            }


            for (i in 0 until projectsJson.length()) {
                val projectTitle = projectsJson.getJSONObject(i).getString("title")
                val projectDirname = projectsJson.getJSONObject(i).getString("dirname")
                val flagEmoji = projectsJson.getJSONObject(i).getString("flags")

                projectsList.add(flagEmoji + projectTitle)
                projectDirnames.add(projectDirname)

                // If progress of a project not found, intialize with empty or a list
                if (!myDones.containsKey(projectDirname))
                    myDones.put(projectDirname, mutableListOf<Int>())
                if (!adminDones.containsKey(projectDirname))
                    adminDones.put(projectDirname, mutableListOf<Int>())
            }
            val initialEntries = projectsJson.getJSONObject(0).getJSONArray("entries")
            val wantedKey = projectsJson.getJSONObject(0).getString("wanted")
            val upnKey = projectsJson.getJSONObject(0).getString("upn")
            val lonKey = projectsJson.getJSONObject(0).getString("lon")
            currentDirname = projectsJson.getJSONObject(0).getString("dirname")
            updateMainScript(initialEntries, wantedKey, upnKey, lonKey)

            val toolbar: Toolbar = activity!!.findViewById(R.id.toolbar_main)
            toolbar.title = projectsList[currentProjectsIndex]

            // Dismiss the initial (loading) progress dialog
            MainActivity.pd.dismiss()
        }.addOnFailureListener {
            val projectsList = listOf("Unknown error occurred!", "Please restart the app :(")
        }

        return binding.root
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        mContext = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity!!.volumeControlStream = AudioManager.STREAM_MUSIC

        this.audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Cannot create warning handler in onCreate nor onAttach
        // because some views are not inflated? created? yet
        // and they return null and crash.
        wh.onCreate(activity!!)
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
                    (0 .. newMainScripts.size - 1).filter { it !in myDones[currentDirname]!! &&
                            it !in adminDones[currentDirname]!!}
            nextIndex = unfinishedIndices.shuffled().take(1)[0]
        } catch (e: Exception) {
            nextIndex = 0
        }

        updateIndex(nextIndex)
    }

    private fun updateIndex(newIndex: Int) {
        currentIndex = newIndex
        currentTotalIndex = mainScripts.size

        main_text.text = mainScripts[newIndex]

        // FIXME: Maybe there is a better way to update upper/lower note
        if (currentUpn != "") {
            upper_note.text = upnScripts[newIndex]
        } else {
            upper_note.text = ""
        }
        if (currentLon != "") {
            lower_note.text = lonScripts[newIndex]
        } else {
            lower_note.text = ""
        }
        index_text!!.text = "${currentIndex + 1}/${currentTotalIndex}"

        if (newIndex in adminDones[currentDirname]!!) {
            checkbox.visibility = View.VISIBLE
            checkbox.setImageResource(R.drawable.ic_done_24dp)
            checkbox_dummy.setImageResource(R.drawable.ic_done_24dp)
        }
        else if (newIndex in myDones[currentDirname]!!) {
            checkbox.visibility = View.VISIBLE
            checkbox.setImageResource(R.drawable.ic_thanks_24dp)
            checkbox_dummy.setImageResource(R.drawable.ic_thanks_24dp)
        }
        else {
            checkbox.visibility = View.INVISIBLE
        }
    }

    private fun updateMyDones(){
        if (currentIndex !in myDones[currentDirname]!!)
            myDones[currentDirname]?.add(currentIndex)
        checkbox.visibility = View.VISIBLE
        // Explictly set bitmap source because initially not specified
        checkbox.setImageResource(R.drawable.ic_thanks_24dp)
        checkbox_dummy.setImageResource(R.drawable.ic_thanks_24dp)
    }
    private fun showGrid() {
        val gridView = layoutInflater.inflate(R.layout.grid_view, null) as GridView//GridView(this)
        val builder = AlertDialog.Builder(mContext, R.style.GridDialog);
        val mList = mutableListOf<Int>()
        val titleView = layoutInflater.inflate(R.layout.grid_title, null)

        try {
            for (i in 1 until currentTotalIndex + 1) {
                mList.add(i);
            }

            GridBaseAdapter(
                    mContext, mList, mainScripts, myDones, adminDones, currentDirname
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
        }
    }

    private fun getClosePolicy(): ClosePolicy {
        val builder = ClosePolicy.Builder()
        return builder.build()
    }

    private fun getTooltip(context: Context, anchor: View, text: String, xoff: Int): Tooltip {
        var tooltip: Tooltip
        tooltip = Tooltip.Builder(context)
                .anchor(anchor, xoff, 0, false)
                .text(text)
                .styleId(null)
                .typeface(null)
                .maxWidth(resources.displayMetrics.widthPixels / 2)
                .arrow(true)
                .floatingAnimation(Tooltip.Animation.DEFAULT)
                .closePolicy(getClosePolicy())
                // Show for the first 7 seconds
                .showDuration(7000)
                .overlay(false)
                .create()
        return tooltip
    }

    fun onFling(e0: MotionEvent?, e1: MotionEvent?, vx: Float, vy: Float) : Boolean{
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

//    override fun onBackPressed() {
//        if (viewModel.onBackPressed()) {
//        } else {
//            super.onBackPressed()
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        BWU.log("MainActivity#onDestroy")
        viewModel.onDestroy()
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
//        if (id == R.id.fab_setting) {
//            callSettingActivity()
//            return true
//        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MainActivity.CODE_SETTING) {
            viewModel.onSettingUpdate()
        }
    }

    /**
     * Setting menu
     */
    private fun callSettingActivity() {
        val intent = Intent(mContext, SettingActivity::class.java)
        startActivityForResult(intent, MainActivity.CODE_SETTING)
    }
    /**
     * 実行時パーミッションの解説
     */
//    private fun showInformation() {
//        val adb = AlertDialog.Builder(this)
//        adb.setTitle(R.string.title_permission)
//        adb.setMessage(R.string.permission_audio)
//        adb.setCancelable(false)
//        adb.setPositiveButton(R.string.ok) { dialog, which ->
//            callApplicationDetailActivity()
//            finish()
//        }.setNegativeButton(R.string.cancel) { dialog, which -> finish() }
//        val dialog = adb.show()
//    }

    /**
     * アプリ詳細を呼び出す
     */
//    private fun callApplicationDetailActivity() {
//        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
//                Uri.parse("package:$packageName"))
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        startActivity(intent)
//    }
}