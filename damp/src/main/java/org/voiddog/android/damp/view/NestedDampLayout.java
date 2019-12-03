package org.voiddog.android.damp.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringFlingAnimation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ListViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import org.voiddog.android.damp.util.DampViewUtil;

import java.util.ArrayList;
import java.util.List;

import static androidx.customview.widget.ViewDragHelper.INVALID_POINTER;

public class NestedDampLayout extends FrameLayout implements NestedScrollingChild2, NestedScrollingParent2{
    public static final String TAG = "NestedDampLayout";
    // 启用头部回弹
    public static final int DAMP_FLAG_START = 1;
    // 启用底部回弹
    public static final int DAMP_FLAG_END = 2;
    // 启用两个方向的回弹，目前无效，有bug
    @Deprecated
    public static final int DAMP_FLAG_BOTH = 3;

    /**
     * get the offset change event
     */
    public interface OffsetChangeListener {
        /**
         * notify when the offset of children is change
         * @param layout
         * @param oldOffset
         * @param newOffset
         */
        void onOffsetChange(NestedDampLayout layout, int oldOffset, int newOffset);
    }

    /**
     * tell nested damp layout that if target scroll child can
     * scroll up or scroll down
     */
    public interface OnChildScrollCallback {
        /**
         * judge if the current scroll child can scroll up
         * @param parent the callback emitter
         * @param targetChild the target that parent found, you may not use this
         * @return true if current scroll child can scroll up
         */
        boolean canChildScrollUp(NestedDampLayout parent, @Nullable View targetChild);

        /**
         * judge if the current scroll child can scroll down
         * @param parent the callback emitter
         * @param targetChild the target that parent found, you may not use this
         * @return true if current scroll child can scroll down
         */
        boolean canChildScrollDown(NestedDampLayout parent, @Nullable View targetChild);
    }

    public NestedDampLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public NestedDampLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NestedDampLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public NestedDampLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /****************************************************************
     *                   Nested Child Dispatcher
     ****************************************************************/
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        childHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return childHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return childHelper.startNestedScroll(axes);
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return childHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        childHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return childHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return childHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow) {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow) {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }


    /****************************************************************
     *                     Nested Parent Receiver
     ****************************************************************/

    @Override
    public int getNestedScrollAxes() {
        return parentHelper.getNestedScrollAxes();
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return (axes&ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
        targetChild = child;
        targetView = target;
        downPointInTargetChild = true;
        if (type == ViewCompat.TYPE_TOUCH) {
            nestedScrollInProgress = true;
        }
        if (nestedVelocityTracker != null) {
            nestedVelocityTracker.recycle();
            nestedVelocityTracker = null;
        }
        recordNestedDown();
        parentHelper.onNestedScrollAccepted(child, target, axes, type);
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
        if (!isDragged) {
            final float offset = getOffset();
            final float minFlingOffset = getMinFlingOffset();
            final float maxFlingOffset = getMaxFlingOffset();
            if (dy > 0 && (dampFlag&DAMP_FLAG_START)==DAMP_FLAG_START) {
                // scroll down
                if (offset > minFlingOffset) {
                    // head over scroll
                    consumed[1] = scroll(dy);
                }
            } else if (dy < 0 && (dampFlag&DAMP_FLAG_END)==DAMP_FLAG_END){
                // scroll up
                if (offset < maxFlingOffset) {
                    // bottom over scroll
                    consumed[1] = scroll(dy);
                }
            }
        }
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        if (!isDragged) {
            // scroll up
            int consumedScroll = 0;
            final float offset = getOffset();
            final float minFlingOffset = getMinFlingOffset();
            final float maxFlingOffset = getMaxFlingOffset();
            if (dyUnconsumed != 0) {
                consumedScroll = scroll(dyUnconsumed);
            }
            recordNestedDy(dyConsumed + consumedScroll);

            if (type == ViewCompat.TYPE_NON_TOUCH) {
                int dy = dyUnconsumed + dyConsumed;
                if (offset < minFlingOffset) {
                    if (!animation.isRunning()) {
                        applyNestedVelocity();
                        playAnim();
                    }
                } else if (offset > maxFlingOffset) {
                    if (!animation.isRunning()) {
                        applyNestedVelocity();
                        playAnim();
                    }
                }
                // 到底了，而且有未消耗滚动，表示无法继续往下滚了
                if (offset <= minFlingOffset && dy > 0 && dyUnconsumed > 0) {
                    DampViewUtil.stopScroll(target);
                } else if (offset >= maxFlingOffset && dy < 0 && dyUnconsumed < 0) {
                    DampViewUtil.stopScroll(target);
                }
            }
        }
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            applyNestedVelocity();
            playAnim();
            if (nestedVelocityTracker != null) {
                nestedVelocityTracker.recycle();
                nestedVelocityTracker = null;
            }
        } else if (type == ViewCompat.TYPE_TOUCH) {
            nestedScrollInProgress = false;
            playAnim();
        }
        parentHelper.onStopNestedScroll(target, type);
        stopNestedScroll(type);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // apply the views layout when layout change, because the frameLayout will
        // reset the layout of children
        childOffset = 0;
        applyOffsetToView();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTargetChild();

        if (!isEnabled()) {
            return false;
        }

        // ensure touch slop
        if (touchSlop < 0) {
            // use zero as slop
            touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }

        final int action = ev.getAction();

        // Shortcut since we're begin dragged
        if (action == MotionEvent.ACTION_MOVE && isDragged) {
            return true;
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                isDragged = false;
                final int x = (int) (ev.getX() + 0.5f);
                final int y = (int) (ev.getY() + 0.5f);
                if (targetChild != null && isPointInChildBounds(targetChild, x, y)) {
                    downPointInTargetChild = true;
                } else {
                    downPointInTargetChild = false;
                }

                lastMotionY = y;
                lastMotionX = x;
                activePointerId = ev.getPointerId(0);
                ensureVelocityTracker();
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (activePointerId != INVALID_POINTER) {
                    activePointerId = ev.getPointerId(ev.getActionIndex());
                    lastMotionY = (int) (ev.getY(ev.getActionIndex()) + 0.5f);
                    lastMotionX = (int) (ev.getX(ev.getActionIndex()) + 0.5f);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (activePointerId == INVALID_POINTER) {
                    // touch down wasn't on content
                    break;
                }
                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    break;
                }

                final int y = (int) (ev.getY(pointerIndex) + 0.5f);
                final int x = (int) (ev.getX(pointerIndex) + 0.5f);
                final int dy = y - lastMotionY;
                final int yDiff = Math.abs(dy);
                final int xDiff = Math.abs(x - lastMotionX);
                if (yDiff > touchSlop && yDiff > xDiff) {
                    isDragged = true;
                    if (targetView != null && downPointInTargetChild) {
                        // if target child is nested scroll child
                        // do not intercept the touch event
                        // use the nested event instead
                        if (targetView instanceof NestedScrollingChild) {
                            isDragged = false;
                        } else if (dy > 0 && canChildScrollUp()) {
                            isDragged = false;
                        } else if (dy < 0 && canChildScrollDown()) {
                            isDragged = false;
                        }
                    }
                    lastMotionY = y;
                }

                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                isDragged = false;
                downPointInTargetChild = false;
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
                playAnim();
                activePointerId = INVALID_POINTER;
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
            }
        }

        if (velocityTracker != null) {
            velocityTracker.addMovement(ev);
        }

        if (isDragged && !hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)){
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        }

        return isDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (touchSlop < 0) {
            // use 0 as slop
            touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }
        ev = MotionEvent.obtain(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                final int x = (int) (ev.getX() + 0.5f);
                final int y = (int) (ev.getY() + 0.5f);

                lastMotionX = x;
                lastMotionY = y;
                activePointerId = ev.getPointerId(0);
                ensureVelocityTracker();
                // dispatch start nested scroll
                if (!hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)){
                    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (activePointerId != INVALID_POINTER) {
                    activePointerId = ev.getPointerId(ev.getActionIndex());
                    lastMotionX = (int) (ev.getX(ev.getActionIndex()) + 0.5f);
                    lastMotionY = (int) (ev.getY(ev.getActionIndex()) + 0.5f);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int activePointIndex = ev.findPointerIndex(activePointerId);
                if (activePointIndex == -1) {
                    // if not set isInTouch = false, will error?
                    return false;
                }

                int y = (int) (ev.getY(activePointIndex) + 0.5f);
                int x = (int) (ev.getX(activePointIndex) + 0.5f);
                int dy = y - lastMotionY;

                if (!hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)){
                    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                }
                // dispatch nested pre scroll
                if (dispatchNestedPreScroll(0, -dy, scrollConsumed, screenOffset, ViewCompat.TYPE_TOUCH)) {
                    dy += scrollConsumed[1];
                    ev.offsetLocation(0, screenOffset[1]);
                    y += screenOffset[1];
                }

                if (!isDragged && Math.abs(dy) > touchSlop) {
                    isDragged = true;
                    if (dy > 0) {
                        // scroll down
                        dy -= touchSlop;
                    } else {
                        dy += touchSlop;
                    }
                }

                if (isDragged) {
                    lastMotionY = y;
                    lastMotionX = x;

                    // TODO bind target
                    // begin drag and scroll zoom head layout_img_article_cover
                    int oldOffsetY = (int) getOffset();
                    int scrollDy = -dy;
                    int consumedScrollDy = scroll(scrollDy);
                    int newOffsetY = (int) getOffset();
                    if (consumedScrollDy < 0) {
                        // scroll down
                        // over scroll do not add to scroll consumed
                        consumedScrollDy = Math.min(oldOffsetY, 0) - Math.min(newOffsetY, 0);
                    }
                    if (dispatchNestedScroll(0, consumedScrollDy, 0, scrollDy - consumedScrollDy,
                            screenOffset, ViewCompat.TYPE_TOUCH)) {
                        lastMotionY -= screenOffset[1];
                        ev.offsetLocation(0, screenOffset[1]);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (!isDragged) {
                    performClick();
                }
                isDragged = false;
                downPointInTargetChild = false;
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
                if (velocityTracker != null && activePointerId != INVALID_POINTER) {
                    velocityTracker.addMovement(ev);
                    velocityTracker.computeCurrentVelocity(1000);
                    float yvel = velocityTracker.getYVelocity(activePointerId);
                    setVelocity(getContext(), yvel);
                }
                playAnim();
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                isDragged = false;
                downPointInTargetChild = false;
                stopNestedScroll(ViewCompat.TYPE_TOUCH);
                playAnim();
                activePointerId = INVALID_POINTER;
                break;
            }
        }

        if (velocityTracker != null) {
            velocityTracker.addMovement(ev);
        }

        ev.recycle();
        return true;
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if (child == targetChild) {
            targetChild = null;
            targetView = null;
        }
    }

    /**
     * get the offset of children
     * @return
     */
    public float getOffset() {
        return offsetValueHolder.getValue();
    }

    /**
     * 可以移动的最小 offset
     * @return
     */
    public float getMinOffset() {
        return -getHeight();
    }

    /**
     * 可以移动的最大 offset
     * @return
     */
    public float getMaxOffset() {
        return getHeight();
    }

    /**
     * fling 事件的最小 offset ，小于此数值发生 spring（回弹） 事件
     * @return
     */
    public float getMinFlingOffset() {
        return 0;
    }

    /**
     * fling 事件的最大 offset，大于此数值发生 spring（回弹）事件
     * @return
     */
    public float getMaxFlingOffset() {
        return 0;
    }

    /**
     * 获取动画
     * @return
     */
    public SpringFlingAnimation getDampAnimation() {
        return animation;
    }

    /**
     * 设置回弹方式，头部 or 尾部，暂不支持 both
     * @param flag {@link #DAMP_FLAG_START} 默认 start，即头部回弹
     */
    public void setDampFlag(int flag) {
        this.dampFlag = flag;
        if ((dampFlag&DAMP_FLAG_START) == DAMP_FLAG_START) {
            animation.setSpringFlag(SpringFlingAnimation.SPRING_FLAG_MAX);
        } else if ((dampFlag&DAMP_FLAG_END) == DAMP_FLAG_END) {
            animation.setSpringFlag(SpringFlingAnimation.SPRING_FLAG_MIN);
        }
    }

    /**
     * 强制更新 offset
     * @param newOffset
     */
    public void forceOffset(float newOffset) {
        updateOffset(newOffset);
    }

    public void addOffsetChangeListener(OffsetChangeListener listener) {
        offsetChangeListeners.add(listener);
    }

    public void removeOffsetChangeListener(OffsetChangeListener listener) {
        offsetChangeListeners.remove(listener);
    }

    public void setChildScrollCallback(OnChildScrollCallback childScrollCallback) {
        this.childScrollCallback = childScrollCallback;
    }

    /**
     * 用于判断是否有触控事件发生（自己的触控和 nested 的触控）
     * @return
     */
    public boolean isInNestedOrTouch() {
        if (isDragged || nestedScrollInProgress) {
            return true;
        }
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
    }

    /**
     * judge if have child can scroll up
     * @return
     */
    public boolean canChildScrollUp() {
        if (childScrollCallback != null) {
            return childScrollCallback.canChildScrollUp(this, targetChild);
        }
        if (targetChild instanceof ListView) {
            return ListViewCompat.canScrollList((ListView) targetChild, -1);
        }
        return targetChild != null && targetChild.canScrollVertically(-1);
    }

    public boolean canChildScrollDown() {
        if (childScrollCallback != null) {
            return childScrollCallback.canChildScrollDown(this, targetChild);
        }
        if (targetChild instanceof ListView) {
            return ListViewCompat.canScrollList((ListView) targetChild, 1);
        }
        return targetChild != null && targetChild.canScrollVertically(1);
    }

    protected List<OffsetChangeListener> offsetChangeListeners = new ArrayList<>();
    protected SpringFlingAnimation animation;
    protected int dampFlag = DAMP_FLAG_START;
    protected FloatValueHolder offsetValueHolder;

    // touch event
    protected boolean isDragged;
    protected boolean downPointInTargetChild;
    protected boolean nestedScrollInProgress;
    protected int activePointerId = INVALID_POINTER;
    protected int lastMotionY;
    protected int lastMotionX;
    protected int touchSlop = -1;
    protected int minFlingVelocity = -1;
    protected int maxFlingVelocity = -1;
    protected VelocityTracker velocityTracker;
    // consumed
    protected int[] scrollConsumed = new int[2];
    protected int[] screenOffset = new int[2];
    // cache child view offset
    protected int childOffset;
    // the targetScrollChild
    @Nullable
    protected View targetChild;
    protected View targetView;
    // child scroll callback
    @Nullable
    protected OnChildScrollCallback childScrollCallback;
    protected NestedScrollingChildHelper childHelper;
    protected NestedScrollingParentHelper parentHelper;
    // nested parent
    protected VelocityTracker nestedVelocityTracker;
    protected long nestedMotionDownTime;
    protected int lastNestedMotionY;

    protected void init() {
        offsetValueHolder = new FloatValueHolder();
        animation = new SpringFlingAnimation(offsetValueHolder);
        animation.setRangeValueHolder(new SpringFlingAnimation.FloatRangeValueHolder() {
            @Override
            public float getMinRange() {
                return getMinFlingOffset();
            }

            @Override
            public float getMaxRange() {
                return getMaxFlingOffset();
            }
        });
        animation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                if (isInNestedOrTouch()) {
                    animation.cancel();
                    animation.setStartVelocity(0);
                    return;
                }
                int oldOffset = childOffset;
                applyOffsetToView();
                int newOffset = childOffset;
                notifyOffsetChange(oldOffset, newOffset);
            }
        });
        setDampFlag(DAMP_FLAG_START);
        childHelper = new NestedScrollingChildHelper(this);
        parentHelper = new NestedScrollingParentHelper(this);
        setNestedScrollingEnabled(true);
    }

    protected int scroll(int dy) {
        int oldDy = dy;
        float minOffset = getMinOffset();
        float maxOffset = getMaxOffset();
        float maxFlingOffset = getMaxFlingOffset();
        float minFlingOffset = getMinFlingOffset();
        float oldOffset = getOffset();
        float newOffset = oldOffset - dy;
        float ratio = 1;
        if (newOffset > maxFlingOffset) {
            // overScroll
            ratio = 0.5f - 0.5f * (newOffset - maxFlingOffset) / maxOffset;
        } else if (newOffset < minFlingOffset) {
            // overScroll
            ratio = 0.5f - 0.5f * (newOffset - minFlingOffset) / minOffset;
        }
        dy *= ratio;
        newOffset = oldOffset - dy;
        int consumed = updateOffset(newOffset);
        int unConsumed = -dy - consumed;
        return oldDy + (int)(unConsumed / ratio);
    }

    protected int updateOffset(float newOffset) {
        float minOffset = getMinOffset();
        float maxOffset  = getMaxOffset();
        float minFlingOffset = getMinFlingOffset();
        float maxFlingOffset = getMaxFlingOffset();

        // calculate new offset
        newOffset = MathUtils.clamp(newOffset, minOffset, maxOffset);
        if ((dampFlag&DAMP_FLAG_END) == 0 && newOffset < minFlingOffset) {
            newOffset = minFlingOffset;
        } else if ((dampFlag&DAMP_FLAG_START) == 0
                && newOffset > maxFlingOffset) {
            newOffset = maxFlingOffset;
        }
        int oldUIOffset = childOffset;
        offsetValueHolder.setValue(newOffset);
        applyOffsetToView();
        int newUIOffset = childOffset;
        notifyOffsetChange(oldUIOffset, newUIOffset);
        return newUIOffset - oldUIOffset;
    }

    protected void setVelocity(Context context, float v){
        if (minFlingVelocity == -1){
            ViewConfiguration vc = ViewConfiguration.get(context);
            minFlingVelocity = vc.getScaledMinimumFlingVelocity();
            maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        }
        v = Math.abs(v) < minFlingVelocity ? 0 : v;
        v = Math.max(-maxFlingVelocity, Math.min(v, maxFlingVelocity));
        animation.setStartVelocity(v);
    }

    protected void ensureVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
    }

    protected void applyOffsetToView() {
        float offset = getOffset();
        int moveOffset = (int) (offset - childOffset);
        if (moveOffset == 0) {
            return;
        }
        childOffset += moveOffset;
        for (int i = getChildCount() - 1; i >= 0; --i) {
            View child = getChildAt(i);
            child.offsetTopAndBottom(moveOffset);
        }
    }

    protected void ensureTargetChild() {
        if (targetChild != null) {
            return;
        }
        int count = getChildCount();
        for (int i = 0; i < count; ++i) {
            View child = getChildAt(i);
            if (child instanceof NestedScrollingChild) {
                targetChild = targetView = child;
            } else if (child instanceof ScrollView) {
                targetChild = targetView = child;
            } else if (child instanceof AbsListView) {
                targetChild = targetView = child;
            } else if (child.canScrollVertically(1) || child.canScrollVertically(-1)) {
                targetChild = targetView = child;
            }
            if (targetChild != null) {
                break;
            }
        }
    }

    protected void playAnim() {
        animation.start();
    }

    protected void notifyOffsetChange(int oldOffset, int newOffset) {
        for (OffsetChangeListener listener : offsetChangeListeners) {
            listener.onOffsetChange(this, oldOffset, newOffset);
        }
    }

    /**
     * Check if a given point in the CoordinatorLayout's coordinates are within the view bounds
     * of the given direct child view.
     *
     * @param child child view to test
     * @param x X coordinate to test, in the CoordinatorLayout's coordinate system
     * @param y Y coordinate to test, in the CoordinatorLayout's coordinate system
     * @return true if the point is within the child view's bounds, false otherwise
     */
    private Rect tmp_isPointInChildBounds;
    protected boolean isPointInChildBounds(View child, int x, int y) {
        if (tmp_isPointInChildBounds == null) {
            tmp_isPointInChildBounds = new Rect();
        }
        Rect r = tmp_isPointInChildBounds;
        getDescendantRect(child, r);
        return r.contains(x, y);
    }

    /**
     * Retrieve the transformed bounding rect of an arbitrary descendant view.
     * This does not need to be a direct child.
     *
     * @param descendant descendant view to reference
     * @param out rect to set to the bounds of the descendant view
     */
    private void getDescendantRect(View descendant, Rect out) {
        DampViewUtil.getDescendantRect(this, descendant, out);
    }

    protected void recordNestedDown() {
        nestedVelocityTracker = VelocityTracker.obtain();
        nestedMotionDownTime = System.currentTimeMillis();
        lastNestedMotionY = 0;
        MotionEvent nestedMotionEvent = MotionEvent.obtain(nestedMotionDownTime, nestedMotionDownTime
                , MotionEvent.ACTION_DOWN, 0, lastNestedMotionY, 0);
        nestedVelocityTracker.addMovement(nestedMotionEvent);
        nestedMotionEvent.recycle();
    }

    protected void recordNestedDy(float dy) {
        if (dy == 0) {
            return;
        }
        lastNestedMotionY -= dy;
        MotionEvent nestedMotionEvent = MotionEvent.obtain(nestedMotionDownTime, System.currentTimeMillis(), MotionEvent.ACTION_MOVE
                , 0, lastNestedMotionY, 0);
        if (nestedVelocityTracker != null) {
            nestedVelocityTracker.addMovement(nestedMotionEvent);
        }
        nestedMotionEvent.recycle();
    }

    protected void applyNestedVelocity() {
        if (nestedVelocityTracker != null) {
            nestedVelocityTracker.computeCurrentVelocity(1000);
            float velocity = nestedVelocityTracker.getYVelocity();
            velocity /= 10;
            setVelocity(getContext(), velocity);
        }
    }
}
