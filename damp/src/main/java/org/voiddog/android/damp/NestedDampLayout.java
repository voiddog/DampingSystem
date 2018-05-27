package org.voiddog.android.damp;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.animation.DynamicAnimation;
import android.support.animation.FloatValueHolder;
import android.support.animation.SpringFlingAnimation;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingParent2;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ListViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;

import static android.support.v4.widget.ViewDragHelper.INVALID_POINTER;

public class NestedDampLayout extends FrameLayout implements NestedScrollingChild2, NestedScrollingParent2{

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
        void onOffsetChange(NestedDampLayout layout, float oldOffset, float newOffset);
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

    // nested
    @Override
    public boolean startNestedScroll(int axes, int type) {
        return false;
    }

    @Override
    public void stopNestedScroll(int type) {

    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return false;
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
        return false;
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
        return false;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
        return false;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {

    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {

    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
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
                    if (targetChild != null && downPointInTargetChild) {
                        if (dy > 0 && canChildScrollUp()) {
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
                    // TODO target
                    float yvel = velocityTracker.getYVelocity(activePointerId);
                    if (yvel < 0){
                        setVelocity(getContext(), yvel);
                    }
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
        }
    }

    public float getOffset() {
        return offsetValueHolder.getValue();
    }

    public float getMinOffset() {
        return -getHeight();
    }

    public float getMaxOffset() {
        return getHeight();
    }

    public float getMinFlingOffset() {
        return 0;
    }

    public float getMaxFlingOffset() {
        return 0;
    }

    public SpringFlingAnimation getDampAnimation() {
        return animation;
    }

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

    // private
    private List<OffsetChangeListener> offsetChangeListeners = new ArrayList<>();
    private SpringFlingAnimation animation;
    private FloatValueHolder offsetValueHolder;

    // touch event
    private boolean isDragged;
    private boolean downPointInTargetChild;
    private boolean nestedScrollInProgress;
    private int activePointerId = INVALID_POINTER;
    private int lastMotionY;
    private int lastMotionX;
    private int touchSlop = -1;
    private int minFlingVelocity = -1;
    private int maxFlingVelocity = -1;
    private VelocityTracker velocityTracker;
    // consumed
    private int[] scrollConsumed = new int[2];
    private int[] screenOffset = new int[2];
    // cache child view offset
    private int childOffset;
    // the targetScrollChild
    @Nullable
    private View targetChild;
    // child scroll callback
    @Nullable
    private OnChildScrollCallback childScrollCallback;

    private void init() {
        setNestedScrollingEnabled(true);
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
                float oldOffset = childOffset;
                applyOffsetToView();
                notifyOffsetChange(oldOffset, getOffset());
            }
        });
        animation.setSpringFlag(SpringFlingAnimation.SPRING_FLAG_MAX | SpringFlingAnimation.SPRING_FLAG_MIN);
    }

    private int scroll(int dy) {
        int oldDy = dy;
        float minOffset = getMinOffset();
        float maxOffset = getMaxOffset();
        float maxFlingOffset = getMaxFlingOffset();
        float minFlingOffset = getMinFlingOffset();
        float newOffset = getOffset() - dy;
        float ratio = 1;
        if (newOffset > maxFlingOffset) {
            // overScroll
            ratio = 1 - (newOffset - maxFlingOffset) / maxOffset;
        } else if (newOffset < minFlingOffset) {
            // overScroll
            ratio = 1 - (newOffset - minFlingOffset) / minOffset;
        }
        dy *= ratio;
        newOffset = getOffset() - dy;
        float consumed = updateOffset(newOffset);
        float unConsumed = -dy - consumed;
        return oldDy + (int) (unConsumed * ratio);
    }

    private float updateOffset(float newOffset) {
        float minOffset = getMinOffset();
        float maxOffset  = getMaxOffset();

        // calculate new offset
        newOffset = MathUtils.clamp(newOffset, minOffset, maxOffset);
        float oldOffset = getOffset();
        offsetValueHolder.setValue(newOffset);
        applyOffsetToView();
        notifyOffsetChange(oldOffset, newOffset);
        return newOffset - oldOffset;
    }

    private void setVelocity(Context context, float v){
        if (minFlingVelocity == -1){
            ViewConfiguration vc = ViewConfiguration.get(context);
            minFlingVelocity = vc.getScaledMinimumFlingVelocity();
            maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        }
        v = Math.abs(v) < minFlingVelocity ? 0 : v;
        v = Math.max(-maxFlingVelocity, Math.min(v, maxFlingVelocity));
        animation.setStartVelocity(v);
    }

    private void ensureVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
    }

    private void applyOffsetToView() {
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

    private void ensureTargetChild() {
        if (targetChild != null) {
            return;
        }
        int count = getChildCount();
        for (int i = 0; i < count; ++i) {
            View child = getChildAt(i);
            if (child instanceof NestedScrollingChild) {
                targetChild = child;
            } else if (child instanceof ScrollView) {
                targetChild = child;
            } else if (child instanceof AbsListView) {
                targetChild = child;
            } else if (child.canScrollVertically(1) || child.canScrollVertically(-1)) {
                targetChild = child;
            }
            if (targetChild != null) {
                break;
            }
        }
    }

    private void playAnim() {
        animation.start();
    }

    private void notifyOffsetChange(float oldOffset, float newOffset) {
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
    private boolean isPointInChildBounds(View child, int x, int y) {
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
        getDescendantRect(this, descendant, out);
    }

    /**
     * This is a port of the common
     * {@link ViewGroup#offsetDescendantRectToMyCoords(View, Rect)}
     * from the framework, but adapted to take transformations into account. The result
     * will be the bounding rect of the real transformed rect.
     *
     * @param descendant view defining the original coordinate system of rect
     * @param rect (in/out) the rect to offset from descendant to this view's coordinate system
     */
    Matrix tmpMatrix_offsetDescendantRect;
    RectF tmpRectF_offsetDescendantRect;
    private void offsetDescendantRect(ViewGroup parent, View descendant, Rect rect) {
        if (tmpMatrix_offsetDescendantRect == null) {
            tmpMatrix_offsetDescendantRect = new Matrix();
        } else {
            tmpMatrix_offsetDescendantRect.reset();
        }
        Matrix m = tmpMatrix_offsetDescendantRect;

        offsetDescendantMatrix(parent, descendant, m);

        if (tmpRectF_offsetDescendantRect == null) {
            tmpRectF_offsetDescendantRect = new RectF();
        }
        RectF rectF = tmpRectF_offsetDescendantRect;
        rectF.set(rect);
        m.mapRect(rectF);
        rect.set((int) (rectF.left + 0.5f), (int) (rectF.top + 0.5f),
                (int) (rectF.right + 0.5f), (int) (rectF.bottom + 0.5f));
    }

    /**
     * Retrieve the transformed bounding rect of an arbitrary descendant view.
     * This does not need to be a direct child.
     *
     * @param descendant descendant view to reference
     * @param out rect to set to the bounds of the descendant view
     */
    private void getDescendantRect(ViewGroup parent, View descendant, Rect out) {
        out.set(0, 0, descendant.getWidth(), descendant.getHeight());
        offsetDescendantRect(parent, descendant, out);
    }

    private void offsetDescendantMatrix(ViewParent target, View view, Matrix m) {
        final ViewParent parent = view.getParent();
        if (parent instanceof View && parent != target) {
            final View vp = (View) parent;
            offsetDescendantMatrix(target, vp, m);
            m.preTranslate(-vp.getScrollX(), -vp.getScrollY());
        }

        m.preTranslate(view.getLeft(), view.getTop());

        if (!view.getMatrix().isIdentity()) {
            m.preConcat(view.getMatrix());
        }
    }
}
