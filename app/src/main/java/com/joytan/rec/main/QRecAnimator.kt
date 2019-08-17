package com.joytan.rec.main

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * メイン画面のアニメ隔離クラス
 */
class QRecAnimator {


    /**
     * 削除アニメーションを開始する。
     */
    fun startDeleteAnimation(view: View) {
        val dp = view.resources.displayMetrics.density
        val pvha = PropertyValuesHolder.ofFloat("alpha", view.alpha, 0f)
        val pvht = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 100f * dp)
        val animator = ObjectAnimator.ofPropertyValuesHolder(view, pvha, pvht)
        animator.duration = 200
        animator.start()
    }

    /**
     * フェードインを行う
     *
     * @param view
     */
    fun fadeIn(view: View) {
        view.visibility = View.VISIBLE
        view.translationY = 0f
        val a = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        a.duration = 500
        a.start()
    }

    fun fadeIn(view: View, direction: String) {
        view.visibility = View.VISIBLE
        view.translationY = 0f
        val dp = view.resources.displayMetrics.density
        val pvha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f)

        val moveX : Float
        if (direction == "left") {
            moveX = -150f
        } else {
            moveX = 150f
        }

        val pvht = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, moveX * dp, 0f)
        val a = ObjectAnimator.ofPropertyValuesHolder(view, pvha, pvht)
        a.duration = 500
        a.start()
    }

    fun fadeOut(view: View, direction: String) {
        view.visibility = View.VISIBLE
        view.translationY = 0f
        val dp = view.resources.displayMetrics.density
        val pvha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f)

        val moveX : Float
        if (direction == "left") {
            moveX = -150f
        } else {
            moveX = 150f
        }

        val pvht = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, -moveX * dp)
        val a = ObjectAnimator.ofPropertyValuesHolder(view, pvha, pvht)
        a.duration = 500
        a.start()
    }

}
