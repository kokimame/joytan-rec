package com.joytan.rec.setting

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.joytan.rec.R

class ProjectArrayAdapter(context: Context,
                          private val resource: Int,
                          private val mList: List<String>,
                          var currentProjectIndex: Int)
    : ArrayAdapter<String>(context, resource, mList){

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val textView = super.getView(position, convertView, parent)
        if (position == currentProjectIndex) {
            textView.setBackgroundColor(context.resources.getColor(R.color.bg_darker))
        } else {
            textView.setBackgroundColor(Color.TRANSPARENT)
        }
        return textView
    }

    fun setCurrentIndex(index: Int) {
        currentProjectIndex = index
    }
}