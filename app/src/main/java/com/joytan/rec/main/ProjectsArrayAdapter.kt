package com.joytan.rec.main

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.joytan.rec.R

class ProjectsArrayAdapter(context: Context,
                           private val resource: Int,
                           private val mList: List<String>,
                           private var currentProjectsIndex: Int)
    : ArrayAdapter<String>(context, resource, mList){


    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val textView: View

        if (position == currentProjectsIndex) {
//            textView = TextView(context)
            textView = View(context)
            textView.visibility = View.GONE
        } else {
            textView = super.getDropDownView(position, null, parent) as TextView
        }

        return textView
    }

    fun setCurrentIndex(index: Int) {
        currentProjectsIndex = index
    }
}