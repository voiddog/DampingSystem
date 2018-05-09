package android.support.design.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.animation.DynamicAnimation;
import android.support.animation.FloatValueHolder;
import android.support.animation.SpringFlingAnimation;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.view.NestedScrollingChild2;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.voiddog.android.damp.R;

import java.util.ArrayList;
import java.util.List;

import static android.support.v4.widget.ViewDragHelper.INVALID_POINTER;

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
 * 可以越界的 appbar layout
 *
 * @author qigengxin
 * @since 2018-05-02 14:30
 */
@CoordinatorLayout.DefaultBehavior(OverScrollAppBarLayout.Behavior.class)
public class OverScrollAppBarLayout extends FrameLayout implements NestedScrollingChild2 {

    /****************************************************************
     *                         Listener
     ****************************************************************/
    public interface OnOffsetChangeListener {
        void onOffsetChange(OverScrollAppBarLayout appbarLayout, int oldOffset, int newOffset, int minOffset, int maxOffset);
    }

    /****************************************************************
     *                         LayoutParams
     ****************************************************************/

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {
        public static final int BOTTOM = 0;
        public static final int TOP = 1;
        public static final int CENTER = 3;
        public static final int FULL = 4;

        public int expandGravity;

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);
            init(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height, gravity);
        }

        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull MarginLayoutParams source) {
            super(source);
        }

        @TargetApi(19)
        public LayoutParams(@NonNull FrameLayout.LayoutParams source) {
            super(source);
        }

        private void init(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OverScrollAppBarLayout, 0, 0);
            expandGravity = a.getInt(R.styleable.OverScrollAppBarLayout_ovd_damp_appbar_expand_gravity, expandGravity);
            a.recycle();
        }
    }


    // over scroll appbar close height
    private int closeHeight = 0;
    private NestedScrollingChildHelper childHelper;
    private List<OnOffsetChangeListener> offsetChangeListenerList;
    // the view that determine touch scroll
    // if the view is not null and can scroll down, zoom head layout_img_article_cover can't scroll down by touch event
    private View bindDirectScrollBrother;
    private int bindDirectScrollBrotherId;

    public OverScrollAppBarLayout(@NonNull Context context) {
        super(context);
        init(null, 0);
    }

    public OverScrollAppBarLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public OverScrollAppBarLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    //===============================getter and setter==================================

    public int getCloseHeight() {
        return closeHeight;
    }

    public void setCloseHeight(int closeHeight) {
        this.closeHeight = closeHeight;
    }

    public View getBindDirectScrollBrother() {
        return bindDirectScrollBrother;
    }

    public void setBindDirectScrollBrother(View bindDirectScrollBrother) {
        this.bindDirectScrollBrother = bindDirectScrollBrother;
    }

    public void addOnOffsetChangeListener(OnOffsetChangeListener changeListener) {
        offsetChangeListenerList.add(changeListener);
    }

    public void removeOnOffsetChangeListener(OnOffsetChangeListener changeListener) {
        offsetChangeListenerList.remove(changeListener);
    }

    @Nullable
    public Behavior getBehavior() {
        if (getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) getLayoutParams();
            if (lp.getBehavior() instanceof Behavior) {
                return (Behavior) lp.getBehavior();
            }
        }
        return null;
    }

    /**
     * 判断是否在 nested touch 中，或者在 drag 中
     * @return
     */
    public boolean isInNestedOrTouch() {
        Behavior behavior = getBehavior();
        if (behavior == null) {
            return false;
        }
        if (behavior.isDragged || behavior.nestedScrollInProgress) {
            return true;
        }
        if (hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)) {
            return true;
        }
        return false;
    }

    //===============================Nested Scroll Dispatcher==================================

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        childHelper.setNestedScrollingEnabled(enabled);
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
    public void stopNestedScroll() {
        childHelper.stopNestedScroll();
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

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    //=============================== protected function ==================================
    protected OnOffsetChangeListener innerOffsetChangeListener = new OnOffsetChangeListener() {
        @Override
        public void onOffsetChange(OverScrollAppBarLayout appbarLayout, int oldOffset
                , int newOffset, int minOffset, int maxOffset) {
            for (int i = getChildCount() - 1; i >=0; --i){
                View child = getChildAt(i);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                switch (lp.expandGravity){
                    case LayoutParams.BOTTOM:{
                        child.setTranslationY(0);
                        break;
                    }
                    case LayoutParams.TOP: {
                        if (newOffset > 0){
                            child.setTranslationY(-newOffset);
                        } else {
                            child.setTranslationY(0);
                        }
                        break;
                    }
                    case LayoutParams.CENTER:{
                        if (newOffset > 0){
                            child.setTranslationY(-newOffset/2);
                        } else {
                            child.setTranslationY(0);
                        }
                        break;
                    }
                    case LayoutParams.FULL:{
                        if (newOffset > 0){
                            child.setPivotY(child.getHeight());
                            float scale = (child.getHeight() + newOffset) * 1f / child.getHeight();
                            child.setScaleY(scale);
                            child.setScaleX(scale);
                        } else {
                            child.setScaleX(1);
                            child.setScaleY(1);
                        }
                        break;
                    }
                }
            }
        }
    };

    protected View getBindScrollBrother() {
        if (bindDirectScrollBrother != null){
            return bindDirectScrollBrother;
        }
        if (bindDirectScrollBrotherId == 0 || getParent() == null || !(getParent() instanceof ViewGroup)){
            return null;
        }
        ViewGroup parent = (ViewGroup) getParent();
        bindDirectScrollBrother = parent.findViewById(bindDirectScrollBrotherId);
        return bindDirectScrollBrother;
    }

    //=============================== private function start ==================================

    private void init(AttributeSet attrSet, int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrSet, R.styleable.OverScrollAppBarLayout, defStyle, 0);
        closeHeight = a.getDimensionPixelSize(R.styleable.OverScrollAppBarLayout_ovd_damp_appbar_close_height, closeHeight);
        bindDirectScrollBrotherId = a.getResourceId(R.styleable.OverScrollAppBarLayout_ovd_damp_appbar_brother_id, 0);
        a.recycle();

        childHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        offsetChangeListenerList = new ArrayList<>();
        addOnOffsetChangeListener(innerOffsetChangeListener);
    }

    private void notifyNewOffset(int oldOffset, int offset, int minOffset, int maxOffset) {
        for (OnOffsetChangeListener changeListener : offsetChangeListenerList) {
            changeListener.onOffsetChange(this, oldOffset, offset, minOffset, maxOffset);
        }
    }

    //=============================== private function end ==================================


    /****************************************************************
     *                         Behavior
     ****************************************************************/

    public static class Behavior extends CoordinatorLayout.Behavior<OverScrollAppBarLayout> {
        // damping animation
        private SpringFlingAnimation animation;

        // touch event
        private boolean isDragged;
        private boolean nestedScrollInProgress;
        private int activePointerId = INVALID_POINTER;
        private int lastMotionY;
        private int touchSlop = -1;
        private int minFlingVelocity = -1;
        private int maxFlingVelocity = -1;
        private VelocityTracker velocityTracker;
        // consumed
        private int[] scrollConsumed = new int[2];
        private int[] screenOffset = new int[2];
        // offset value holder
        private FloatValueHolder offsetValueHolder;
        // bind view
        private CoordinatorLayout bindParent;
        private OverScrollAppBarLayout bindChild;

        public Behavior() {
            init();
        }

        public Behavior(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public CoordinatorLayout getBindParent() {
            return bindParent;
        }

        public OverScrollAppBarLayout getBindChild() {
            return bindChild;
        }

        public int getBindTotalScrollRange() {
            if (bindParent == null || bindChild == null) {
                return 0;
            }
            return getTotalScrollRange(bindParent, bindChild);
        }

        public int getBindMinOffset() {
            if (bindParent == null || bindChild == null) {
                return 0;
            }
            return getMinOffset(bindParent, bindChild);
        }

        public int getBindMaxOffset() {
            if (bindParent == null || bindChild == null) {
                return 0;
            }
            return getMaxOffset(bindParent, bindChild);
        }

        public int getBindOverScrollOffset() {
            if (bindParent == null || bindChild == null) {
                return 0;
            }
            return getOverScrollOffset(bindParent, bindChild);
        }

        //=============================== offset range ==================================
        public int getTotalScrollRange(CoordinatorLayout parent, OverScrollAppBarLayout child) {
            return getOverScrollOffset(parent, child) - getMinOffset(parent, child);
        }

        public int getMinOffset(CoordinatorLayout parent, OverScrollAppBarLayout child) {
            return child.getCloseHeight() - child.getMeasuredHeight();
        }

        public int getMaxOffset(CoordinatorLayout parent, OverScrollAppBarLayout child) {
            return parent.getMeasuredHeight() - getOverScrollOffset(parent, child);
        }

        /**
         * 判断何时是 overscroll
         * @param parent
         * @param child
         * @return
         */
        public int getOverScrollOffset(CoordinatorLayout parent, OverScrollAppBarLayout child) {
            return 0;
        }

        public void forceOffset(float offset) {
            offsetValueHolder.setValue(offset);
            if (bindChild != null && bindParent != null) {
                updateOffset(bindParent, bindChild, offset, getMinOffset(bindParent, bindChild), getMaxOffset(bindParent, bindChild));
            }
        }

        public float getOffset() {
            return offsetValueHolder.getValue();
        }

        /**
         * 添加获取动画的接口
         * @return
         */
        public SpringFlingAnimation getAnimation() {
            return animation;
        }

        /**
         * 播放动画，如果可以的话
         */
        @Deprecated
        public void playAnim() {
            getAnimation().start();
        }

        //=============================== layout ==================================

        @Override
        public boolean onLayoutChild(CoordinatorLayout parent, OverScrollAppBarLayout child, int layoutDirection) {
            bindViews(parent, child);
            parent.onLayoutChild(child, layoutDirection);
            child.notifyNewOffset((int)getOffset(), (int)getOffset()
                    , getMinOffset(parent, child), getMaxOffset(parent, child));
            applyOffsetToView(child);
            return true;
        }

        //=============================== touch event ==================================
        @Override
        public boolean onInterceptTouchEvent(CoordinatorLayout parent, OverScrollAppBarLayout child, MotionEvent ev) {
            // ensure touch slop
            if (touchSlop < 0) {
                // use zero as slop
                touchSlop = ViewConfiguration.get(parent.getContext()).getScaledTouchSlop();
                touchSlop /= 4;
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

                    if (parent.isPointInChildBounds(child, x, y)) {
                        lastMotionY = y;
                        activePointerId = ev.getPointerId(0);
                        ensureVelocityTracker();
                    }
                    break;
                }
                case MotionEvent.ACTION_POINTER_DOWN: {
                    if (activePointerId != INVALID_POINTER) {
                        activePointerId = ev.getPointerId(ev.getActionIndex());
                        lastMotionY = (int) (ev.getY(ev.getActionIndex()) + 0.5f);
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
                    final int yDiff = Math.abs(y - lastMotionY);
                    if (yDiff > touchSlop) {
                        isDragged = true;
                        lastMotionY = y;
                    }

                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    isDragged = false;
                    child.stopNestedScroll(ViewCompat.TYPE_TOUCH);
                    playAnim(parent, child);
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

            if (isDragged && !child.hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)){
                child.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
            }

            return isDragged;
        }

        @Override
        public boolean onTouchEvent(CoordinatorLayout parent, OverScrollAppBarLayout child, MotionEvent ev) {
            if (touchSlop < 0) {
                // use 0 as slop
                touchSlop = ViewConfiguration.get(parent.getContext()).getScaledTouchSlop();
                touchSlop /= 4;
            }
            ev = MotionEvent.obtain(ev);

            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    final int x = (int) (ev.getX() + 0.5f);
                    final int y = (int) (ev.getY() + 0.5f);

                    if (parent.isPointInChildBounds(child, x, y)) {
                        lastMotionY = y;
                        activePointerId = ev.getPointerId(0);
                        ensureVelocityTracker();
                        // dispatch start nested scroll
                        if (!child.hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)){
                            child.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                        }
                    } else {
                        // if not set isInTouch = false, will error?
                        return false;
                    }
                    break;
                }
                case MotionEvent.ACTION_POINTER_DOWN: {
                    if (activePointerId != INVALID_POINTER) {
                        activePointerId = ev.getPointerId(ev.getActionIndex());
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
                    int dy = y - lastMotionY;

                    if (!child.hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)){
                        child.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
                    }
                    // dispatch nested pre scroll
                    if (child.dispatchNestedPreScroll(0, -dy, scrollConsumed, screenOffset, ViewCompat.TYPE_TOUCH)) {
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

                        if (dy > 0) {
                            // if bind brother can scroll, do not scroll down
                            View brother = child.getBindScrollBrother();
                            if (brother != null && brother.canScrollVertically(-1)) {
                                break;
                            }
                        }

                        // begin drag and scroll zoom head layout_img_article_cover
                        int oldOffsetY = (int) getOffset();
                        int scrollDy = -dy;
                        int consumedScrollDy = scroll(parent, child, scrollDy, getMinOffset(parent, child), getMaxOffset(parent, child));
                        int newOffsetY = (int) getOffset();
                        if (consumedScrollDy < 0) {
                            // scroll down
                            // over scroll do not add to scroll consumed
                            consumedScrollDy = Math.min(oldOffsetY, 0) - Math.min(newOffsetY, 0);
                        }
                        if (child.dispatchNestedScroll(0, consumedScrollDy, 0, scrollDy - consumedScrollDy,
                                screenOffset, ViewCompat.TYPE_TOUCH)) {
                            lastMotionY -= screenOffset[1];
                            ev.offsetLocation(0, screenOffset[1]);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    isDragged = false;
                    child.stopNestedScroll(ViewCompat.TYPE_TOUCH);
                    if (velocityTracker != null && activePointerId != INVALID_POINTER) {
                        velocityTracker.addMovement(ev);
                        velocityTracker.computeCurrentVelocity(1000);
                        float yvel = velocityTracker.getYVelocity(activePointerId);
                        View brother = child.getBindScrollBrother();
                        if (yvel < 0 || brother == null || !brother.canScrollVertically(-1)){
                            setVelocity(parent.getContext(), yvel);
                        }
                    }
                    playAnim(parent, child);
                    break;
                }
                case MotionEvent.ACTION_CANCEL: {
                    isDragged = false;
                    child.stopNestedScroll(ViewCompat.TYPE_TOUCH);
                    playAnim(parent, child);
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

        /****************************************************************
         *                    Behavior Nested Scroll
         ****************************************************************/


        private VelocityTracker nestedVelocityTracker;
        private long nestedMotionDownTime;
        private int lastNestedMotionY;

        @Override
        public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull OverScrollAppBarLayout child
                , @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
            return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0 && directTargetChild != child;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull CoordinatorLayout coordinatorLayout, @NonNull OverScrollAppBarLayout child
                , @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
            super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, axes, type);
            if (type == ViewCompat.TYPE_TOUCH) {
                nestedScrollInProgress = true;
            }

            if (nestedVelocityTracker != null) {
                nestedVelocityTracker.recycle();
                nestedVelocityTracker = null;
            }
            nestedVelocityTracker = VelocityTracker.obtain();
            nestedMotionDownTime = System.currentTimeMillis();
            lastNestedMotionY = 0;
            MotionEvent nestedMotionEvent = MotionEvent.obtain(nestedMotionDownTime, nestedMotionDownTime
                    , MotionEvent.ACTION_DOWN, 0, lastMotionY, 0);
            nestedVelocityTracker.addMovement(nestedMotionEvent);
            nestedMotionEvent.recycle();
        }

        @Override
        public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull OverScrollAppBarLayout child
                , @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
            if (dy > 0 && !isDragged) {
                // scroll up
                consumed[1] = scroll(coordinatorLayout, child, dy, getMinOffset(coordinatorLayout, child)
                        , getMaxOffset(coordinatorLayout, child));

                lastNestedMotionY -= consumed[1];
                MotionEvent nestedMotionEvent = MotionEvent.obtain(nestedMotionDownTime, System.currentTimeMillis(), MotionEvent.ACTION_MOVE
                        , 0, lastNestedMotionY, 0);
                if (nestedVelocityTracker != null) {
                    nestedVelocityTracker.addMovement(nestedMotionEvent);
                }
                nestedMotionEvent.recycle();
            }
        }

        @Override
        public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull OverScrollAppBarLayout child
                , @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
            if (dyUnconsumed < 0 && !isDragged) {
                // scroll down
                int consumedScroll = scroll(coordinatorLayout, child, dyUnconsumed, getMinOffset(coordinatorLayout, child)
                        , getMaxOffset(coordinatorLayout, child));
                lastNestedMotionY -= consumedScroll;
                MotionEvent nestedMotionEvent = MotionEvent.obtain(nestedMotionDownTime, System.currentTimeMillis(), MotionEvent.ACTION_MOVE
                        , 0, lastNestedMotionY, 0);
                if (nestedVelocityTracker != null) {
                    nestedVelocityTracker.addMovement(nestedMotionEvent);
                }
                nestedMotionEvent.recycle();
            }
            if (isOverScroll(coordinatorLayout, child) && type == ViewCompat.TYPE_NON_TOUCH && !isDragged) {
                if (nestedVelocityTracker != null) {
                    nestedVelocityTracker.computeCurrentVelocity(1000);
                    float velocity = nestedVelocityTracker.getYVelocity();
                    velocity /= 4;
                    if (velocity > 0) {
                        setVelocity(coordinatorLayout.getContext(), velocity);
                    }
                }
                if (target instanceof NestedScrollingChild2) {
                    ((NestedScrollingChild2) target).stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
                }
                playAnim(coordinatorLayout, child);
            } else if (!child.canScrollVertically(1) && dyUnconsumed > 0 && type == ViewCompat.TYPE_NON_TOUCH){
                if (target instanceof NestedScrollingChild2) {
                    ((NestedScrollingChild2) target).stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);
                }
            }
        }

        @Override
        public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull OverScrollAppBarLayout child
                , @NonNull View target, int type) {
            if (type == ViewCompat.TYPE_NON_TOUCH) {
                if (nestedVelocityTracker != null) {
                    nestedVelocityTracker.recycle();
                    nestedVelocityTracker = null;
                }
            } else if (type == ViewCompat.TYPE_TOUCH) {
                nestedScrollInProgress = false;
                playAnim(coordinatorLayout, child);
            }
        }

        /****************************************************************
         *                     private function
         ****************************************************************/

        private void bindViews(CoordinatorLayout parent, OverScrollAppBarLayout child) {
            if (bindParent != parent) {
                bindParent = parent;
            }
            if (bindChild != child) {
                bindChild = child;
            }
        }

        private void applyOffsetToView(OverScrollAppBarLayout child) {
            int offset = (int) (offsetValueHolder.getValue() - child.getTop());
            if (offset != 0) {
                ViewCompat.offsetTopAndBottom(child, offset);
            }
        }

        /**
         * @param dy < 0 scroll down > 0 scroll up
         * @return
         */
        private int scroll(CoordinatorLayout coordinatorLayout, OverScrollAppBarLayout header, int dy, int minOffset, int maxOffset) {
            int oldDy = dy;
            float newOffset = getOffset() - dy;
            float ratio = 1;
            if (newOffset > 0) {
                // overScroll
                ratio = 1 - newOffset * 1f / maxOffset;
            }
            dy *= ratio;
            newOffset = getOffset() - dy;
            int consumed = updateOffset(coordinatorLayout, header, newOffset, minOffset, maxOffset);
            int unConsumed = -dy - consumed;
            return oldDy + (int) (unConsumed * ratio);
        }

        private int updateOffset(CoordinatorLayout parent, OverScrollAppBarLayout header, float newOffset
                , int minOffset, int maxOffset) {

            // calculate a new offset
            newOffset = MathUtils.clamp(newOffset, minOffset, maxOffset);
            int oldIntOffset = (int) getOffset();
            offsetValueHolder.setValue(newOffset);
            applyOffsetToView(header);
            int newIntOffset = (int) getOffset();
            header.notifyNewOffset(oldIntOffset, newIntOffset, minOffset, maxOffset);

            return newIntOffset - oldIntOffset;
        }

        private void ensureVelocityTracker() {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
        }

        private boolean isOverScroll(CoordinatorLayout parent, OverScrollAppBarLayout child) {
            return offsetValueHolder.getValue() > getOverScrollOffset(parent, child);
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

        private void playAnim(CoordinatorLayout parent, OverScrollAppBarLayout child) {
            bindViews(parent, child);
            animation.start();
        }

        private void init() {
            offsetValueHolder = new FloatValueHolder();
            animation = new SpringFlingAnimation(offsetValueHolder);
            animation.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
                @Override
                public void onAnimationUpdate(DynamicAnimation animation, float value, float velocity) {
                    if (bindChild == null || bindParent == null || isDragged || nestedScrollInProgress) {
                        animation.cancel();
                        animation.setStartVelocity(0);
                        return;
                    }
                    int oldOffset = bindChild.getTop();
                    bindChild.notifyNewOffset(oldOffset, (int) getOffset(), getMinOffset(bindParent, bindChild)
                            , getMaxOffset(bindParent, bindChild));
                    applyOffsetToView(bindChild);
                }
            });
            animation.setSpringFlag(SpringFlingAnimation.SPRING_FLAG_MAX);
            animation.setRangeValueHolder(new SpringFlingAnimation.FloatRangeValueHolder() {
                @Override
                public float getMinRange() {
                    if (bindParent == null || bindChild == null) {
                        return 0;
                    }
                    return getMinOffset(bindParent, bindChild);
                }

                @Override
                public float getMaxRange() {
                    if (bindParent == null || bindChild == null) {
                        return 0;
                    }
                    return getOverScrollOffset(bindParent, bindChild);
                }
            });
        }
    }

    /****************************************************************
     *                         Scroller Behavior
     ****************************************************************/
    public static class ScrollingViewBehavior extends HeaderScrollingViewBehavior {

        public ScrollingViewBehavior() {
        }

        public ScrollingViewBehavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        View findFirstDependency(List<View> views) {
            for (int i = 0, z = views.size(); i < z; i++) {
                View view = views.get(i);
                if (view instanceof OverScrollAppBarLayout) {
                    return view;
                }
            }
            return null;
        }

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
            return dependency instanceof OverScrollAppBarLayout;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
            offsetChildAsNeeded(parent, child, dependency);
            return false;
        }

        @Override
        int getScrollRange(View v) {
            if (v.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) v.getLayoutParams();
                if (lp.getBehavior() instanceof Behavior && v instanceof OverScrollAppBarLayout
                        && v.getParent() instanceof CoordinatorLayout) {
                    Behavior behavior = (Behavior) lp.getBehavior();
                    CoordinatorLayout parent = (CoordinatorLayout) v.getParent();
                    return behavior.getTotalScrollRange(parent, (OverScrollAppBarLayout) v);
                }
            }
            return super.getScrollRange(v);
        }

        private void offsetChildAsNeeded(CoordinatorLayout parent, View child, View dependency) {
            final CoordinatorLayout.Behavior behavior =
                    ((CoordinatorLayout.LayoutParams) dependency.getLayoutParams()).getBehavior();
            if (behavior instanceof Behavior) {
                // Offset the child, pinning it to the bottom the header-dependency, maintaining
                // any vertical gap and overlap
                ViewCompat.offsetTopAndBottom(child, (dependency.getBottom() - child.getTop())
                        + getVerticalLayoutGap()
                        - getOverlapPixelsForOffset(dependency));
            }
        }
    }
}
