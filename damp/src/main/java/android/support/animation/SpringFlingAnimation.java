package android.support.animation;

import android.annotation.SuppressLint;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;

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
 * @since 2018-04-29 16:56
 */
public class SpringFlingAnimation extends DynamicAnimation<SpringFlingAnimation> {

    public static final int SPRING_FLAG_MIN = 1;
    public static final int SPRING_FLAG_MAX = 2;

    public interface FloatRangeValueHolder {
        /**
         * get the min range value
         *
         * @return
         */
        float getMinRange();

        /**
         * get the max range value
         *
         * @return
         */
        float getMaxRange();
    }

    // 数值范围提供其
    private FloatRangeValueHolder rangeValueHolder;
    // fling 逻辑
    private DragForce flingForce;
    // 阻尼逻辑
    private SpringForce springForce;
    // 阻尼标记
    private int springFlag = SPRING_FLAG_MIN | SPRING_FLAG_MAX;

    public SpringFlingAnimation(FloatValueHolder floatValueHolder) {
        super(floatValueHolder);
        init();
    }

    public <K> SpringFlingAnimation(K object, FloatPropertyCompat<K> property) {
        super(object, property);
        init();
    }

    public SpringFlingAnimation setRangeValueHolder(@NonNull FloatRangeValueHolder valueHolder) {
        this.rangeValueHolder = valueHolder;
        return this;
    }

    public void setSpringFlag(int springFlag) {
        this.springFlag = springFlag;
    }

    public int getSpringFlag() {
        return springFlag;
    }

    @Override
    public void start() {
        springForce.setValueThreshold(getValueThreshold());
        super.start();
    }

    /**
     * Returns the spring that the animation uses for animations.
     *
     * @return the spring that the animation uses for animations
     */
    public SpringForce getSpring() {
        return springForce;
    }

    /**
     * Sets the friction for the fling animation. The greater the friction is, the sooner the
     * animation will slow down. When not set, the friction defaults to 1.
     *
     * @param friction the friction used in the animation
     * @return the animation whose friction will be scaled
     * @throws IllegalArgumentException if the input friction is not positive
     */
    public SpringFlingAnimation setFlingFriction(
            @FloatRange(from = 0.0, fromInclusive = false) float friction) {
        if (friction <= 0) {
            throw new IllegalArgumentException("Friction must be positive");
        }
        flingForce.setFrictionScalar(friction);
        return this;
    }

    /**
     * Returns the friction being set on the animation via {@link #setFlingFriction(float)}. If the
     * friction has not been set, the default friction of 1 will be returned.
     *
     * @return friction being used in the animation
     */
    public float getFlingFriction() {
        return flingForce.getFrictionScalar();
    }

    @Override
    boolean updateValueAndVelocity(long deltaT) {
        float min = 0, max = 0;
        if (rangeValueHolder != null) {
            min = rangeValueHolder.getMinRange();
            max = rangeValueHolder.getMaxRange();
        }
        mValue = mProperty.getValue(mTarget);
        if (mValue >= min && mValue <= max) {
            // fling
            MassState state = flingForce.updateValueAndVelocity(mValue, mVelocity, deltaT);
            mValue = state.mValue;
            mVelocity = state.mVelocity;

            // When the animation hits the max/min value, consider animation done.
            if (mValue > max) {
                if ((springFlag&SPRING_FLAG_MAX) == 0) {
                    // 不能比 max 大
                    mValue = max;
                    mVelocity = 0;
                    return true;
                }
            } else if (mValue < min) {
                if ((springFlag&SPRING_FLAG_MIN) == 0) {
                    // 不能比 min 小
                    mValue = min;
                    mVelocity = 0;
                    return true;
                }
            }

            if (isAtEquilibrium(mValue, mVelocity)) {
                return true;
            }
            return false;
        } else {
            //  越界
            float finalPosition;
            if (mValue > max) {
                finalPosition = max;
            } else {
                finalPosition = min;
            }
            springForce.setFinalPosition(finalPosition);
            MassState massState = springForce.updateValues(mValue, mVelocity, deltaT);
            mValue = massState.mValue;
            mVelocity = massState.mVelocity;
            if (mValue > max) {
                if ((springFlag&SPRING_FLAG_MAX) == 0) {
                    // 不能比 max 大
                    mValue = max;
                    mVelocity = 0;
                    return true;
                }
            } else if (mValue < min) {
                if ((springFlag&SPRING_FLAG_MIN) == 0) {
                    // 不能比 min 小
                    mValue = min;
                    mVelocity = 0;
                    return true;
                }
            }
            if (isAtEquilibrium(mValue, mVelocity)) {
                mValue = springForce.getFinalPosition();
                mVelocity = 0;
                return true;
            }
            return false;
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    float getAcceleration(float value, float velocity) {
        float min = 0, max = 0;
        if (rangeValueHolder != null) {
            min = rangeValueHolder.getMinRange();
            max = rangeValueHolder.getMaxRange();
        }
        if (value > min && value < max) {
            // fling
            return flingForce.getAcceleration(value, velocity);
        }
        return springForce.getAcceleration(value, velocity);
    }

    @SuppressLint("RestrictedApi")
    @Override
    boolean isAtEquilibrium(float value, float velocity) {
        float min = 0, max = 0;
        if (rangeValueHolder != null) {
            min = rangeValueHolder.getMinRange();
            max = rangeValueHolder.getMaxRange();
        }
        if (value > min && value < max) {
            return flingForce.isAtEquilibrium(value, velocity);
        }
        return springForce.isAtEquilibrium(value, velocity);
    }

    @Override
    void setValueThreshold(float threshold) {
        flingForce.setValueThreshold(threshold);
    }

    //===============================below is private ==================================

    private void init() {
        springForce = new SpringForce()
                .setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                .setStiffness(600f);
        flingForce = new DragForce();
        flingForce.setValueThreshold(getValueThreshold());
    }

    private static final class DragForce implements Force {

        private static final float DEFAULT_FRICTION = -4.2f;

        // This multiplier is used to calculate the velocity threshold given a certain value
        // threshold. The idea is that if it takes >= 1 frame to move the value threshold amount,
        // then the velocity is a reasonable threshold.
        private static final float VELOCITY_THRESHOLD_MULTIPLIER = 1000f / 16f;
        private float mFriction = DEFAULT_FRICTION;
        private float mVelocityThreshold;

        // Internal state to hold a value/velocity pair.
        private final DynamicAnimation.MassState mMassState = new DynamicAnimation.MassState();

        void setFrictionScalar(float frictionScalar) {
            mFriction = frictionScalar * DEFAULT_FRICTION;
        }

        float getFrictionScalar() {
            return mFriction / DEFAULT_FRICTION;
        }

        MassState updateValueAndVelocity(float value, float velocity, long deltaT) {
            mMassState.mVelocity = (float) (velocity * Math.exp((deltaT / 1000f) * mFriction));
            mMassState.mValue = (float) (value - velocity / mFriction
                    + velocity / mFriction * Math.exp(mFriction * deltaT / 1000f));
            if (isAtEquilibrium(mMassState.mValue, mMassState.mVelocity)) {
                mMassState.mVelocity = 0f;
            }
            return mMassState;
        }

        @Override
        public float getAcceleration(float position, float velocity) {
            return velocity * mFriction;
        }

        @Override
        public boolean isAtEquilibrium(float value, float velocity) {
            return Math.abs(velocity) < mVelocityThreshold;
        }

        void setValueThreshold(float threshold) {
            mVelocityThreshold = threshold * VELOCITY_THRESHOLD_MULTIPLIER;
        }
    }
}
