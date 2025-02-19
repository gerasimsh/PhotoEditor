package ja.burhanrashid52.photoeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.effect.Effect
import android.media.effect.EffectContext
import android.media.effect.EffectFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.media.effect.EffectFactory.*
import ja.burhanrashid52.photoeditor.PhotoFilter.*

/**
 *
 *
 * Filter Images using ImageFilterView
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.2
 * @since 2/14/2018
 */
internal class ImageFilterView : GLSurfaceView, GLSurfaceView.Renderer {
    private val mTextures = IntArray(2)
    private var mEffectContext: EffectContext? = null
    private var mEffect: Effect? = null
    private val mTexRenderer = TextureRenderer()
    private var mImageWidth: Int = 0
    private var mImageHeight: Int = 0
    private var mInitialized = false
    private var mCurrentEffect: PhotoFilter? = null
    private var mSourceBitmap: Bitmap? = null
    private var mCustomEffect: CustomEffect? = null
    private var mOnSaveBitmap: OnSaveBitmap? = null
    private var isSaveImage = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        setFilterEffect(NONE)
    }

    fun setSourceBitmap(sourceBitmap: Bitmap) {
        /* if (mSourceBitmap != null && mSourceBitmap.sameAs(sourceBitmap)) {
            //mCurrentEffect = NONE;
        }*/
        mSourceBitmap = sourceBitmap
        mInitialized = false
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {

    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        mTexRenderer?.updateViewSize(width, height)
    }

    override fun onDrawFrame(gl: GL10) {

        if (!mInitialized) {
            //Only need to do this once
            mEffectContext = EffectContext.createWithCurrentGlContext()
            mTexRenderer.init()
            loadTextures()
            mInitialized = true
        }
        if (mCurrentEffect != NONE || mCustomEffect != null) {
            //if an effect is chosen initialize it and apply it to the texture
            initEffect()
            applyEffect()
        }
        renderResult()
        if (isSaveImage) {
            val mFilterBitmap = BitmapUtil.createBitmapFromGLSurface(this, gl)
            Log.e(TAG, "onDrawFrame: $mFilterBitmap")
            isSaveImage = false
            if (mOnSaveBitmap != null) {
                Handler(Looper.getMainLooper()).post {
                    mFilterBitmap?.let {
                        mOnSaveBitmap?.onBitmapReady(
                            it
                        )
                    }
                }
            }
        }
    }

    fun setFilterEffect(effect: PhotoFilter) {
        mCurrentEffect = effect
        mCustomEffect = null
        requestRender()
    }

    fun setFilterEffect(customEffect: CustomEffect) {
        mCustomEffect = customEffect
        requestRender()
    }


    fun saveBitmap(onSaveBitmap: OnSaveBitmap) {
        mOnSaveBitmap = onSaveBitmap
        isSaveImage = true
        requestRender()
    }

    private fun loadTextures() {
        // Generate textures
        GLES20.glGenTextures(2, mTextures, 0)

        // Load input bitmap
        mSourceBitmap?.let { sourseBitmap ->
            mImageWidth = sourseBitmap.width
            mImageHeight = sourseBitmap.height
            mTexRenderer.updateTextureSize(mImageWidth, mImageHeight)

            // Upload to texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0])
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, sourseBitmap, 0)

            // Set texture parameters
            GLToolbox.initTexParams()
        }

    }

    private fun initEffect() {
        val effectFactory = mEffectContext!!.factory
        if (mEffect != null) {
            mEffect!!.release()
        }
        if (mCustomEffect != null) {
            mEffect = effectFactory.createEffect(mCustomEffect?.effectName)
            val parameters = mCustomEffect?.parameters
            if (parameters != null) {
                for ((key, value) in parameters) {
                    mEffect!!.setParameter(key, value)
                }
            }
        } else {
            // Initialize the correct effect based on the selected menu/action item
            when (mCurrentEffect) {

                AUTO_FIX -> {
                    mEffect = effectFactory.createEffect(EFFECT_AUTOFIX)
                    mEffect!!.setParameter("scale", 0.5f)
                }
                BLACK_WHITE -> {
                    mEffect = effectFactory.createEffect(EFFECT_BLACKWHITE)
                    mEffect!!.setParameter("black", .1f)
                    mEffect!!.setParameter("white", .7f)
                }
                BRIGHTNESS -> {
                    mEffect = effectFactory.createEffect(EFFECT_BRIGHTNESS)
                    mEffect!!.setParameter("brightness", 2.0f)
                }
                CONTRAST -> {
                    mEffect = effectFactory.createEffect(EFFECT_CONTRAST)
                    mEffect!!.setParameter("contrast", 1.4f)
                }
                CROSS_PROCESS -> mEffect = effectFactory.createEffect(EFFECT_CROSSPROCESS)
                DOCUMENTARY -> mEffect = effectFactory.createEffect(EFFECT_DOCUMENTARY)
                DUE_TONE -> {
                    mEffect = effectFactory.createEffect(EFFECT_DUOTONE)
                    mEffect!!.setParameter("first_color", Color.YELLOW)
                    mEffect!!.setParameter("second_color", Color.DKGRAY)
                }
                FILL_LIGHT -> {
                    mEffect = effectFactory.createEffect(EFFECT_FILLLIGHT)
                    mEffect!!.setParameter("strength", .8f)
                }
                FISH_EYE -> {
                    mEffect = effectFactory.createEffect(EFFECT_FISHEYE)
                    mEffect!!.setParameter("scale", .5f)
                }
                FLIP_HORIZONTAL -> {
                    mEffect = effectFactory.createEffect(EFFECT_FLIP)
                    mEffect!!.setParameter("horizontal", true)
                }
                FLIP_VERTICAL -> {
                    mEffect = effectFactory.createEffect(EFFECT_FLIP)
                    mEffect!!.setParameter("vertical", true)
                }
                GRAIN -> {
                    mEffect = effectFactory.createEffect(EFFECT_GRAIN)
                    mEffect!!.setParameter("strength", 1.0f)
                }
                GRAY_SCALE -> mEffect = effectFactory.createEffect(EFFECT_GRAYSCALE)
                LOMISH -> mEffect = effectFactory.createEffect(EFFECT_LOMOISH)
                NEGATIVE -> mEffect = effectFactory.createEffect(EFFECT_NEGATIVE)
                NONE -> {
                }
                POSTERIZE -> mEffect = effectFactory.createEffect(EFFECT_POSTERIZE)
                ROTATE -> {
                    mEffect = effectFactory.createEffect(EFFECT_ROTATE)
                    mEffect!!.setParameter("angle", 180)
                }
                SATURATE -> {
                    mEffect = effectFactory.createEffect(EFFECT_SATURATE)
                    mEffect!!.setParameter("scale", .5f)
                }
                SEPIA -> mEffect = effectFactory.createEffect(EFFECT_SEPIA)
                SHARPEN -> mEffect = effectFactory.createEffect(EFFECT_SHARPEN)
                TEMPERATURE -> {
                    mEffect = effectFactory.createEffect(EFFECT_TEMPERATURE)
                    mEffect!!.setParameter("scale", .9f)
                }
                TINT -> {
                    mEffect = effectFactory.createEffect(EFFECT_TINT)
                    mEffect!!.setParameter("tint", Color.MAGENTA)
                }
                VIGNETTE -> {
                    mEffect = effectFactory.createEffect(EFFECT_VIGNETTE)
                    mEffect!!.setParameter("scale", .5f)
                }
            }
        }
    }

    private fun applyEffect() {
        mEffect!!.apply(mTextures[0], mImageWidth, mImageHeight, mTextures[1])
    }

    private fun renderResult() {
        if (mCurrentEffect != NONE || mCustomEffect != null) {
            // if no effect is chosen, just render the original bitmap
            mTexRenderer.renderTexture(mTextures[1])
        } else {
            // render the result of applyEffect()
            mTexRenderer.renderTexture(mTextures[0])
        }
    }

    companion object {

        private val TAG = "ImageFilterView"
    }
}
