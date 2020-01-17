package com.joytan.rec.main.view

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.facebook.internal.Mutable
import com.joytan.rec.R
import com.joytan.rec.main.MainActivity


/**
 * View for circular progress in the main fragment
 */
class CircularProgress : View {

    private var viewWidth = 0f

    private var viewHeight = 0f

    private var centerX = 0f

    private var centerY = 0f

    private var arcList = mutableListOf<Pair<Pair<RectF, Int>, Float>>()

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    private var totalSize = 0

    private var radius = 0f

    private var currentIndex = 0

    private val normalPaint = Paint()

    private var blurredPaint = Paint()

    private var thickPaint = Paint()

    private var indexPaint = Paint()

    private var baseRectF = RectF()

    private var sweepAngle = 0f

    override fun onDraw(canvas: Canvas) {
        normalPaint.color = ContextCompat.getColor(context, R.color.bg_darkest)
        canvas.drawCircle(centerX, centerY, radius, normalPaint)

        val currentAngle = -90 + sweepAngle * currentIndex
        // To be overdrawn if the current index is in progress
        canvas.drawArc(baseRectF, currentAngle + sweepAngle / 4, sweepAngle / 2, false, indexPaint)

        arcList.forEach {
            val (shape, startAngle) = it
            val (rectF, col) = shape
            normalPaint.color = col
            thickPaint.color = col
            if (startAngle == currentAngle) {
                canvas.drawArc(rectF, startAngle + sweepAngle / 4, sweepAngle / 2, false, thickPaint)
            } else {
                canvas.drawArc(rectF, startAngle, sweepAngle, false, normalPaint)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        this.viewWidth = w.toFloat()
        this.viewHeight = h.toFloat()
    }

    fun addToArcList(index : Int, color : Int) {
        sweepAngle = 360f / totalSize
        val startAngle = -90 + sweepAngle * index

        arcList.add(Pair(Pair(baseRectF, color), startAngle))
        invalidate()
    }

    fun setCurrentIndex(currentIndex : Int) {
        this.currentIndex = currentIndex
        val dp = context.resources.displayMetrics.density

        indexPaint.isAntiAlias = true
        indexPaint.style = Paint.Style.STROKE
        indexPaint.strokeWidth = 25 * dp
        indexPaint.color = ContextCompat.getColor(context, R.color.primary_dark)
        indexPaint.isDither = true

        invalidate()
    }

    fun initialize(index : Int, totalSize: Int) {
        val dp = context.resources.displayMetrics.density
        this.currentIndex = index
        this.totalSize = totalSize

        radius = 150f * dp
        centerX = width / 2f
        centerY = height / 2f

        normalPaint.isAntiAlias = true
        normalPaint.style = Paint.Style.STROKE
        normalPaint.strokeWidth = 2 * dp
        normalPaint.isDither = true

        thickPaint.isAntiAlias = true
        thickPaint.style = Paint.Style.STROKE
        thickPaint.strokeWidth = 25 * dp
        thickPaint.color = ContextCompat.getColor(context, R.color.primary_dark)
        thickPaint.isDither = true

        baseRectF = RectF(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius)

        sweepAngle = 360f / totalSize
    }

    fun setProgress(progress : MutableList<Int>, color : Int, index : Int, totalSize : Int) {

        initialize(index, totalSize)

        for (i in progress) {
            val startAngle = -90 + sweepAngle * i
            arcList.add(Pair(Pair(baseRectF, color), startAngle))
        }

        invalidate()
    }

    /**
     * Start animation while recording
     */
    fun startRecordAnimation() {
        val dp = context.resources.displayMetrics.density
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横画面
            centerX = (32 + 64) * dp
            centerY = viewHeight / 2
        } else {
            //縦画面
            centerX = viewWidth / 2
            centerY = (viewHeight - 104 * dp) / 2
        }
        //最大半径
        val radius = (Math.sqrt((centerY * centerY + centerX * centerX).toDouble()) * 1.2f).toFloat()
        //大きくしつつ透明にする
//        val pvhR = PropertyValuesHolder.ofFloat("radius", 0f, radius)
        val pvhA = PropertyValuesHolder.ofFloat("alpha", 0.5f, 0f)
//        val animator = ObjectAnimator.ofPropertyValuesHolder(this, pvhA, pvhR)
//        animator.duration = 500
//        animator.interpolator = LinearInterpolator()
//        animator.start()
    }

}
