package ja.burhanrashid52.photoeditor

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.annotation.RequiresApi
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout

/**
 *
 *
 * This ViewGroup will have the [BrushDrawingView] to draw paint on it with [ImageView]
 * which our source image
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 1/18/2018
 */

class PhotoEditorView : RelativeLayout {

    private var mImgSource: FilterImageView? = null
    internal var brushDrawingView: BrushDrawingView? = null
        private set
    private var mImageFilterView: ImageFilterView? = null


    /**
     * Source image which you want to edit
     *
     * @return source ImageView
     */
    val source: ImageView?
        get() = mImgSource

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(attrs)
    }

    @SuppressLint("Recycle")
    private fun init(attrs: AttributeSet?) {
        //Setup image attributes
        mImgSource = FilterImageView(context)
        mImgSource?.id = imgSrcId
        mImgSource?.adjustViewBounds = true
        val imgSrcParam = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        imgSrcParam.addRule(CENTER_IN_PARENT, TRUE)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.PhotoEditorView)
            val imgSrcDrawable = a.getDrawable(R.styleable.PhotoEditorView_photo_src)
            if (imgSrcDrawable != null) {
                mImgSource?.setImageDrawable(imgSrcDrawable)
            }
        }

        //Setup brush view
        brushDrawingView = BrushDrawingView(context)
        brushDrawingView?.visibility = View.GONE
        brushDrawingView?.id = brushSrcId
        //Align brush to the size of image view
        val brushParam = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        brushParam.addRule(CENTER_IN_PARENT, TRUE)
        brushParam.addRule(ALIGN_TOP, imgSrcId)
        brushParam.addRule(ALIGN_BOTTOM, imgSrcId)

        //Setup GLSurface attributes
        mImageFilterView = ImageFilterView(context)
        mImageFilterView?.id = glFilterId
        mImageFilterView?.visibility = View.GONE

        //Align brush to the size of image view
        val imgFilterParam = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        imgFilterParam.addRule(CENTER_IN_PARENT, TRUE)
        imgFilterParam.addRule(ALIGN_TOP, imgSrcId)
        imgFilterParam.addRule(ALIGN_BOTTOM, imgSrcId)

        mImgSource?.setOnImageChangedListener(object : FilterImageView.OnImageChangedListener {
            override fun onBitmapLoaded(sourceBitmap: Bitmap?) {
                mImageFilterView?.apply {
                    setFilterEffect(PhotoFilter.NONE)
                    sourceBitmap?.let { setSourceBitmap(it) }
                }

                Log.d(TAG, "onBitmapLoaded() called with: sourceBitmap = [$sourceBitmap]")
            }
        })


        //Add image source
        addView(mImgSource, imgSrcParam)

        //Add Gl FilterView
        addView(mImageFilterView, imgFilterParam)

        //Add brush view
        addView(brushDrawingView, brushParam)

    }


    internal fun saveFilter(onSaveBitmap: OnSaveBitmap) {
        if (mImageFilterView?.visibility == View.VISIBLE) {
            mImageFilterView?.saveBitmap(object : OnSaveBitmap {
                override fun onBitmapReady(saveBitmap: Bitmap) {
                    Log.e(TAG, "saveFilter: $saveBitmap")
                    mImgSource?.setImageBitmap(saveBitmap)
                    mImageFilterView?.visibility = View.GONE
                    onSaveBitmap.onBitmapReady(saveBitmap)
                }

                override fun onFailure(e: Exception) {
                    onSaveBitmap.onFailure(e)
                }
            })
        } else {
            mImgSource?.bitmap?.let { onSaveBitmap.onBitmapReady(it) }
        }


    }

    internal fun setFilterEffect(filterType: PhotoFilter) {
        mImageFilterView?.apply {
            visibility = View.VISIBLE
            mImgSource?.bitmap?.let { setSourceBitmap(it) }
            setFilterEffect(filterType)
        }

    }

    internal fun setFilterEffect(customEffect: CustomEffect) {
        mImageFilterView?.apply {
            visibility = View.VISIBLE
            mImgSource?.bitmap?.let { setSourceBitmap(it) }
            setFilterEffect(customEffect)
        }

    }

    companion object {

        private val TAG = "PhotoEditorView"
        private val imgSrcId = 1
        private val brushSrcId = 2
        private val glFilterId = 3
    }
}
