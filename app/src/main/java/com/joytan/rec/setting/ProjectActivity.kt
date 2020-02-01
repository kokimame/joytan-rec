package com.joytan.rec.setting

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

import com.joytan.rec.R
import com.joytan.rec.handler.AnalyticsHandler
import com.joytan.rec.main.MainActivity
import com.joytan.rec.main.MainFragment
import kotlinx.android.synthetic.main.activity_project.*
import android.R.id.edit
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class ProjectActivity : AppCompatActivity() {

    private lateinit var padapter : ProjectArrayAdapter

    // Create a Firestore database reference from our app
    private val db = FirebaseFirestore.getInstance()
    private val ah = AnalyticsHandler()
    private val projectLookup = mutableListOf<Map<String, *>>()
    private var currentProjectIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        setSupportActionBar(toolbar_project)
        val lsab = supportActionBar
        lsab?.setHomeButtonEnabled(true)
        lsab?.setDisplayHomeAsUpEnabled(true)

        //Analytics
        ah.onCreate(this)

        db.collection("projects")
                .whereEqualTo("state", "open")
                .get()
                .addOnSuccessListener { pInfo ->
                    val projectTitles = mutableListOf<String>()
                    for (proj in pInfo) {
                        val description = proj.data["flags"] as String +
                                proj.data["title"] as String
                        projectLookup.add(proj.data)
                        projectTitles.add(description)
                    }
                    setupSpinner(projectTitles)
                }
                .addOnFailureListener { exception ->
                    Log.e(MainActivity.DEBUG_TAG, "Error getting documents ... ", exception)
                }

        btn_new_project.setOnClickListener {
            val newProject = projectLookup[padapter.currentProjectIndex] as Map<String, String>
            MainFragment.currentProject = newProject
            val sharedPref = applicationContext.getSharedPreferences(
                    getString(R.string.saved_user_data), 0)

            with (sharedPref.edit()) {
                Log.e(MainActivity.DEBUG_TAG, "put string to sharedPref ${newProject["dirname"]}")
                putString(getString(R.string.last_project_name), newProject["dirname"])
                commit()
            }

            setResult(MainFragment.PROJECT_STARTUP_CODE)
            finish()
        }
    }

    private fun setupSpinner(projectsList: List<String>) {
        padapter = ProjectArrayAdapter(
                this, R.layout.spinner_item, projectsList, currentProjectIndex
        ).also {
            adapter ->
            // Apply the adapter to the spinner
            project_list.adapter = adapter
        }

        project_list.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                padapter.setCurrentIndex(position)
                Log.e(MainActivity.DEBUG_TAG, projectLookup[position].toString() )
                padapter.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ah.onResume()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (MainFragment.currentProject.isEmpty()) {
            Toast.makeText(this, "Choose a project", Toast.LENGTH_SHORT).show()
            return false
        }
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return false
    }
}