package com.joytan.rec.main

//import android.databinding.BindingAdapter
import android.widget.ImageView
import androidx.databinding.BindingAdapter

/**
 * A BindingAdapter to use image_src attribute for setting an image displayed in ImageView
 */
@BindingAdapter("image_src")
fun setImageSrc(imageView: ImageView, resId: Int) {
    imageView.setImageResource(resId)
}
