package com.joytan.rec.main

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.GridView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.joytan.rec.R
import com.joytan.rec.data.QRecAnimator
import com.joytan.rec.databinding.FragmentMainBinding
import com.joytan.rec.main.handler.WarningHandler
import com.joytan.util.BWU
import it.sephiroth.android.library.xtooltip.Tooltip
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.main_control.*
import kotlinx.android.synthetic.main.main_script.*
import kotlinx.android.synthetic.main.main_script_dummy.*
import kotlinx.android.synthetic.main.main_status.*
import org.json.JSONArray
import java.io.File
import java.lang.Exception
import kotlin.math.abs


class MainFragment : Fragment(){

    companion object {
        // Set new project from "Project" fragment
        var currentProject = mapOf<String, String>()
        val defaultUid = FirebaseInstanceId.getInstance().getToken()?.substring(0, 22)
        var clientUid = defaultUid
    }

    // Context of host activity
    private lateinit var mContext: Context
    private val wh = WarningHandler()
    // Audio volume
    private lateinit var audioManager: AudioManager
    // Handling custom animation for buttion, explosion etc
    private val animator = QRecAnimator()

    private var mainScripts = mutableListOf<String>()
    // List of upper note (e.g. transliteration, roma-ji)
    private var upperNotes = mutableListOf<String>()
    // List of lower note (e.g. English translation)
    private var lowerNotes = mutableListOf<String>()

    private val mAuth = FirebaseAuth.getInstance()

    // Create a storage reference from our app
    private val fStorageRef = FirebaseStorage.getInstance().reference
    // Create a RealTime database reference from our app
    private val fDatabaseRef = FirebaseDatabase.getInstance().reference

    private var currentIndex = 0
    private var currentProjectId = String()
    private val projectJsonRef = fStorageRef.child("project_json")
    private var entryArray = JSONArray()

    // Map to save user progress record
    private var myProgress = mutableMapOf<String, MutableList<Int>>()
    private var adminProgress = mutableMapOf<String, MutableList<Int>>()

    private val adminUid = "3fG1zIUGn1hAf8JkDGd500uNuIi1"

    private val SWIPE_MIN_DISTANCE = 120
    private val SWIPE_MAX_OFF_PATH = 250
    private val SWIPE_THRESHOLD_VELOCITY = 200

    // ViewModel for Main screen
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(MainActivity.INFO_TAG, "onCreate")

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
                    if (adminProgress[currentProjectId]!!.indexOf(index) == -1) {
                        break
                    }
                }
                updateIndex(index)
                animator.fadeOut(script_layout_dummy, direction)
                animator.fadeIn(script_layout, direction)
            }
            override fun onGetAudioPath(): String {
                return "projects/${currentProjectId}/" +
                        (currentIndex + 1).toString().padStart(5, '0') +
                        "/$clientUid/${currentProject["wanted"]}.wav"
            }
            override fun onUpdateVolume(volume: Float) {
                //Update volume in the volume visualizer
                visualVolume.setVolume(volume)
            }
            override fun onShowWarningMessage(resId: Int) {
                wh.show(resId)
            }
            override fun onShowProgress(message: String) {
                updateMyProgress()
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
        })

        viewModel.onCreate()
        activity!!.volumeControlStream = AudioManager.STREAM_MUSIC
        this.audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        mAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            clientUid = if (user != null) mAuth.currentUser!!.uid else clientUid
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.e(MainActivity.INFO_TAG, "onCreateView")

        val binding = DataBindingUtil.inflate<FragmentMainBinding>(
                inflater, R.layout.fragment_main, container, false
        )
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.e(MainActivity.INFO_TAG, "onAttach")
        mContext = context
    }


    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        Log.e(MainActivity.INFO_TAG, "onViewStateRestored")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e(MainActivity.INFO_TAG, "onViewCreated")
        // Cannot create warning handler in onCreate nor onAttach
        // because some views are not inflated? created? yet
        // and they return null and crash.
        wh.onCreate(activity!!)
        activity!!.drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        if (currentProject.isEmpty()) {
            findNavController().navigate(R.id.action_nav_main_to_nav_project)
            MainActivity.pd.dismiss()
        } else {
            // If a project is set in MainFragment
            if (currentProjectId != currentProject["dirname"]!!) {
                currentProjectId = currentProject["dirname"]!!
                loadProgressData(myProgress,"users/$clientUid/audio/projects/${currentProject["dirname"]}")
                loadProgressData(adminProgress,"users/$adminUid/done/projects/${currentProject["dirname"]}")
                setNewProject()
            } else {
                // If the new project is the same with the previous one
                // Keep the previous index
                updateIndex(currentIndex)
            }
            updateToolbarTitle(currentProject["flags"] + currentProject["title"])
        }
        view.setOnTouchListener(object : OnMainTouchListener() {
            override fun onScriptNavigation(
                    e0: MotionEvent?, e1: MotionEvent?, vx: Float, vy: Float): Boolean {
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
        })
    }
    private fun setNewProject() {
        val tempProjectFile = File.createTempFile(currentProjectId, "json")
        MainActivity.pd.show ()
        projectJsonRef.child("$currentProjectId.json")
                .getFile(tempProjectFile).addOnSuccessListener {
                    val jsonString: String = tempProjectFile.readText(Charsets.UTF_8)
                    entryArray = JSONArray(jsonString)
                    updateScript(entryArray)
                    MainActivity.pd.dismiss()
                }.addOnFailureListener {
                    // FIXME
                    // If failed what can be done?
                    MainActivity.pd.dismiss()
                }
    }
    private fun loadProgressData (progressLookup : MutableMap<String, MutableList<Int>>,
                                  pathString : String) {
        try {
            val entryRef = fDatabaseRef.child(pathString)
            // Progress initialization for current project
            progressLookup[currentProjectId] = mutableListOf<Int>()
            entryRef.addListenerForSingleValueEvent((object: ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.value is Map<*, *>) {
                        val pData = p0.value as Map<String, Map<String, String>>
                        var newData = pData.map { it.key.toInt() - 1 }.toMutableList()
                        progressLookup[currentProjectId] = newData
                    }
                }
                override fun onCancelled(p0: DatabaseError) {
                }
            }))
        } catch (e: Exception) {
        }
    }
    /**
     * Update the main script
     */
    private fun updateScript(entriesJson: JSONArray) {
        val wantedKey = currentProject["wanted"]
        val upnKey = currentProject["upn"]
        val lonKey = currentProject["lon"]
        val newMainScripts = mutableListOf<String>()
        val newUpperNotes = mutableListOf<String>()
        val newLowerNotes = mutableListOf<String>()

        var nextIndex: Int

        for (i in 0 until entriesJson.length()) {
            newMainScripts.add(entriesJson.getJSONObject(i).getString(wantedKey))
            if (upnKey != "") {
                newUpperNotes.add(entriesJson.getJSONObject(i).getString(upnKey))
            }
            if (lonKey != "") {
                newLowerNotes.add(entriesJson.getJSONObject(i).getString(lonKey))
            }
        }
        mainScripts = newMainScripts
        upperNotes = newUpperNotes
        lowerNotes = newLowerNotes

        try {
            val unfinishedIndices =
                    (0 .. newMainScripts.size - 1).filter { it !in myProgress[currentProjectId]!! &&
                            it !in adminProgress[currentProjectId]!!}
            nextIndex = unfinishedIndices.shuffled().take(1)[0]
        } catch (e: Exception) {
            nextIndex = 0
        }

        updateIndex(nextIndex)
    }

    private fun updateIndex(newIndex: Int) {
        currentIndex = newIndex

        main_text.text = mainScripts[newIndex]

        upper_note.text = if (currentProject["upn"] != "") upperNotes[newIndex] else ""
        lower_note.text = if (currentProject["lon"] != "") lowerNotes[newIndex] else ""

        index_text!!.text = "${currentIndex + 1}/${mainScripts.size}"

        if (newIndex in adminProgress[currentProjectId]!!) {
            checkbox.visibility = View.VISIBLE
            checkbox.setImageResource(R.drawable.ic_done_24dp)
            checkbox_dummy.setImageResource(R.drawable.ic_done_24dp)
        }
        else if (newIndex in myProgress[currentProjectId]!!) {
            checkbox.visibility = View.VISIBLE
            checkbox.setImageResource(R.drawable.ic_thanks_24dp)
            checkbox_dummy.setImageResource(R.drawable.ic_thanks_24dp)
        }
        else {
            checkbox.visibility = View.INVISIBLE
        }
    }

    private fun updateMyProgress(){
        if (currentIndex !in myProgress[currentProjectId]!!)
            myProgress[currentProjectId]?.add(currentIndex)
        checkbox.visibility = View.VISIBLE
        // Explictly set bitmap source because initially not specified
        checkbox.setImageResource(R.drawable.ic_thanks_24dp)
        checkbox_dummy.setImageResource(R.drawable.ic_thanks_24dp)
    }

    private fun updateToolbarTitle(newTitle : String) {
        activity!!.toolbar_main.title = newTitle
    }

    private fun showGrid() {
        val gridView = layoutInflater.inflate(R.layout.grid_view, null) as GridView
        val builder = AlertDialog.Builder(mContext, R.style.GridDialog)
        val mList = mutableListOf<Int>()
        val titleView = layoutInflater.inflate(R.layout.grid_title, null)

        try {
            for (i in 1 until mainScripts.size + 1) {
                mList.add(i)
            }
            GridBaseAdapter(
                    mContext, mList, mainScripts, myProgress[currentProjectId], adminProgress[currentProjectId]
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

    override fun onDestroy() {
        super.onDestroy()
        BWU.log("MainActivity#onDestroy")
        viewModel.onDestroy()
    }
}