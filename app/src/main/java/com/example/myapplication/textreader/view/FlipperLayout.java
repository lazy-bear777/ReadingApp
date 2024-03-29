package com.example.myapplication.textreader.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

public class FlipperLayout extends ViewGroup {

    private int index = 1;
    private static final String TAG = "FlipperLayout";

    /**
     * 弹性滑动对象，实现过渡效果的滑动
     */
    private Scroller mScroller;

    private VelocityTracker mVelocityTracker;

    private int mVelocityValue = 0;

    /**
     * 商定这个滑动是否有效的距离
     */
    private int limitDistance = 0;

    private int screenWidth = 0;

    /**
     * 手指移动的方向
     */
    private static final int MOVE_TO_LEFT = 0;
    private static final int MOVE_TO_RIGHT = 1;
    private static final int MOVE_NO_RESULT = 2;

    /**
     * 最后触摸的结果方向
     */
    private int mTouchResult = MOVE_NO_RESULT;
    /**
     * 一开始的方向
     */
    private int mDirection = MOVE_NO_RESULT;

    /**
     * 触摸的模式
     */
    private static final int MODE_NONE = 0;
    private static final int MODE_MOVE = 1;

    private int mMode = MODE_NONE;

    /**
     * 滑动的view
     */
    private View mScrollerView = null;

    /**
     * 最上层的view（处于边缘的，看不到的）
     */
    private View currentTopView = null;

    /**
     * 显示的view，显示在屏幕
     */
    private View currentShowView = null;

    /**
     * 最底层的view（看不到的）
     */
    private View currentBottomView = null;

    public FlipperLayout(Context context) {
        super(context);
        init(context);
    }

    public FlipperLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public FlipperLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mScroller = new Scroller(context);
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        //limitDistance = screenWidth / 3;
    }

    /***
     * @param listener
     * @param currentBottomView 最底层的view，初始状态看不到
     * @param currentShowView   正在显示的View
     * @param currentTopView    最上层的View，初始化时滑出屏幕
     */
    public void initFlipperViews(TouchListener listener, View currentBottomView, View currentShowView, View currentTopView) {
        this.currentBottomView = currentBottomView;
        this.currentShowView = currentShowView;
        this.currentTopView = currentTopView;
        setTouchResultListener(listener);
        addView(currentBottomView);
        addView(currentShowView);
        addView(currentTopView);
        /** 默认将最上层的view滑动到边缘（用于查看上一页） */
        currentTopView.scrollTo(-screenWidth, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int height = child.getMeasuredHeight();
            int width = child.getMeasuredWidth();
            child.layout(0, 0, width, height);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private int startX = 0;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    break;
                }
                startX = (int) ev.getX();
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //在View的onTouchEvent方法中追踪当前单击事件的速度
        obtainVelocityTracker(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:


                if (!mScroller.isFinished()) {
                    return super.onTouchEvent(event);
                }

                if (startX == 0) {
                    startX = (int) event.getX();
                }

                //确定滑动方向
                final int distance = startX - (int) event.getX();
                if (mDirection == MOVE_NO_RESULT) {
                    if (mListener.whetherHasNextPage() && distance > 0) { //左滑
                        mDirection = MOVE_TO_LEFT;
//                    } else if (mListener.whetherHasPreviousPage() && distance < 0) { //右滑
//                        mDirection = MOVE_TO_RIGHT;
//                    }
                    } else if (index > 1 && distance < 0) { //右滑
                        mDirection = MOVE_TO_RIGHT;
                    }

                }

                //是否停止滑动判断
                //当暂停时如果没有上一页或者下一页时停止
//                if (mMode == MODE_NONE
//                        && ((mDirection == MOVE_TO_LEFT && mListener.whetherHasNextPage()) || (mDirection == MOVE_TO_RIGHT && mListener
//                        .whetherHasPreviousPage()))) {
//                    mMode = MODE_MOVE;
//                }

                if (mMode == MODE_NONE
                        && ((mDirection == MOVE_TO_LEFT && mListener.whetherHasNextPage()) || (mDirection == MOVE_TO_RIGHT
                        && index > 1))) {
                    mMode = MODE_MOVE;
                }

                if (mMode == MODE_MOVE &&
                        (mDirection == MOVE_TO_LEFT && distance <= 0)) {
                        mMode = MODE_NONE;
                }

                if (mDirection != MOVE_NO_RESULT) {
                    //确定滑动的view
                    if (mDirection == MOVE_TO_LEFT) {
                        if (mScrollerView != currentShowView) {
                            mScrollerView = currentShowView;
                        }
                    } else {
                        if (mScrollerView != currentTopView) {
                            mScrollerView = currentTopView;
                        }
                    }

                    if (mMode == MODE_MOVE) {
                        //计算当前的滑动速度 时间间隔1000ms
                        mVelocityTracker.computeCurrentVelocity(1000, ViewConfiguration.getMaximumFlingVelocity());

                        if (mDirection == MOVE_TO_LEFT) {
                            mScrollerView.scrollTo(distance, 0);
                        } else {
                            mScrollerView.scrollTo(screenWidth + distance - startX, 0);
                        }
                    } else {
                        final int scrollX = mScrollerView.getScrollX();

                        if (mDirection == MOVE_TO_LEFT && scrollX != 0 && mListener.whetherHasNextPage()) {
                            mScrollerView.scrollTo(0, 0);
                        } else if (mDirection == MOVE_TO_RIGHT && index > 1 && screenWidth != Math.abs(scrollX)) {
                            mScrollerView.scrollTo(-screenWidth, 0);
                        }
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                if (mScrollerView == null) {
                    return super.onTouchEvent(event);
                }

                //左正 右负
                final int scrollX = mScrollerView.getScrollX();

                //获取当前的速度
                mVelocityValue = (int) mVelocityTracker.getXVelocity();

                int time = 500;

                if (mMode == MODE_MOVE && mDirection == MOVE_TO_LEFT) {
                    if (scrollX > limitDistance || mVelocityValue < -time) {
                        // 手指向左移动，可以翻屏幕
                        mTouchResult = MOVE_TO_LEFT;
                        if (mVelocityValue < -time) {
                            time = 200;
                        }
                        mScroller.startScroll(scrollX, 0, screenWidth - scrollX, 0, time);
                    } else {
                        mTouchResult = MOVE_NO_RESULT;
                        mScroller.startScroll(scrollX, 0, -scrollX, 0, time);
                    }
                } else if (mMode == MODE_MOVE && mDirection == MOVE_TO_RIGHT) {
                    if ((screenWidth - scrollX) > limitDistance || mVelocityValue > time) {
                        // 手指向右移动，可以翻屏幕
                        mTouchResult = MOVE_TO_RIGHT;
                        if (mVelocityValue > time) {
                            time = 250;
                        }
                        mScroller.startScroll(scrollX, 0, -scrollX, 0, time);
                    } else {
                        mTouchResult = MOVE_NO_RESULT;
                        mScroller.startScroll(scrollX, 0, screenWidth - scrollX, 0, time);
                    }
                }
                resetVariables();
                postInvalidate();
                break;
        }
        return true;
    }

    private void resetVariables() {
        mDirection = MOVE_NO_RESULT;
        mMode = MODE_NONE;
        startX = 0;
        releaseVelocityTracker();
    }

    private TouchListener mListener;

    private void setTouchResultListener(TouchListener listener) {
        this.mListener = listener;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            mScrollerView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        } else if (mScroller.isFinished() && mListener != null && mTouchResult != MOVE_NO_RESULT) {

            if (mTouchResult == MOVE_TO_LEFT) { //下一页

                index ++;

                if (currentTopView != null) {
                    removeView(currentTopView);
                }
                currentTopView = mScrollerView;
                currentShowView = currentBottomView;

                if (mListener.currentIsLastPage()) { //当前页不是最后一页
                    final View newView = mListener.createView(mTouchResult, index);
                    currentBottomView = newView;
                    addView(newView, 0);
                } else {  //当前页是最后一页
                    currentBottomView = new View(getContext());
                    currentBottomView.setVisibility(View.GONE);
                    addView(currentBottomView, 0);
                }
            } else { //上一页
                if(index > 1)
                    index --;

                if (currentBottomView != null) {
                    removeView(currentBottomView);
                }
                currentBottomView = currentShowView;
                currentShowView = mScrollerView;

                //if (mListener.currentIsFirstPage()) {
                if (index == 1) {
                    currentTopView = new View(getContext());
                    currentTopView.scrollTo(-screenWidth, 0);
                    currentTopView.setVisibility(View.GONE);
                    addView(currentTopView);
                } else {
                    currentTopView = mListener.createView(mTouchResult, index);
                    currentTopView.scrollTo(-screenWidth, 0);
                    addView(currentTopView);
                }
            }

            Log.d(TAG, "index:" + index);
            mTouchResult = MOVE_NO_RESULT;
        }
    }


    private void obtainVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /***
     * 用来实时回调触摸事件回调
     *
     * @author freeson
     */
    public interface TouchListener {

        /**
         * 手指向左滑动，即查看下一章节
         */
        int MOVE_TO_LEFT = 0;


        /**
         * 创建一个承载Text的View
         *
         * @param direction 滑动方向
         * @param index 新建页的索引
         * @return
         */
        View createView(final int direction, int index);


        /***
         * 当前页是否是最后一页
         *
         * @return
         */
        boolean currentIsLastPage();

        /***
         * 当前页是否有下一页（用来判断可滑动性）
         *
         * @return
         */
        boolean whetherHasNextPage();
    }

}
