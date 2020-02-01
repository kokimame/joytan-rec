package com.joytan.rec.main

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.TwitterAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.joytan.rec.R
import com.joytan.rec.data.QRecAnimator
import com.joytan.rec.databinding.FragmentMainBinding
import com.joytan.rec.handler.WarningHandler
import com.joytan.rec.setting.ProjectActivity
import com.joytan.util.BWU
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.background_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.main_control.*
import kotlinx.android.synthetic.main.main_script.*
import kotlinx.android.synthetic.main.main_script_dummy.*
import kotlinx.android.synthetic.main.main_status.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.json.JSONArray
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs

class MainFragment : Fragment(){

    companion object {
        // Set new project from "Project" fragment
        var currentProject = mapOf<String, String>()
        val defaultUid = FirebaseInstanceId.getInstance().getToken()?.substring(0, 22)
        var clientUid = defaultUid
        val PROJECT_STARTUP_CODE = 100
    }

    // Context of host activity
    private lateinit var mContext : Context
    private lateinit var mActivity : Activity

    private val wh = WarningHandler()
    // Audio volume
    private lateinit var audioManager: AudioManager
    // Handling custom animation for buttion, explosion etc
    private val animator = QRecAnimator()

    private var entryIds = mutableListOf<String>()
    private var mainScripts = mutableListOf<String>()
    // List of upper note (e.g. transliteration, roma-ji)
    private var upperNotes = mutableListOf<String>()
    // List of lower note (e.g. English translation)
    private var lowerNotes = mutableListOf<String>()

    private val mAuth = FirebaseAuth.getInstance()

    // Create a storage reference from our app
    private val fStorageRef = FirebaseStorage.getInstance().reference
    // Create a Firestore database reference from our app
    private val db = FirebaseFirestore.getInstance()

    private var clientDisplayName = ""
    private var clientMediaLink = ""
    private var clientPhotoUrl = ""
    private var currentIndex = 0
    private var projectName = String()
    private var lastProjectName = String()
    private val projectJsonRef = fStorageRef.child("project_json")
    private var entryArray = JSONArray()

    // Map to project name to a list of indices
    private var clientProgressLookup = mutableMapOf<String, MutableList<Int>>()
    private var adminProgressLookup = mutableMapOf<String, MutableList<Int>>()

    private val SWIPE_MIN_DISTANCE = 120
    private val SWIPE_MAX_OFF_PATH = 250
    private val SWIPE_THRESHOLD_VELOCITY = 200

    private val COLOR_CLIENT_PROGRESS = Color.YELLOW
    private val COLOR_ADMIN_PROGRESS = Color.GREEN

    // ViewModel for Main screen
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(MainActivity.DEBUG_TAG, "onCreate")

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
                    // TODO: Change behavior when all completed
                    //  (Likely to not happen because such projects will be archived by admin)
                    if (adminProgressLookup[projectName]!!.indexOf(index) == -1) {
                        break
                    }
                }
                updateIndex(index)
                animator.fadeOut(script_layout_dummy, direction)
                animator.fadeIn(script_layout, direction)
            }

            override fun onUpdateProgress(progress : MutableList<Int>, color : Int, initialIndex : Int, totalSize : Int) {
                circular_progress.setProgress(progress, color, initialIndex, totalSize)
            }

            override fun onGetVoiceProps(): HashMap<String, *> {
                // Pass to the Share Handler when it submits voice recording to the storage
                return hashMapOf(
                        "client_id" to clientUid,
                        "entry_id" to entryIds[currentIndex],
                        "project" to projectName,
                        "username" to clientDisplayName,
                        "user_link" to clientMediaLink,
                        "photo_url" to clientPhotoUrl,
                        "created_at" to FieldValue.serverTimestamp(),
                        "vote_like" to 0,
                        "vote_dislike" to 0,
                        "main_script" to mainScripts[currentIndex],
                        "lon" to if (lowerNotes.size == 0) "" else lowerNotes[currentIndex],
                        "upn" to if (upperNotes.size == 0) "" else upperNotes[currentIndex]
                )
            }
            override fun onUpdateVolume(volume: Float) {
                //Update volume in the volume visualizer
                visualVolume.setVolume(volume)
            }
            override fun onShowWarningMessage(resId: Int) {
                wh.show(resId)
            }
            override fun onShowProgress(message: String) {
                // FIXME: Logic could be simpler
                updateClientProgress()
                MainActivity.pd.setMessage(message)
                MainActivity.pd.setCancelable(false)
                MainActivity.pd.show()
            }
            override fun onDismissProgress() {
                if (MainActivity.pd.isShowing) {
                    MainActivity.pd.dismiss()
                }
            }
            override fun onStartComment() {
                buildCommentDialog()

            }
            override fun onShowGrid() {
                buildToCGrid()
            }
        })

        viewModel.onCreate()
        activity!!.volumeControlStream = AudioManager.STREAM_MUSIC
        this.audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // TODO: Retrieve last project name from shared preference or Firestore
        // and verify whether the project is still active, if not go to the projection startup
        if (projectName.isEmpty()) {
            val sharedPref = activity?.getSharedPreferences(
                    getString(R.string.saved_user_data), Context.MODE_PRIVATE)
            lastProjectName = sharedPref!!.getString(getString(R.string.last_project_name),"default_project")!!
        }
        Log.e(MainActivity.DEBUG_TAG, "got from sharedPref $lastProjectName")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.e(MainActivity.DEBUG_TAG, "onCreateView")

        val binding = DataBindingUtil.inflate<FragmentMainBinding>(
                inflater, R.layout.fragment_main, container, false
        )
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.e(MainActivity.DEBUG_TAG, "onAttach")
        mContext = context
        mActivity = mContext as Activity

        mAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                clientUid = user.uid
                mActivity.nav_username.text = user.displayName
                mActivity.nav_client_id.text = user.uid.substring(0, 8) + ".."
                mActivity.nav_view.menu.findItem(R.id.nav_logout).isVisible = true
                mActivity.nav_view.menu.findItem(R.id.nav_signup).isVisible = false

                user.providerData.forEach {profile ->
                    if (profile.providerId == TwitterAuthProvider.PROVIDER_ID) {
                        clientMediaLink = "tw::" + profile.uid
                        // Name, email address, and profile photo Url
                        clientDisplayName = profile.displayName!!
                        clientPhotoUrl = profile.photoUrl.toString()

                    } else if (profile.providerId == FacebookAuthProvider.PROVIDER_ID) {
                        clientMediaLink = "fb::" + profile.uid
                        // Name, email address, and profile photo Url
                        clientDisplayName = profile.displayName!!
                        clientPhotoUrl = profile.photoUrl.toString()
                    }
                }
            } else {
                mActivity.nav_username.text = ""
                mActivity.nav_client_id.text = "-"
                mActivity.nav_view.menu.findItem(R.id.nav_logout).isVisible = false
                mActivity.nav_view.menu.findItem(R.id.nav_signup).isVisible = true
            }
            try {
                user!!.providerData.forEach {profile ->
                    if (profile.providerId == TwitterAuthProvider.PROVIDER_ID) {
                        val uid = profile.uid
                        // Name, email address, and profile photo Url
                        val name = profile.displayName
                        val email = profile.email
                        val photoUrl = profile.photoUrl
                        Log.e(MainActivity.DEBUG_TAG, String.format("uid=%s name=%s email=%s url=%s", uid, name, email, photoUrl));

                    }
                }
            } catch (e : Exception) {
                Log.e(MainActivity.DEBUG_TAG, "Exception here ... $e")
            }

            db.collection("users").document(clientUid!!)
                    .get()
                    .addOnSuccessListener { document ->
                        try {
                            if (document.get("voice_add") != null) {
                                mActivity.cnt_voice.text = document.get("voice_add").toString()
                            }
                        } catch (e: Exception) { Log.e(MainActivity.DEBUG_TAG, e.toString()) }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(MainActivity.DEBUG_TAG, "Error getting user counter ... ", exception)
                    }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e(MainActivity.DEBUG_TAG, "onViewCreated")
        // Cannot create warning handler in onCreate nor onAttach
        // because some views are not inflated? created? yet
        // and they return null and crash.
        wh.onCreate(activity!!)

        activity!!.drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        // FIXME This logic could be written smarter
        ///////////////////////////////////////////
        if (currentProject.isEmpty()) {
            // When project information is not available, try to load the last project
            db.collection("projects").document(lastProjectName)
                    .get()
                    .addOnCompleteListener { task ->
                        var result = task.result
                        try {
                            currentProject = result!!.data as Map<String, String>
                            setNewProject(lastProjectName)
                            projectName = lastProjectName
                        } catch (exception : Exception) {
                            currentProject = mapOf()
                            Log.e(MainActivity.DEBUG_TAG, "Error getting documents ... ", exception)
                            // If loading the last project fails, go to the project fragment
                            MainActivity.pd.dismiss()
                            val intent = Intent(mActivity, ProjectActivity::class.java)
                            startActivityForResult(intent, PROJECT_STARTUP_CODE)
                        }
                    }
        } else {
            if (projectName != currentProject["dirname"]!!) {
                // New project selected from the Project fragment
                projectName = currentProject["dirname"]!!
                setNewProject(projectName)
            } else {
                updateIndex(currentIndex)
                circular_progress.setProgress(clientProgressLookup[projectName]!!,
                        COLOR_CLIENT_PROGRESS, currentIndex, currentProject["size"]!!.toInt())
                circular_progress.setProgress(adminProgressLookup[projectName]!!,
                        COLOR_ADMIN_PROGRESS, currentIndex, currentProject["size"]!!.toInt())
                updateToolbarTitle()
            }
        }
        ///////////////////////////////////
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == resultCode || requestCode == MainActivity.RC_SIGN_IN) {
            mActivity.recreate()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setNewProject(projectName : String) {
        val tempProjectFile = File.createTempFile(projectName, "json")
        Log.e(MainActivity.DEBUG_TAG, "Set new project for $projectName")
        MainActivity.pd.show ()
        projectJsonRef.child("$projectName.json")
                .getFile(tempProjectFile).addOnSuccessListener {
                    val jsonString: String = tempProjectFile.readText(Charsets.UTF_8)
                    entryArray = JSONArray(jsonString)

                    var indexLookupById = mutableMapOf<String, Int>()
                    for (i in 0 until entryArray.length()) {
                        val entryId = entryArray.getJSONObject(i).getString("id")
                        indexLookupById[entryId] = i
                    }

                    // FIXME: Nesting (setNew -> getClient -> getAdmin) here is pretty dumb
                    getClientProgress(projectName, indexLookupById)
                    updateToolbarTitle()
                }.addOnFailureListener {
                    // FIXME
                    // If failed what can be done?
                    MainActivity.pd.dismiss()
                }
    }

    private fun getClientProgress (projectName : String,
                                   lookup : MutableMap<String, Int>) {
        try {
            // Progress initialization for current project
            clientProgressLookup[projectName] = mutableListOf<Int>()
            db.collection("users").document(clientUid!!)
                    .get()
                    .addOnSuccessListener { result ->
                        try {
                            var sentIds = result.get(projectName) as MutableList<String>
                            var newData = sentIds.map{ lookup[it]!! }.toMutableList()
                            clientProgressLookup[projectName] = newData
                            circular_progress.setProgress(newData, COLOR_CLIENT_PROGRESS,
                                    currentIndex, currentProject["size"]!!.toInt())
                        } catch (e : Exception) {
                            Log.e(MainActivity.DEBUG_TAG, "client catch $e")
                        }
                        getAdminProgress(projectName, lookup)
                    }.addOnFailureListener {
                        // TODO: In what situation this part would be called
                        Log.e(MainActivity.DEBUG_TAG, "client failure $it")
                    }
        } catch (e: Exception) { }
    }

    private fun getAdminProgress(projectName: String,
                                 lookup : MutableMap<String, Int>) {
        try {
            // Progress initialization for current project
            adminProgressLookup[projectName] = mutableListOf<Int>()
            db.collection("projects").document(projectName)
                    .get()
                    .addOnSuccessListener { result ->
                        try {
                            //                                  Use ID instead of Index
                            var finishedIds = result.get("done") as MutableList<String>
                            var newData = finishedIds.map{ lookup[it]!! }.toMutableList()
                            adminProgressLookup[projectName] = newData
                            circular_progress.setProgress(newData, COLOR_ADMIN_PROGRESS,
                                    currentIndex, currentProject["size"]!!.toInt())

                            if (!isLoggedIn()) {
                                buildAuthDialog()
                            }
                        } catch (e : Exception) { }

                        updateScript()

                        // TODO: Try to do this logic in a more stable approach
                        circular_progress.initialize(currentIndex, currentProject["size"]!!.toInt())
                        MainActivity.pd.dismiss()
                    }.addOnFailureListener {
                        // TODO: In what situation this part would be called
                    }
        } catch (e: Exception) { }
    }

    private fun isLoggedIn() : Boolean {
        return mAuth.currentUser != null
    }

    private fun buildAuthDialog() {
        // setup the alert builder
        val builder = AlertDialog.Builder(mContext)
        builder.setTitle("Please sign in first")
        builder.setMessage("Sign-in is required in order to monetize your contributions " +
                "and/or promote yourself on our YouTube channel.")
        builder.setCancelable(false)
        // add a button
        builder.setPositiveButton("OK") { dialog, which ->
            val providers = arrayListOf(
                    AuthUI.IdpConfig.TwitterBuilder().build(),
                    AuthUI.IdpConfig.FacebookBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.EmailBuilder().build()
//                            AuthUI.IdpConfig.GitHubBuilder().build()
            )

            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setTheme(R.style.AppTheme_AppBarOverlay)
                            .setAvailableProviders(providers)
                            .setLogo(R.mipmap.ic_launcher_foreground)
                            .build(), MainActivity.RC_SIGN_IN)
        }
        builder.setNegativeButton("Skip", null)
        // create and show the alert dialog
        val dialog = builder.create()
        dialog.show()
    }

    /**
     * Update the main script
     */
    private fun updateScript() {
        val wantedKey = currentProject["wanted"]
        val upnKey = currentProject["upn"]
        val lonKey = currentProject["lon"]
        val newEntryIds = mutableListOf<String>()
        val newMainScripts = mutableListOf<String>()
        val newUpperNotes = mutableListOf<String>()
        val newLowerNotes = mutableListOf<String>()

        var nextIndex: Int

        for (i in 0 until entryArray.length()) {
            newMainScripts.add(entryArray.getJSONObject(i).getString(wantedKey))
            newEntryIds.add(entryArray.getJSONObject(i).getString("id"))
            if (upnKey != "") {
                newUpperNotes.add(entryArray.getJSONObject(i).getString(upnKey))
            }
            if (lonKey != "") {
                newLowerNotes.add(entryArray.getJSONObject(i).getString(lonKey))
            }
        }
        entryIds = newEntryIds
        mainScripts = newMainScripts
        upperNotes = newUpperNotes
        lowerNotes = newLowerNotes

        try {
            val unfinishedIndices =
                    (0 .. currentProject["size"]!!.toInt() - 1).filter { it !in clientProgressLookup[projectName]!! &&
                            it !in adminProgressLookup[projectName]!!}
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

        if (newIndex in adminProgressLookup[projectName]!!) {
            checkbox.visibility = View.VISIBLE
            checkbox.setImageResource(R.drawable.ic_done_24dp)
            checkbox_dummy.setImageResource(R.drawable.ic_done_24dp)
        }
        else if (newIndex in clientProgressLookup[projectName]!!) {
            checkbox.visibility = View.VISIBLE
            checkbox.setImageResource(R.drawable.ic_thanks_24dp)
            checkbox_dummy.setImageResource(R.drawable.ic_thanks_24dp)
        }
        else {
            checkbox.visibility = View.INVISIBLE
        }
        circular_progress.setCurrentIndex(currentIndex)
    }

    private fun updateClientProgress(){
        if (currentIndex !in clientProgressLookup[projectName]!!)
            clientProgressLookup[projectName]?.add(currentIndex)
        checkbox.visibility = View.VISIBLE
        // Explictly set bitmap source because initially not specified
        checkbox.setImageResource(R.drawable.ic_thanks_24dp)
        checkbox_dummy.setImageResource(R.drawable.ic_thanks_24dp)
        circular_progress.addToArcList(currentIndex, COLOR_CLIENT_PROGRESS)
        mActivity.cnt_voice.text = (Integer.parseInt(
                mActivity.cnt_voice.text.toString()) + 1).toString()
    }

    private fun updateToolbarTitle() {
        activity!!.toolbar_main.title = currentProject["flags"] + currentProject["title"]
    }

    // Build Table of Contents in a grid
    private fun buildToCGrid() {
        val gridView = layoutInflater.inflate(R.layout.grid_view, null) as GridView
        val titleView = layoutInflater.inflate(R.layout.grid_title, null)
        val builder = AlertDialog.Builder(mContext, R.style.GridDialog)

        try {
            GridBaseAdapter(
                    mContext, mainScripts, clientProgressLookup[projectName], adminProgressLookup[projectName]
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


    private fun buildCommentDialog() {
        val commentView = layoutInflater.inflate(R.layout.comment_view, null)
        val titleView = layoutInflater.inflate(R.layout.comment_title, null)
        val builder = AlertDialog.Builder(mContext, R.style.CommentDialog)
        val editText = commentView.findViewById<EditText>(R.id.comment_edittext)
        val forumLink = commentView.findViewById<TextView>(R.id.forum_link)
        val postBtn = commentView.findViewById<Button>(R.id.do_post)
        val cancelBtn = commentView.findViewById<Button>(R.id.cancel_post)
        val commentArrayRef = db.collection("forum/${entryIds[currentIndex]}/comment")

        try {
            commentArrayRef.get().addOnCompleteListener { commentArray ->
                builder.setView(commentView)
                builder.setCustomTitle(titleView)

                val commentSize = commentArray.result!!.size()

                val ad = builder.show()
                postBtn.setOnClickListener {
                    if (!editText.text.isEmpty()) {
                        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // Quoted "Z" to indicate UTC, no timezone offset
                        df.timeZone = TimeZone.getTimeZone("UTC")
                        val nowAsISO = df.format(Date())
                        var fullname = "Anonymous"
                        if (isLoggedIn()) {
                            fullname = mAuth.currentUser!!.displayName!!
                        }

                        val newComment = commentArrayRef.document()
                        // commentJSON for jquery-comment
                        val commentJSON = hashMapOf(
                                "id" to "c${commentSize + 1}",
                                "uid" to newComment.id,
                                "client_id" to clientUid,
                                "parent" to null,
                                "content" to editText.text.toString(),
                                "fullname" to fullname,
                                "created" to nowAsISO,
                                "modified" to nowAsISO,
                                "upvote_count" to 0,
                                "pings" to mutableMapOf<String, String>(),
                                "profile_picture_url" to "",
                                "user_has_upvoted" to false,
                                "created_by_current_user" to false,
                                "from_app" to true
                        )
                        newComment.set(commentJSON).addOnSuccessListener {
                            Toast.makeText(mContext,"Comment successfully posted.", Toast.LENGTH_SHORT).show()
                            ad.dismiss()
                        }.addOnFailureListener {
                            Toast.makeText(mContext,"Failed to post. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(mContext,"Write your comment in the text area", Toast.LENGTH_SHORT).show()
                    }
                }
                cancelBtn.setOnClickListener {
                    ad.cancel()
                }
                editText.hint = "Your comment on \"${main_text.text}\" (e.g. errors and tips for learners)"
                forumLink.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://joytan.pub/forum/?p=${currentProject["dirname"]}&eid=${entryIds[currentIndex]}")
                    startActivity(intent)
                }

            }
        } catch (e: Exception) {
            Log.e(MainActivity.DEBUG_TAG, e.toString())
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