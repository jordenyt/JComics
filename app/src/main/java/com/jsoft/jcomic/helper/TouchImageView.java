package com.jsoft.jcomic.helper;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.support.v7.widget.AppCompatImageView;

public class TouchImageView extends AppCompatImageView {
    Matrix matrix;

    // Remember some things for zooming
    float minScale = 1f;
    float maxScale = 3f;
    float[] m;

    int viewWidth, viewHeight;
    float saveScale = 1f;
    protected float origWidth, origHeight;
    int oldMeasuredWidth, oldMeasuredHeight;
    ComicsViewPager pager;

    ScaleGestureDetector mScaleDetector;
    GestureDetector mGestureDetector;


    Context context;

    public TouchImageView(Context context) {
        super(context);
        sharedConstructing(context);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    public void setPager(ComicsViewPager viewPager) {
        this.pager = viewPager;
    }

    private void sharedConstructing(Context context) {
        super.setClickable(true);
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
        matrix = new Matrix();
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);

            setImageMatrix(matrix);
            invalidate();
            return true; // indicate event was handled
            }

        });
    }

    public void setMaxZoom(float x) {
        maxScale = x;
    }

    private class ScaleListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleImage(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (saveScale < maxScale) {
                scaleImage(2f, e.getX(), e.getY());
                return true;
            } else {
                scaleImage(0.1f, e.getX(), e.getY());
                return true;
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (e.getX() > viewWidth * 0.8) {
                pager.turnNext();
                return true;
            } else if (e.getX() < viewWidth * 0.2) {
                pager.turnPrev();
                return true;
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            double ratio = 2;
            double diffX = e2.getX() - e1.getX();
            double diffY = e2.getY() - e1.getY();
            if (saveScale == 1f) {
                if (diffY / Math.abs(diffX) < -ratio) {
                    //Fling Up
                    pager.exitActivity();
                } else if (diffY / Math.abs(diffX) > ratio) {
                    //Fling Down
                    pager.showPageBar();
                } else if (diffX / Math.abs(diffY) < -ratio) {
                    //Fling Left
                    pager.turnNext();
                } else if (diffX / Math.abs(diffY) > ratio) {
                    //Fling Right
                    pager.turnPrev();
                }
            }
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.e("jComics", "onLongPress");
            //return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float fixTransX = getFixDragTrans(-distanceX, viewWidth,
                    origWidth * saveScale);
            float fixTransY = getFixDragTrans(-distanceY, viewHeight,
                    origHeight * saveScale);
            matrix.postTranslate(fixTransX, fixTransY);
            fixTrans();
            return true;
        }
    }

    void scaleImage(float mScaleFactor, float focusX, float focusY) {
        float origScale = saveScale;
        saveScale *= mScaleFactor;
        if (saveScale > maxScale) {
            saveScale = maxScale;
            mScaleFactor = maxScale / origScale;
        } else if (saveScale < minScale) {
            saveScale = minScale;
            mScaleFactor = minScale / origScale;
        }

        if (origWidth * saveScale <= viewWidth
                || origHeight * saveScale <= viewHeight)
            matrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2,
                    viewHeight / 2);
        else
            matrix.postScale(mScaleFactor, mScaleFactor,
                    focusX, focusY);

        fixTrans();
    }

    void fixTrans() {
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];

        float fixTransX = getFixTrans(transX, viewWidth, origWidth * saveScale);
        float fixTransY = getFixTrans(transY, viewHeight, origHeight * saveScale);

        if (fixTransX != 0 || fixTransY != 0)
            matrix.postTranslate(fixTransX, fixTransY);
    }

    float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;

        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans)
            return -trans + minTrans;
        if (trans > maxTrans)
            return -trans + maxTrans;
        return 0;
    }

    float getFixDragTrans(float delta, float viewSize, float contentSize) {
        if (contentSize <= viewSize) {
            return 0;
        }
        return delta;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        //
        // Rescales image on rotation
        //
        if (oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight
                || viewWidth == 0 || viewHeight == 0)
            return;
        oldMeasuredHeight = viewHeight;
        oldMeasuredWidth = viewWidth;

        if (saveScale == 1) {
            // Fit to screen.
            float scale;

            Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() == 0
                    || drawable.getIntrinsicHeight() == 0)
                return;
            int bmWidth = drawable.getIntrinsicWidth();
            int bmHeight = drawable.getIntrinsicHeight();

            Log.d("bmSize", "bmWidth: " + bmWidth + " bmHeight : " + bmHeight);

            float scaleX = (float) viewWidth / (float) bmWidth;
            float scaleY = (float) viewHeight / (float) bmHeight;
            scale = Math.min(scaleX, scaleY);
            matrix.setScale(scale, scale);

            // Center the image
            float redundantYSpace = (float) viewHeight
                    - (scale * (float) bmHeight);
            float redundantXSpace = (float) viewWidth
                    - (scale * (float) bmWidth);
            redundantYSpace /= (float) 2;
            redundantXSpace /= (float) 2;

            matrix.postTranslate(redundantXSpace, redundantYSpace);

            origWidth = viewWidth - 2 * redundantXSpace;
            origHeight = viewHeight - 2 * redundantYSpace;
            setImageMatrix(matrix);
        }
        fixTrans();
    }
}
