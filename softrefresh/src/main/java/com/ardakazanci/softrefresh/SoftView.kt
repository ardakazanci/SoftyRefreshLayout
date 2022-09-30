package com.ardakazanci.softrefresh

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup


class SoftView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    View(context, attrs, defStyleAttr) {
    private var PULL_HEIGHT = 0
    private var PULL_DELTA = 0
    private var mWidthOffset = 0f
    private var mAniStatus = AnimatorStatus.PULL_DOWN

    internal enum class AnimatorStatus {
        PULL_DOWN, DRAG_DOWN, REL_DRAG, SPRING_UP,
        POP_BALL, OUTER_CIR, REFRESHING, DONE, STOP;

        override fun toString(): String {
            return when (this) {
                PULL_DOWN -> "pull down"
                DRAG_DOWN -> "drag down"
                REL_DRAG -> "release drag"
                SPRING_UP -> "spring up"
                POP_BALL -> "pop ball"
                OUTER_CIR -> "outer circle"
                REFRESHING -> "refreshing..."
                DONE -> "done!"
                STOP -> "stop"
                else -> "unknown state"
            }
        }
    }

    private var mBackPaint: Paint? = null
    private var mBallPaint: Paint? = null
    private var mOutPaint: Paint? = null
    private var mPath: Path = Path()

    constructor(context: Context) : this(context, null, 0) {}
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {}

    private fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        PULL_HEIGHT = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            100f,
            context.getResources().getDisplayMetrics()
        ).toInt()
        PULL_DELTA = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            50f,
            context.getResources().getDisplayMetrics()
        ).toInt()
        mWidthOffset = 0.5f
        mBackPaint = Paint()
        mBackPaint?.setAntiAlias(true)
        mBackPaint?.setStyle(Paint.Style.FILL)
        mBackPaint?.setColor(-0x746f51)
        mBallPaint = Paint()
        mBallPaint?.setAntiAlias(true)
        mBallPaint?.setColor(-0x1)
        mBallPaint?.setStyle(Paint.Style.FILL)
        mOutPaint = Paint()
        mOutPaint?.setAntiAlias(true)
        mOutPaint?.setColor(-0x1)
        mOutPaint?.setStyle(Paint.Style.STROKE)
        mOutPaint?.setStrokeWidth(5F)
        mPath = Path()
    }

    private var mRadius = 0
    private var mWidth = 0
    private var mHeight = 0
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpec = heightMeasureSpec
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (height > PULL_DELTA + PULL_HEIGHT) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                PULL_DELTA + PULL_HEIGHT,
                MeasureSpec.getMode(heightMeasureSpec)
            )
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    protected override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            mRadius = getHeight() / 6
            mWidth = getWidth()
            mHeight = getHeight()
            if (mHeight < PULL_HEIGHT) {
                mAniStatus = AnimatorStatus.PULL_DOWN
            }
            when (mAniStatus) {
                AnimatorStatus.PULL_DOWN -> if (mHeight >= PULL_HEIGHT) {
                    mAniStatus = AnimatorStatus.DRAG_DOWN
                }
                AnimatorStatus.REL_DRAG -> {}
                else -> {}
            }
        }
    }

    protected override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (mAniStatus) {
            AnimatorStatus.PULL_DOWN -> canvas.drawRect(0F, 0F, mWidth.toFloat(), mHeight.toFloat(), mBackPaint!!)
            AnimatorStatus.REL_DRAG, AnimatorStatus.DRAG_DOWN -> drawDrag(canvas)
            AnimatorStatus.SPRING_UP -> {
                drawSpring(canvas, springDelta)
                invalidate()
            }
            AnimatorStatus.POP_BALL -> {
                drawPopBall(canvas)
                invalidate()
            }
            AnimatorStatus.OUTER_CIR -> {
                drawOutCir(canvas)
                invalidate()
            }
            AnimatorStatus.REFRESHING -> {
                drawRefreshing(canvas)
                invalidate()
            }
            AnimatorStatus.DONE -> {
                drawDone(canvas)
                invalidate()
            }
            AnimatorStatus.STOP -> drawDone(canvas)
        }
        if (mAniStatus == AnimatorStatus.REL_DRAG) {
            val params: ViewGroup.LayoutParams = getLayoutParams()
            var height: Int
            do {
                height = relHeight
            } while (height == mLastHeight && relRatio != 1f)
            mLastHeight = height
            params.height = PULL_HEIGHT + height
            requestLayout()
        }
    }

    private fun drawDrag(canvas: Canvas) {
        canvas.drawRect(0f, 0F, mWidth.toFloat(), PULL_HEIGHT.toFloat(), mBackPaint!!)
        mPath.reset()
        mPath.moveTo(0f, PULL_HEIGHT.toFloat())
        mPath.quadTo(
            mWidthOffset * mWidth, PULL_HEIGHT.toFloat() + (mHeight.toFloat() - PULL_HEIGHT.toFloat()) * 2,
            mWidth.toFloat(), PULL_HEIGHT.toFloat()
        )
        canvas.drawPath(mPath, mBackPaint!!)
    }

    private fun drawSpring(canvas: Canvas, springDelta: Int) {
        mPath.reset()
        mPath.moveTo(0f, 0f)
        mPath.lineTo(0f, PULL_HEIGHT.toFloat())
        mPath.quadTo(
            mWidth.toFloat() / 2, PULL_HEIGHT.toFloat() - springDelta.toFloat(),
            mWidth.toFloat(), PULL_HEIGHT.toFloat()
        )
        mPath.lineTo(mWidth.toFloat(), 0f)
        canvas.drawPath(mPath, mBackPaint!!)
        val curH = PULL_HEIGHT - springDelta / 2
        if (curH > PULL_HEIGHT - PULL_DELTA / 2) {
            val leftX = (mWidth / 2 - 2 * mRadius + sprRatio * mRadius).toInt()
            mPath.reset()
            mPath.moveTo(leftX.toFloat(), curH.toFloat())
            mPath.quadTo(
                mWidth.toFloat() / 2, curH - mRadius * sprRatio * 2,
                mWidth.toFloat() - leftX.toFloat(), curH.toFloat()
            )
            canvas.drawPath(mPath, mBallPaint!!)
        } else {
            canvas.drawArc(
                RectF(
                    (mWidth / 2 - mRadius).toFloat(),
                    (curH - mRadius).toFloat(),
                    (mWidth / 2 + mRadius).toFloat(),
                    (curH + mRadius).toFloat()
                ),
                180f, 180f, true, mBallPaint!!
            )
        }
    }

    private fun drawPopBall(canvas: Canvas) {
        mPath.reset()
        mPath.moveTo(0f, 0f)
        mPath.lineTo(0f, PULL_HEIGHT.toFloat())
        mPath.quadTo(
            mWidth.toFloat() / 2, PULL_HEIGHT.toFloat() - PULL_DELTA.toFloat(),
            mWidth.toFloat(), PULL_HEIGHT.toFloat()
        )
        mPath.lineTo(mWidth.toFloat(), 0f)
        canvas.drawPath(mPath, mBackPaint!!)
        val cirCentStart = PULL_HEIGHT - PULL_DELTA / 2
        val cirCenY = (cirCentStart - mRadius * 2 * popRatio).toInt()
        canvas.drawArc(
            RectF(
                (mWidth / 2 - mRadius).toFloat(),
                (cirCenY - mRadius).toFloat(),
                (mWidth / 2 + mRadius).toFloat(),
                (cirCenY + mRadius).toFloat()
            ),
            180f, 360f, true, mBallPaint!!
        )
        if (popRatio < 1) {
            drawTail(canvas, cirCenY, cirCentStart + 1, popRatio)
        } else {
            canvas.drawCircle(mWidth.toFloat() / 2, cirCenY.toFloat(), mRadius.toFloat(), mBallPaint!!)
        }
    }

    private fun drawTail(canvas: Canvas, centerY: Int, bottom: Int, fraction: Float) {
        val bezier1w = (mWidth / 2 + mRadius * 3 / 4 * (1 - fraction)).toInt()
        val start = PointF((mWidth / 2 + mRadius).toFloat(), centerY.toFloat())
        val bezier1 = PointF(bezier1w.toFloat(), bottom.toFloat())
        val bezier2 = PointF((bezier1w + mRadius / 2).toFloat(), bottom.toFloat())
        mPath.reset()
        mPath.moveTo(start.x, start.y)
        mPath.quadTo(
            bezier1.x, bezier1.y,
            bezier2.x, bezier2.y
        )
        mPath.lineTo(mWidth - bezier2.x, bezier2.y)
        mPath.quadTo(
            mWidth - bezier1.x, bezier1.y,
            mWidth - start.x, start.y
        )
        canvas.drawPath(mPath, mBallPaint!!)
    }

    private fun drawOutCir(canvas: Canvas) {
        mPath.reset()
        mPath.moveTo(0f, 0f)
        mPath.lineTo(0f, PULL_HEIGHT.toFloat())
        mPath.quadTo(
            mWidth.toFloat() / 2, PULL_HEIGHT - (1 - outRatio) * PULL_DELTA,
            mWidth.toFloat(), PULL_HEIGHT.toFloat()
        )
        mPath.lineTo(mWidth.toFloat(), 0f)
        canvas.drawPath(mPath, mBackPaint!!)
        val innerY = PULL_HEIGHT - PULL_DELTA / 2 - mRadius * 2
        canvas.drawCircle(mWidth.toFloat() / 2, innerY.toFloat(), mRadius.toFloat(), mBallPaint!!)
    }

    private var mRefreshStart = 90
    private var mRefreshStop = 90
    private var TARGET_DEGREE = 270
    private var mIsStart = true
    private var mIsRefreshing = true
    private fun drawRefreshing(canvas: Canvas) {
        canvas.drawRect(0f, 0f, mWidth.toFloat(), mHeight.toFloat(), mBackPaint!!)
        val innerY = PULL_HEIGHT - PULL_DELTA / 2 - mRadius * 2
        canvas.drawCircle(mWidth.toFloat() / 2, innerY.toFloat(), mRadius.toFloat(), mBallPaint!!)
        val outerR = mRadius + 10
        mRefreshStart += if (mIsStart) 3 else 10
        mRefreshStop += if (mIsStart) 10 else 3
        mRefreshStart = mRefreshStart % 360
        mRefreshStop = mRefreshStop % 360
        var swipe = mRefreshStop - mRefreshStart
        swipe = if (swipe < 0) swipe + 360 else swipe
        canvas.drawArc(
            RectF(
                (mWidth / 2 - outerR).toFloat(),
                (innerY - outerR).toFloat(),
                (mWidth / 2 + outerR).toFloat(),
                (innerY + outerR).toFloat()
            ),
            mRefreshStart.toFloat(), swipe.toFloat(), false, mOutPaint!!
        )
        if (swipe >= TARGET_DEGREE) {
            mIsStart = false
        } else if (swipe <= 10) {
            mIsStart = true
        }
        if (!mIsRefreshing) {
            applyDone()
        }
    }

    fun setRefreshing(isFresh: Boolean) {
        mIsRefreshing = isFresh
    }

    private fun drawDone(canvas: Canvas) {
        val beforeColor: Int = mOutPaint!!.getColor()
        if (doneRatio < 0.3) {
            canvas.drawRect(0f, 0f, mWidth.toFloat(), mHeight.toFloat(), mBackPaint!!)
            val innerY = PULL_HEIGHT - PULL_DELTA / 2 - mRadius * 2
            canvas.drawCircle(mWidth.toFloat() / 2, innerY.toFloat(), mRadius.toFloat(), mBallPaint!!)
            val outerR = (mRadius + 10 + 10 * doneRatio / 0.3f).toInt()
            val afterColor: Int = Color.argb(
                (0xff * (1 - doneRatio / 0.3f)).toInt(), Color.red(beforeColor),
                Color.green(beforeColor), Color.blue(beforeColor)
            )
            mOutPaint!!.setColor(afterColor)
            canvas.drawArc(
                RectF(
                    (mWidth / 2 - outerR).toFloat(),
                    (innerY - outerR).toFloat(),
                    (mWidth / 2 + outerR).toFloat(),
                    (innerY + outerR).toFloat()
                ),
                0f, 360f, false, mOutPaint!!
            )
        }
        mOutPaint!!.setColor(beforeColor)
        if (doneRatio >= 0.3 && doneRatio < 0.7) {
            canvas.drawRect(0f, 0f, mWidth.toFloat(), mHeight.toFloat(), mBackPaint!!)
            val fraction = (doneRatio - 0.3f) / 0.4f
            val startCentY = PULL_HEIGHT - PULL_DELTA / 2 - mRadius * 2
            val curCentY = (startCentY + (PULL_DELTA / 2 + mRadius * 2) * fraction).toInt()
            canvas.drawCircle(mWidth.toFloat() / 2, curCentY.toFloat(), mRadius.toFloat(), mBallPaint!!)
            if (curCentY >= PULL_HEIGHT - mRadius * 2) {
                drawTail(canvas, curCentY, PULL_HEIGHT, 1 - fraction)
            }
        }
        if (doneRatio >= 0.7 && doneRatio <= 1) {
            val fraction = (doneRatio - 0.7f) / 0.3f
            canvas.drawRect(0f, 0f, mWidth.toFloat(), mHeight.toFloat(), mBackPaint!!)
            val leftX = (mWidth / 2 - mRadius - 2 * mRadius * fraction).toInt()
            mPath.reset()
            mPath.moveTo(leftX.toFloat(), PULL_HEIGHT.toFloat())
            mPath.quadTo(
                mWidth.toFloat() / 2, PULL_HEIGHT - mRadius * (1 - fraction),
                mWidth.toFloat() - leftX.toFloat(), PULL_HEIGHT.toFloat()
            )
            canvas.drawPath(mPath, mBallPaint!!)
        }
    }

    private var mLastHeight = 0
    private val relHeight: Int
        private get() = (mSpriDeta * (1 - relRatio)).toInt()
    private val springDelta: Int
        private get() = (PULL_DELTA * sprRatio).toInt()
    private var mStart: Long = 0
    private var mStop: Long = 0
    private var mSpriDeta = 0
    fun releaseDrag() {
        mStart = System.currentTimeMillis()
        mStop = mStart + REL_DRAG_DUR
        mAniStatus = AnimatorStatus.REL_DRAG
        mSpriDeta = mHeight - PULL_HEIGHT
        requestLayout()
    }

    private val relRatio: Float
        private get() {
            if (System.currentTimeMillis() >= mStop) {
                springUp()
                return 1F
            }
            val ratio = (System.currentTimeMillis() - mStart) / REL_DRAG_DUR.toFloat()
            return Math.min(ratio, 1f)
        }
    private var mSprStart: Long = 0
    private var mSprStop: Long = 0
    private fun springUp() {
        mSprStart = System.currentTimeMillis()
        mSprStop = mSprStart + SPRING_DUR
        mAniStatus = AnimatorStatus.SPRING_UP
        invalidate()
    }

    private val sprRatio: Float
        private get() {
            if (System.currentTimeMillis() >= mSprStop) {
                popBall()
                return 1f
            }
            val ratio = (System.currentTimeMillis() - mSprStart) / SPRING_DUR.toFloat()
            return Math.min(1f, ratio)
        }
    private var mPopStart: Long = 0
    private var mPopStop: Long = 0
    private fun popBall() {
        mPopStart = System.currentTimeMillis()
        mPopStop = mPopStart + POP_BALL_DUR
        mAniStatus = AnimatorStatus.POP_BALL
        invalidate()
    }

    private val popRatio: Float
        private get() {
            if (System.currentTimeMillis() >= mPopStop) {
                startOutCir()
                return 1f
            }
            val ratio = (System.currentTimeMillis() - mPopStart) / POP_BALL_DUR.toFloat()
            return Math.min(ratio, 1f)
        }
    private var mOutStart: Long = 0
    private var mOutStop: Long = 0
    private fun startOutCir() {
        mOutStart = System.currentTimeMillis()
        mOutStop = mOutStart + OUTER_DUR
        mAniStatus = AnimatorStatus.OUTER_CIR
        mRefreshStart = 90
        mRefreshStop = 90
        TARGET_DEGREE = 270
        mIsStart = true
        mIsRefreshing = true
        invalidate()
    }

    private val outRatio: Float
        private get() {
            if (System.currentTimeMillis() >= mOutStop) {
                mAniStatus = AnimatorStatus.REFRESHING
                mIsRefreshing = true
                return 1f
            }
            val ratio = (System.currentTimeMillis() - mOutStart) / OUTER_DUR.toFloat()
            return Math.min(ratio, 1f)
        }
    private var mDoneStart: Long = 0
    private var mDoneStop: Long = 0
    private fun applyDone() {
        mDoneStart = System.currentTimeMillis()
        mDoneStop = mDoneStart + DONE_DUR
        mAniStatus = AnimatorStatus.DONE
    }

    private val doneRatio: Float
        private get() {
            if (System.currentTimeMillis() >= mDoneStop) {
                mAniStatus = AnimatorStatus.STOP
                if (onViewAniDone != null) {
                    onViewAniDone!!.viewAniDone()
                }
                return 1f
            }
            val ratio = (System.currentTimeMillis() - mDoneStart) / DONE_DUR.toFloat()
            return Math.min(ratio, 1f)
        }
    private var onViewAniDone: OnViewAniDone? = null

    init {
        initView(context, attrs, defStyleAttr)
    }

    fun setOnViewAniDone(onViewAniDone: OnViewAniDone) {
        this.onViewAniDone = onViewAniDone
    }

    interface OnViewAniDone {
        fun viewAniDone()
    }

    fun setAniBackColor(color: Int) {
        mBackPaint!!.setColor(color)
    }

    fun setAniForeColor(color: Int) {
        mBallPaint!!.setColor(color)
        mOutPaint!!.setColor(color)
        setBackgroundColor(color)
    }

    fun setRadius(smallTimes: Int) {
        mRadius = mHeight / smallTimes
    }

    companion object {
        private const val TAG = "com.ardakazanci.softrefresh.SoftView"
        private const val REL_DRAG_DUR: Long = 200
        private const val SPRING_DUR: Long = 200
        private const val POP_BALL_DUR: Long = 300
        private const val OUTER_DUR: Long = 200
        private const val DONE_DUR: Long = 1000
    }
}