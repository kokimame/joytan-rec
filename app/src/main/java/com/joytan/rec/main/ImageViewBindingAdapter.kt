package com.joytan.rec.main

//import android.databinding.BindingAdapter
import android.widget.ImageView
import androidx.databinding.BindingAdapter


/**
 * ImageViewに表示する画像を、image_src属性で設定するBindingAdapter
 */
@BindingAdapter("image_src")
fun setImageSrc(imageView: ImageView, resId: Int) {
    imageView.setImageResource(resId)
}
