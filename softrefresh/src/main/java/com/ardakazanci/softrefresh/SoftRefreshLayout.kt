package com.ardakazanci.softrefresh


import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.ViewCompat


class SoftRefreshLayout(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    FrameLayout(context, attrs, defStyleAttr) {
    private var mHeaderBackColor = -0x746f51
    private var mHeaderForeColor = -0x1
    private var mHeaderCircleSmaller = 10
    private var mPullHeight = 0f
    private var mHeaderHeight = 0f
    private var mChildView: View? = null
    private var mHeader: SoftView? = null
    private var mIsRefreshing = false
    private var mTouchStartY = 0f
    private var mTouchCurY = 0f
    private var mUpBackAnimator: ValueAnimator? = null
    private var mUpTopAnimator: ValueAnimator? = null
    private val decelerateInterpolator = DecelerateInterpolator(10F)

    constructor(context: Context) : this(context, null, 0) {}
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {}

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        if (childCount > 1) {
            throw RuntimeException("Child Error")
        }
        setAttrs(attrs)
        mPullHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            150f,
            context.getResources().getDisplayMetrics()
        )
        mHeaderHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            100f,
            context.getResources().getDisplayMetrics()
        )
        post {
            mChildView = getChildAt(0)
            addHeaderView()
        }
    }

    private fun setAttrs(attrs: AttributeSet?) {
        val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.SoftRefresh)
        mHeaderBackColor =
            a.getColor(R.styleable.SoftRefresh_SoftBackgroundColor, mHeaderBackColor)
        mHeaderForeColor =
            a.getColor(R.styleable.SoftRefresh_SoftForegroundColor, mHeaderForeColor)
        mHeaderCircleSmaller =
            a.getInt(R.styleable.SoftRefresh_SoftSize, mHeaderCircleSmaller)
        a.recycle()
    }

    private fun addHeaderView() {
        mHeader = SoftView(context)
        val params = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        params.gravity = Gravity.TOP
        mHeader?.layoutParams = params
        addViewInternal(mHeader!!)
        mHeader?.setAniBackColor(mHeaderBackColor)
        mHeader?.setAniForeColor(mHeaderForeColor)
        mHeader?.setRadius(mHeaderCircleSmaller)
        setUpChildAnimation()
    }

    private fun setUpChildAnimation() {
        if (mChildView == null) {
            return
        }
        mUpBackAnimator = ValueAnimator.ofFloat(mPullHeight, mHeaderHeight)
        mUpBackAnimator?.addUpdateListener(AnimatorUpdateListener { animation ->
            val `val` = animation.animatedValue as Float
            mChildView?.setTranslationY(`val`)
        })
        mUpBackAnimator?.setDuration(REL_DRAG_DUR)
        mUpTopAnimator = ValueAnimator.ofFloat(mHeaderHeight, 0f)
        mUpTopAnimator?.addUpdateListener(AnimatorUpdateListener { animation ->
            var `val` = animation.animatedValue as Float
            `val` = decelerateInterpolator.getInterpolation(`val` / mHeaderHeight) * `val`
            if (mChildView != null) {
                mChildView?.setTranslationY(`val`)
            }
            mHeader?.getLayoutParams()?.height = `val`.toInt()
            mHeader?.requestLayout()
        })
        mUpTopAnimator?.setDuration(BACK_TOP_DUR)
        mHeader?.setOnViewAniDone(object : SoftView.OnViewAniDone {
            override fun viewAniDone() {
                mUpTopAnimator?.start()
            }
        })
    }

    private fun addViewInternal(child: View) {
        super.addView(child)
    }

    override fun addView(child: View?) {
        if (childCount >= 1) {
            throw RuntimeException("you can only attach one child")
        }
        mChildView = child
        super.addView(child)
        setUpChildAnimation()
    }

    private fun canChildScrollUp(): Boolean {
        return if (mChildView == null) {
            false
        } else ViewCompat.canScrollVertically(mChildView, -1)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mIsRefreshing) {
            return true
        }
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchStartY = ev.y
                mTouchCurY = mTouchStartY
            }
            MotionEvent.ACTION_MOVE -> {
                val curY = ev.y
                val dy = curY - mTouchStartY
                if (dy > 0 && !canChildScrollUp()) {
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (mIsRefreshing) {
            super.onTouchEvent(event)
        } else when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                mTouchCurY = event.y
                var dy = mTouchCurY - mTouchStartY
                dy = Math.min(mPullHeight * 2, dy)
                dy = Math.max(0f, dy)
                if (mChildView != null) {
                    val offsetY =
                        decelerateInterpolator.getInterpolation(dy / 2 / mPullHeight) * dy / 2
                    mChildView?.setTranslationY(offsetY)
                    mHeader?.getLayoutParams()!!.height = offsetY.toInt()
                    mHeader?.requestLayout()
                }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mChildView != null) {
                    if (mChildView?.getTranslationY()!! >= mHeaderHeight) {
                        mUpBackAnimator!!.start()
                        mHeader?.releaseDrag()
                        mIsRefreshing = true
                        if (onCircleRefreshListener != null) {
                            onCircleRefreshListener!!.refreshing()
                        }
                    } else {
                        val height: Float = mChildView!!.getTranslationY()
                        val backTopAni = ValueAnimator.ofFloat(height, 0f)
                        backTopAni.addUpdateListener { animation ->
                            var `val` = animation.animatedValue as Float
                            `val` =
                                decelerateInterpolator.getInterpolation(`val` / mHeaderHeight) * `val`
                            if (mChildView != null) {
                                mChildView?.setTranslationY(`val`)
                            }
                            mHeader?.getLayoutParams()?.height = `val`.toInt()
                            mHeader?.requestLayout()
                        }
                        backTopAni.duration = (height * BACK_TOP_DUR / mHeaderHeight).toLong()
                        backTopAni.start()
                    }
                }
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    fun finishRefreshing() {
        if (onCircleRefreshListener != null) {
            onCircleRefreshListener!!.completeRefresh()
        }
        mIsRefreshing = false
        mHeader?.setRefreshing(false)
    }

    private var onCircleRefreshListener: OnCircleRefreshListener? = null

    init {
        init(context, attrs, defStyleAttr)
    }

    fun setOnRefreshListener(onCircleRefreshListener: OnCircleRefreshListener?) {
        this.onCircleRefreshListener = onCircleRefreshListener
    }

    interface OnCircleRefreshListener {
        fun completeRefresh()
        fun refreshing()
    }

    companion object {
        private const val TAG = "SoftRefreshLayout"
        private const val BACK_TOP_DUR: Long = 600
        private const val REL_DRAG_DUR: Long = 200
    }
}