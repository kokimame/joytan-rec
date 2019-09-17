package com.joytan.rec.main

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.joytan.rec.R

class CycleAdapter(private val mainScripts: MutableList<String>) :
        RecyclerView.Adapter<CycleAdapter.MyViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    class MyViewHolder(val scriptLayout: LinearLayout) : RecyclerView.ViewHolder(scriptLayout)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        // create a new view
        val scriptLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.main_script, parent, false) as LinearLayout
        // set the view's size, margins, paddings and layout parameters

        Log.i(MainActivity.INFO_TAG, "onCreateViewHolder")
        return MyViewHolder(scriptLayout)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val checkbox = holder.scriptLayout.getChildAt(0) as ImageView
        val mainText = holder.scriptLayout.getChildAt(1) as TextView

        mainText.text = mainScripts[position]
        Log.i(MainActivity.INFO_TAG, "onBindViewHolder" + position.toString())
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = mainScripts.size
}