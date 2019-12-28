package com.joytan.rec.setting

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore

import com.joytan.rec.R
import com.joytan.rec.analytics.AnalyticsHandler
import com.joytan.rec.databinding.FragmentProjectBinding
import com.joytan.rec.main.MainActivity
import com.joytan.rec.main.MainFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_project.*

class ProjectFragment : Fragment() {

    private lateinit var mContext: Context
    private lateinit var padapter : ProjectArrayAdapter

    // Create a Firestore database reference from our app
    private val db = FirebaseFirestore.getInstance()
    private val ah = AnalyticsHandler()
    private val projectLookup = mutableListOf<Map<String, *>>()
    private var currentProjectIndex = 0

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentProjectBinding>(
                inflater, R.layout.fragment_project, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Analytics
        ah.onCreate(activity!!)

        db.collection("project_info")
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
                    Log.e(MainActivity.INFO_TAG, "Error getting documents ... ", exception)
                }

        btn_new_project.setOnClickListener {
            val newProject = projectLookup[padapter.currentProjectIndex] as Map<String, String>
            MainFragment.currentProject = newProject
//            findNavController().navigate(R.id.action_nav_project_to_nav_main)
            findNavController().popBackStack()
        }
        activity!!.drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    private fun setupSpinner(projectsList: List<String>) {
        padapter = ProjectArrayAdapter(
                mContext, R.layout.spinner_item, projectsList, currentProjectIndex
        ).also {
            adapter ->
            // Apply the adapter to the spinner
            project_list.adapter = adapter
        }

        project_list.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                padapter.setCurrentIndex(position)
                Log.e(MainActivity.INFO_TAG, projectLookup[position].toString() )
                padapter.notifyDataSetChanged()
            }
        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

}