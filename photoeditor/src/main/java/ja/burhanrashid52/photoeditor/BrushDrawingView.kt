package ja.burhanrashid52.photoeditor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.support.annotation.ColorInt
import android.support.annotation.IntRange
import android.support.annotation.VisibleForTesting
import android.util.AttributeSet
import android.util.Pair
import android.view.MotionEvent
import android.view.View

import java.util.Stack
import kotlin.math.abs

/**
 *
 *
 * This is custom drawing view used to do painting on user touch events it it will paint on canvas
 * as per attributes provided to the paint
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 12/1/18
 */
class BrushDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var brushSize = DEFAULT_BRUSH_SIZE
        set(size) {
            field = size
            brushDrawingMode = true
        }
    private var eraserSize = DEFAULT_ERASER_SIZE
        private set
    private var opacity = DEFAULT_OPACITY
        set(@IntRange(from = 0, to = 255) opacity) {
            field = opacity
            brushDrawingMode = true
        }

    private val mDrawnPaths = Stack<LinePath>()
    private val mRedoPaths = Stack<LinePath>()
    @get:VisibleForTesting
    internal val drawingPaint = Paint()

    private var mDrawCanvas: Canvas? = null
    private var mBrushDrawMode: Boolean = false

    private var mPath: Path? = null
    private var mTouchX: Float = 0.toFloat()
    private var mTouchY: Float = 0.toFloat()

    private var mBrushViewChangeListener: BrushViewChangeListener? = null

    internal var brushDrawingMode: Boolean
        get() = mBrushDrawMode
        set(brushDrawMode) {
            this.mBrushDrawMode = brushDrawMode
            if (brushDrawMode) {
                this.visibility = View.VISIBLE
                refreshBrushDrawing()
            }
        }

    internal var brushColor: Int
        get() = drawingPaint.color
        set(@ColorInt color) {
            drawingPaint.color = color
            brushDrawingMode = true
        }

    internal val drawingPath: Pair<Stack<LinePath>, Stack<LinePath>>
        @VisibleForTesting
        get() = Pair(mDrawnPaths, mRedoPaths)

    init {
        setupBrushDrawing()
    }

    private fun setupBrushDrawing() {
        //Caution: This line is to disable hardware acceleration to make eraser feature work properly
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        drawingPaint.color = Color.BLACK
        setupPathAndPaint()
        visibility = View.GONE
    }

    private fun setupPathAndPaint() {
        mPath = Path()
        drawingPaint.isAntiAlias = true
        drawingPaint.isDither = true
        drawingPaint.style = Paint.Style.STROKE
        drawingPaint.strokeJoin = Paint.Join.ROUND
        drawingPaint.strokeCap = Paint.Cap.ROUND
        drawingPaint.strokeWidth = brushSize
        drawingPaint.alpha = opacity
        drawingPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    private fun refreshBrushDrawing() {
        mBrushDrawMode = true
        setupPathAndPaint()
    }

    internal fun brushEraser() {
        mBrushDrawMode = true
        drawingPaint.strokeWidth = eraserSize
        drawingPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    internal fun setBrushEraserSize(brushEraserSize: Float) {
        this.eraserSize = brushEraserSize
        brushDrawingMode = true
    }

    internal fun setBrushEraserColor(@ColorInt color: Int) {
        drawingPaint.color = color
        brushDrawingMode = true
    }

    internal fun clearAll() {
        mDrawnPaths.clear()
        mRedoPaths.clear()
        mDrawCanvas?.let {
            it.drawColor(0, PorterDuff.Mode.CLEAR)
        }
        invalidate()
    }

    internal fun setBrushViewChangeListener(brushViewChangeListener: BrushViewChangeListener) {
        mBrushViewChangeListener = brushViewChangeListener
    }

    public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mDrawCanvas = Canvas(canvasBitmap)
    }

    public override fun onDraw(canvas: Canvas) {
        for (linePath in mDrawnPaths) {
            canvas.drawPath(linePath.drawPath, linePath.drawPaint)
        }
        mPath?.let { canvas.drawPath(it, drawingPaint) }
    }

    /**
     * Handle touch event to draw paint on canvas i.e brush drawing
     *
     * @param event points having touch info
     * @return true if handling touch events
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (mBrushDrawMode) {
            val touchX = event.x
            val touchY = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> touchStart(touchX, touchY)
                MotionEvent.ACTION_MOVE -> touchMove(touchX, touchY)
                MotionEvent.ACTION_UP -> touchUp()
            }
            invalidate()
            true
        } else {
            false
        }
    }

    internal fun undo(): Boolean {
        if (!mDrawnPaths.empty()) {
            mRedoPaths.push(mDrawnPaths.pop())
            invalidate()
        }
        mBrushViewChangeListener?.apply {
            onViewRemoved(this@BrushDrawingView)
        }

        return !mDrawnPaths.empty()
    }

    internal fun redo(): Boolean {
        if (!mRedoPaths.empty()) {
            mDrawnPaths.push(mRedoPaths.pop())
            invalidate()
        }

        if (mBrushViewChangeListener != null) {

            mBrushViewChangeListener?.apply {
                onViewAdd(this@BrushDrawingView)
            }
        }
        return !mRedoPaths.empty()
    }


    private fun touchStart(x: Float, y: Float) {
        mRedoPaths.clear()
        mPath?.reset()
        mPath?.moveTo(x, y)
        mTouchX = x
        mTouchY = y
        mBrushViewChangeListener?.apply {
            onStartDrawing()
        }

    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - mTouchX)
        val dy = abs(y - mTouchY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath?.quadTo(mTouchX, mTouchY, (x + mTouchX) / 2, (y + mTouchY) / 2)
            mTouchX = x
            mTouchY = y
        }
    }

    private fun touchUp() {
        mPath?.lineTo(mTouchX, mTouchY)
        // Commit the path to our offscreen
        mPath?.let { mDrawCanvas?.drawPath(it, drawingPaint) }
        // kill this so we don't double draw
        mDrawnPaths.push(LinePath(mPath, drawingPaint))
        mPath = Path()
        mBrushViewChangeListener?.apply {
            onStopDrawing()
            onViewAdd(this@BrushDrawingView)
        }

    }

    companion object {

        internal const val DEFAULT_BRUSH_SIZE = 25.0f
        internal const val DEFAULT_ERASER_SIZE = 50.0f
        internal const val DEFAULT_OPACITY = 255
        private const val TOUCH_TOLERANCE = 4f
    }
}