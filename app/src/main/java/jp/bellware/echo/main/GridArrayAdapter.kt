package jp.bellware.echo.main

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import jp.bellware.echo.R

class GridArrayAdapter(context: Context,
                         private val layoutResource: Int,
                         private val values: List<Int>,
                         private val progressDB: MutableMap<String, MutableList<Int>>,
                         private val currentDirname: String) : ArrayAdapter<Int>(
        context, layoutResource, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view as TextView

        if(textView.text.toString().toInt() - 1 in progressDB[currentDirname]!!)
            textView.setBackgroundColor(ContextCompat.getColor(context, R.color.play))
        else
            textView.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_dark))

        return textView
    }
}