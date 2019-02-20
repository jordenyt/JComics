package com.jsoft.jcomic.helper;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.jsoft.jcomic.FullscreenActivity;

public class ComicsViewPager extends ViewPager {

    private FullscreenActivity activity;

    public ComicsViewPager(Context context) {
        super(context);
    }

    public void setActivity(FullscreenActivity activity) {
        this.activity = activity;
    }

    public ComicsViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void turnPrev() {
        if (this.getCurrentItem() == 0) {
            activity.episodeSwitch(-1);
        } else {
            activity.switchPageNum(this.getCurrentItem() - 1);
        }
    }

    public void turnNext() {
        int currentPageNum = this.getCurrentItem();
        this.setCurrentItem(this.getCurrentItem() + 1);

        if (currentPageNum == this.getCurrentItem()) {
            activity.episodeSwitch(1);
        } else {
            activity.switchPageNum(this.getCurrentItem());
        }
    }

    public void showPageBar() {
        activity.showPageBar();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }

    public void exitActivity() {
        activity.onBackPressed();
    }
}