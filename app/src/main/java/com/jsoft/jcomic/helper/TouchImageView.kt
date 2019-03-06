package com.jsoft.jcomic.helper

import android.content.Context
import android.graphics.Matrix
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView

class TouchImageView: AppCompatImageView {
    internal var matrix: Matrix = Matrix()

    // Remember some things for zooming
    private var minScale = 1f
    internal var maxScale = 3f
    private var m: FloatArray = FloatArray(9)

    internal var viewWidth: Int = 0
    internal var viewHeight: Int = 0
    internal var saveScale = 1f
    private var origWidth: Float = 0.toFloat()
    private var origHeight: Float = 0.toFloat()
    private var oldMeasuredHeight: Int = 0
    internal var pager: ComicsViewPager = ComicsViewPager(context)

    private var mScaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var mGestureDetector: GestureDetector = GestureDetector(context, GestureListener())


    constructor(context: Context) : super(context) {
        sharedConstructing(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        sharedConstructing(context)
    }

    fun setPager(viewPager: ComicsViewPager) {
        this.pager = viewPager
    }

    private fun sharedConstructing(context: Context) {
        super.setClickable(true)
        //mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        //mGestureDetector = GestureDetector(context, GestureListener())
        //matrix = Matrix()
        //m = FloatArray(9)
        imageMatrix = matrix
        scaleType = ImageView.ScaleType.MATRIX

        setOnTouchListener { v, event ->
            mScaleDetector.onTouchEvent(event)
            mGestureDetector.onTouchEvent(event)

            imageMatrix = matrix
            invalidate()
            true // indicate event was handled
        }
    }

    fun setMaxZoom(x: Float) {
        maxScale = x
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleImage(detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            return if (saveScale < maxScale) {
                scaleImage(2f, e.x, e.y)
                true
            } else {
                scaleImage(0.1f, e.x, e.y)
                true
            }
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (e.x > viewWidth * 0.8) {
                pager.turnNext()
                return true
            } else if (e.x < viewWidth * 0.2) {
                pager.turnPrev()
                return true
            }
            return false
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val ratio = 2.0
            val diffX = (e2.x - e1.x).toDouble()
            val diffY = (e2.y - e1.y).toDouble()
            if (saveScale == 1f) {
                if (diffY / Math.abs(diffX) < -ratio) {
                    //Fling Up
                    pager.exitActivity()
                } else if (diffY / Math.abs(diffX) > ratio) {
                    //Fling Down
                    pager.showPageBar()
                } else if (diffX / Math.abs(diffY) < -ratio) {
                    //Fling Left
                    pager.turnNext()
                } else if (diffX / Math.abs(diffY) > ratio) {
                    //Fling Right
                    pager.turnPrev()
                }
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            Log.e("jComics", "onLongPress")
            //return false;
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val fixTransX = getFixDragTrans(-distanceX, viewWidth.toFloat(),
                    origWidth * saveScale)
            val fixTransY = getFixDragTrans(-distanceY, viewHeight.toFloat(),
                    origHeight * saveScale)
            matrix.postTranslate(fixTransX, fixTransY)
            fixTrans()
            return true
        }
    }

    internal fun scaleImage(mScaleFactor: Float, focusX: Float, focusY: Float) {
        var mScaleFactor = mScaleFactor
        val origScale = saveScale
        saveScale *= mScaleFactor
        if (saveScale > maxScale) {
            saveScale = maxScale
            mScaleFactor = maxScale / origScale
        } else if (saveScale < minScale) {
            saveScale = minScale
            mScaleFactor = minScale / origScale
        }

        if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight)
            matrix.postScale(mScaleFactor, mScaleFactor, (viewWidth / 2).toFloat(),
                    (viewHeight / 2).toFloat())
        else
            matrix.postScale(mScaleFactor, mScaleFactor,
                    focusX, focusY)

        fixTrans()
    }

    internal fun fixTrans() {
        matrix.getValues(m)
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]

        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), origHeight * saveScale)

        if (fixTransX != 0f || fixTransY != 0f)
            matrix.postTranslate(fixTransX, fixTransY)
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float

        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }

        if (trans < minTrans)
            return -trans + minTrans
        return if (trans > maxTrans) -trans + maxTrans else 0f
    }

    internal fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0f
        } else delta
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = View.MeasureSpec.getSize(heightMeasureSpec)

        //
        // Rescales image on rotation
        //
        if (oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight
                || viewWidth == 0 || viewHeight == 0)
            return
        oldMeasuredHeight = viewHeight

        if (saveScale == 1f) {
            // Fit to screen.
            val scale: Float

            val drawable = drawable
            if (drawable == null || drawable.intrinsicWidth == 0
                    || drawable.intrinsicHeight == 0)
                return
            val bmWidth = drawable.intrinsicWidth
            val bmHeight = drawable.intrinsicHeight

            Log.d("bmSize", "bmWidth: $bmWidth bmHeight : $bmHeight")

            val scaleX = viewWidth.toFloat() / bmWidth.toFloat()
            val scaleY = viewHeight.toFloat() / bmHeight.toFloat()
            scale = Math.min(scaleX, scaleY)
            matrix.setScale(scale, scale)

            // Center the image
            var redundantYSpace = viewHeight.toFloat() - scale * bmHeight.toFloat()
            var redundantXSpace = viewWidth.toFloat() - scale * bmWidth.toFloat()
            redundantYSpace /= 2.toFloat()
            redundantXSpace /= 2.toFloat()

            matrix.postTranslate(redundantXSpace, redundantYSpace)

            origWidth = viewWidth - 2 * redundantXSpace
            origHeight = viewHeight - 2 * redundantYSpace
            imageMatrix = matrix
        }
        fixTrans()
    }
}
