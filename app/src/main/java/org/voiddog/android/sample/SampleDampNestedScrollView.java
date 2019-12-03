package org.voiddog.android.sample;

import android.content.Context;
import android.os.Build;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringFlingAnimation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * ┏┛ ┻━━━━━┛ ┻┓
 * ┃　　　　　　 ┃
 * ┃　　　━　　　┃
 * ┃　┳┛　  ┗┳　┃
 * ┃　　　　　　 ┃
 * ┃　　　┻　　　┃
 * ┃　　　　　　 ┃
 * ┗━┓　　　┏━━━┛
 * * ┃　　　┃   神兽保佑
 * * ┃　　　┃   代码无BUG！
 * * ┃　　　┗━━━━━━━━━┓
 * * ┃　　　　　　　    ┣┓
 * * ┃　　　　         ┏┛
 * * ┗━┓ ┓ ┏━━━┳ ┓ ┏━┛
 * * * ┃ ┫ ┫   ┃ ┫ ┫
 * * * ┗━┻━┛   ┗━┻━┛
 *
 * @author qigengxin
 * @since 2018-05-01 12:10
 */
public class SampleDampNestedScrollView extends FrameLayout{

    // 手势帮助类
    private GestureDetector gestureDetector;
    // 动画
    private SpringFlingAnimation animation;
    // 偏移位置提供
    private FloatValueHolder scrollValueHolder;

    public SampleDampNestedScrollView(@NonNull Context context) {
        super(context);
        init();
    }

    public SampleDampNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SampleDampNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public void addView(View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index, params);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            return;
        }

        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            final int widthPadding;
            final int heightPadding;
            final int targetSdkVersion = getContext().getApplicationInfo().targetSdkVersion;
            final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                widthPadding = getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin;
                heightPadding = getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin;
            } else {
                widthPadding = getPaddingLeft() + getPaddingRight();
                heightPadding = getPaddingTop() + getPaddingBottom();
            }

            final int desiredHeight = getMeasuredHeight() - heightPadding;
            if (child.getMeasuredHeight() < desiredHeight) {
                final int childWidthMeasureSpec = getChildMeasureSpec(
                        widthMeasureSpec, widthPadding, lp.width);
                final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        desiredHeight, MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        // 强行让儿子 wrap content
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int usedTotal = getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin +
                heightUsed;
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - usedTotal),
                MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                // 检测到手指落下，结束动画
                stopAnim();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 检测到手指抬起，开始动画
                animation.start();
                break;
        }
        return gestureDetector.onTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                // 检测到手指落下，停止动画
                stopAnim();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 检测到手指抬起，开始动画
                animation.start();
                break;
        }
        super.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), gestureListener);
        gestureDetector.setIsLongpressEnabled(false);
        scrollValueHolder = new FloatValueHolder();
        animation = new SpringFlingAnimation(scrollValueHolder);
        animation.setRangeValueHolder(rangeValueHolder);
        animation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                //动画更新，更新 offset
                updateScroll();
            }
        });
    }

    private void stopAnim() {
        animation.cancel();
        animation.setStartVelocity(0);
    }

    private void updateScroll() {
        // 根据 offset 滚动到指定位置
        scrollTo(0, (int) scrollValueHolder.getValue());
    }

    /**
     * 移动范围
     */
    private SpringFlingAnimation.FloatRangeValueHolder rangeValueHolder = new SpringFlingAnimation.FloatRangeValueHolder() {
        @Override
        public float getMinRange() {
            return 0;
        }

        @Override
        public float getMaxRange() {
            if (getChildCount() == 0) {
                return 0;
            }
            int ret = getChildAt(0).getBottom() - getHeight();
            return ret < 0 ? 0 : ret;
        }
    };

    /**
     * 手势监听器
     */
    private GestureDetector.OnGestureListener gestureListener = new GestureDetector.OnGestureListener() {

        boolean isDown = false;

        @Override
        public boolean onDown(MotionEvent e) {
            isDown = true;
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {}

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // 这是为了第一次 move 的时候，distanceY 的突变置为 0
            if (isDown) {
                isDown = false;
                return true;
            }
            float min = rangeValueHolder.getMinRange(), max = rangeValueHolder.getMaxRange();
            float scrollY = scrollValueHolder.getValue();
            float ratio = 1;
            if (scrollY < min) {
                ratio *= (1 - (min - scrollY) / getHeight())/2;
            } else if (scrollY > max) {
                ratio *= (1 - (scrollY - max) / getHeight())/2;
            }
            if (ratio < 0) {
                ratio = 0;
            }
            distanceY *= ratio;
            scrollValueHolder.setValue(scrollValueHolder.getValue() + distanceY);
            updateScroll();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {}

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            animation.setStartVelocity(-velocityY);
            return true;
        }
    };
}
