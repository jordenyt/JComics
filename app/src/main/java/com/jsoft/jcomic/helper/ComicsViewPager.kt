package com.jsoft.jcomic.helper

import android.content.Context
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

import com.jsoft.jcomic.FullscreenActivity

class ComicsViewPager : ViewPager {

    private var activity: FullscreenActivity? = null

    constructor(context: Context) : super(context)

    fun setActivity(activity: FullscreenActivity) {
        this.activity = activity
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    fun turnPrev() {
        if (this.currentItem == 0) {
            activity!!.episodeSwitch(-1)
        } else {
            activity!!.switchPageNum(this.currentItem - 1)
        }
    }

    fun turnNext() {
        val currentPageNum = this.currentItem
        this.currentItem = this.currentItem + 1

        if (currentPageNum == this.currentItem) {
            activity!!.episodeSwitch(1)
        } else {
            activity!!.switchPageNum(this.currentItem)
        }
    }

    fun showPageBar() {
        activity!!.showPageBar()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // Never allow swiping to switch between pages
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Never allow swiping to switch between pages
        return false
    }

    fun exitActivity() {
        activity!!.onBackPressed()
    }

}