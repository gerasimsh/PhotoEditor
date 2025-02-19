package ja.burhanrashid52.photoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.AsyncTask
import android.support.annotation.ColorInt
import android.support.annotation.IntRange
import android.support.annotation.RequiresPermission
import android.support.annotation.UiThread
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 *
 *
 * This class in initialize by [PhotoEditor.Builder] using a builder pattern with multiple
 * editing attributes
 *
 *
 * @author [Burhanuddin Rashid](https://github.com/burhanrashid52)
 * @version 0.1.1
 * @since 18/01/2017
 */
class PhotoEditor private constructor(builder: Builder) : BrushViewChangeListener {
    private val mLayoutInflater: LayoutInflater
    private val context: Context
    private val parentView: PhotoEditorView?
    private val imageView: ImageView?
    private val deleteView: View?
    private val brushDrawingView: BrushDrawingView?
    private val addedViews: MutableList<ViewParam>
    private val redoViews: MutableList<View>
    private var mOnPhotoEditorListener: OnPhotoEditorListener? = null
    private val isTextPinchZoomable: Boolean
    private val mDefaultTextTypeface: Typeface?
    private val mDefaultEmojiTypeface: Typeface?

    /**
     * Create a new instance and scalable touchview
     *
     * @return scalable multitouch listener
     */
    private//multiTouchListener.setOnMultiTouchListener(this);
    val multiTouchListener: MultiTouchListener
        get() = MultiTouchListener(
            deleteView,
            parentView!!,
            imageView!!,
            isTextPinchZoomable,
            mOnPhotoEditorListener
        )

    /**
     * @return true is brush mode is enabled
     */
    val brushDrawableMode: Boolean?
        get() = brushDrawingView != null && brushDrawingView.brushDrawingMode

    /**
     * @return provide the size of eraser
     * @see PhotoEditor.setBrushEraserSize
     */
    val eraserSize: Float
        get() = (brushDrawingView?.eraserSize ?: 0F)

    /**
     * @return provide the size of eraser
     * @see PhotoEditor.setBrushSize
     */
    /**
     * set the size of bursh user want to paint on canvas i.e [BrushDrawingView]
     *
     * @param size size of brush
     */
    var brushSize: Float
        get() = brushDrawingView?.brushSize ?: 0f
        set(size) {
            if (brushDrawingView != null)
                brushDrawingView.brushSize = size
        }

    /**
     * @return provide the size of eraser
     * @see PhotoEditor.setBrushColor
     */
    /**
     * set brush color which user want to paint
     *
     * @param color color value for paint
     */
    var brushColor: Int
        get() = brushDrawingView?.brushColor ?: 0
        set(@ColorInt color) {
            brushDrawingView?.brushColor = color
        }

    /**
     * Check if any changes made need to save
     *
     * @return true if nothing is there to change
     */
    val isCacheEmpty: Boolean
        get() = addedViews.size == 0 && redoViews.size == 0

    val addedViewsCharacteristic: List<ViewCharacteristic>
        get() {
            val list = ArrayList<ViewCharacteristic>()
            for (viewParam in addedViews) {
                if (viewParam.view?.tag === ViewType.EMOJI || viewParam.view?.tag === ViewType.TEXT)
                    getViewCharacteristic(viewParam)?.let {
                        list.add(it)
                    }
            }
            return list
        }


    init {
        this.context = builder.context
        this.parentView = builder.parentView
        this.imageView = builder.imageView
        this.deleteView = builder.deleteView
        this.brushDrawingView = builder.brushDrawingView
        this.isTextPinchZoomable = builder.isTextPinchZoomable
        this.mDefaultTextTypeface = builder.textTypeface
        this.mDefaultEmojiTypeface = builder.emojiTypeface
        mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        brushDrawingView?.setBrushViewChangeListener(this)
        addedViews = ArrayList()
        redoViews = ArrayList()
    }

    /**
     * This will add image on [PhotoEditorView] which you drag,rotate and scale using pinch
     * if [PhotoEditor.Builder.setPinchTextScalable] enabled
     *
     * @param desiredImage bitmap image you want to add
     */
    fun addImage(desiredImage: Bitmap) {
        val viewParam = ViewParam()
        val imageRootView = getLayout(ViewType.IMAGE, viewParam)
        imageRootView?.let {
            val imageView = it.findViewById<ImageView>(R.id.imgPhotoEditorImage)
            val frmBorder = it.findViewById<FrameLayout>(R.id.frmBorder)
            val imgClose = it.findViewById<ImageView>(R.id.imgPhotoEditorClose)

            imageView.setImageBitmap(desiredImage)

            val multiTouchListener = multiTouchListener
            multiTouchListener.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
                override fun onClick() {
                    val isBackgroundVisible = frmBorder.tag != null && frmBorder.tag as Boolean
                    frmBorder.setBackgroundResource(if (isBackgroundVisible) 0 else R.drawable.rounded_border_tv)
                    imgClose.visibility = if (isBackgroundVisible) View.GONE else View.VISIBLE
                    frmBorder.tag = !isBackgroundVisible
                }

                override fun onLongClick() {

                }
            })

            imageRootView.setOnTouchListener(multiTouchListener)

            addViewToParent(ViewType.IMAGE, viewParam, null)
        }


    }

    private fun addViewToParent(
        viewType: ViewType,
        viewParam: ViewParam,
        viewCharacteristic: ViewCharacteristic?
    ) {
        val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)


        if (viewCharacteristic != null) {
            viewParam.view?.pivotX = viewCharacteristic.pivotX
            viewParam.view?.pivotY = viewCharacteristic.pivotY
            viewParam.view?.scaleX = viewCharacteristic.scaleX
            viewParam.view?.scaleY = viewCharacteristic.scaleY
            viewParam.view?.rotation = viewCharacteristic.rotation
            viewParam.view?.translationX = viewCharacteristic.coordX
            viewParam.view?.translationY = viewCharacteristic.coordY
        }
        parentView?.addView(viewParam.view, params)
        addedViews.add(viewParam)
        mOnPhotoEditorListener?.apply { onAddViewListener(viewType, addedViews.size) }
    }

    /**
     * This add the text on the [PhotoEditorView] with provided parameters
     * by default [TextView.setText] will be 18sp
     *
     * @param text              text to display
     * @param colorCodeTextView text color to be displayed
     */
    @SuppressLint("ClickableViewAccessibility")
    fun addText(text: String, colorCodeTextView: Int) {
        addText(null, text, colorCodeTextView)
    }

    /**
     * This add the text on the [PhotoEditorView] with provided parameters
     * by default [TextView.setText] will be 18sp
     *
     * @param textTypeface      typeface for custom font in the text
     * @param text              text to display
     * @param colorCodeTextView text color to be displayed
     */
    @SuppressLint("ClickableViewAccessibility")
    fun addText(textTypeface: Typeface?, text: String, colorCodeTextView: Int) {
        val styleBuilder = TextStyleBuilder()

        styleBuilder.withTextColor(colorCodeTextView)
        if (textTypeface != null) {
            styleBuilder.withTextFont(textTypeface)
        }

        addText(text, styleBuilder, textTypeface, colorCodeTextView)
    }

    /**
     * This add the text on the [PhotoEditorView] with provided parameters
     * by default [TextView.setText] will be 18sp
     *
     * @param text         text to display
     * @param styleBuilder text style builder with your style
     */
    @SuppressLint("ClickableViewAccessibility")
    fun addText(
        text: String,
        styleBuilder: TextStyleBuilder?,
        textTypeface: Typeface?,
        color: Int
    ) {
        val viewParam = ViewParam().apply {
            typeface = textTypeface
            textStyleBuilder = styleBuilder
            colorView = color
        }
        brushDrawingView?.brushDrawingMode = false
        val textRootView = getLayout(ViewType.TEXT, viewParam)
        textRootView?.let { rootView ->
            val textInputTv = rootView.findViewById<TextView>(R.id.tvPhotoEditorText)
            val imgClose = rootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            val frmBorder = rootView.findViewById<FrameLayout>(R.id.frmBorder)

            textInputTv?.let {
                it.text = text
                styleBuilder?.applyStyle(it)
            }

            val multiTouchListener = multiTouchListener
            multiTouchListener.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
                override fun onClick() {
                    val isBackgroundVisible = frmBorder?.tag != null && frmBorder.tag as Boolean
                    frmBorder?.setBackgroundResource(if (isBackgroundVisible) 0 else R.drawable.rounded_border_tv)
                    imgClose?.visibility = if (isBackgroundVisible) View.GONE else View.VISIBLE
                    frmBorder?.tag = !isBackgroundVisible
                }

                override fun onLongClick() {
                    textInputTv?.let {
                        val textInput = it.text.toString()
                        val currentTextColor = it.currentTextColor
                        mOnPhotoEditorListener?.apply {
                            onEditTextChangeListener(
                                rootView,
                                textInput,
                                currentTextColor
                            )

                        }
                    }

                }
            })

            rootView.setOnTouchListener(multiTouchListener)
            addViewToParent(ViewType.TEXT, viewParam, null)
        }

    }

    /**
     * This will update text and color on provided view
     *
     * @param view      view on which you want update
     * @param inputText text to update [TextView]
     * @param colorCode color to update on [TextView]
     */
    fun editText(view: View, inputText: String, colorCode: Int) {
        editText(view, null, inputText, colorCode)
    }

    /**
     * This will update the text and color on provided view
     *
     * @param view         root view where text view is a child
     * @param textTypeface update typeface for custom font in the text
     * @param inputText    text to update [TextView]
     * @param colorCode    color to update on [TextView]
     */
    fun editText(view: View, textTypeface: Typeface?, inputText: String, colorCode: Int) {
        val styleBuilder = TextStyleBuilder()
        styleBuilder.withTextColor(colorCode)
        if (textTypeface != null) {
            styleBuilder.withTextFont(textTypeface)
        }

        editTextTypeface(view, inputText, styleBuilder, textTypeface)
    }

    /**
     * This will update the text and color on provided view
     *
     * @param view         root view where text view is a child
     * @param inputText    text to update [TextView]
     * @param styleBuilder style to apply on [TextView]
     */
    fun editTextTypeface(
        view: View,
        inputText: String,
        styleBuilder: TextStyleBuilder?,
        textTypeface: Typeface?
    ) {
        //todo
        /*val inputTextView = view.findViewById<TextView>(R.id.tvPhotoEditorText)
        if (inputTextView != null && addedViews.contains(view) && !TextUtils.isEmpty(inputText)) {
            inputTextView.text = inputText
            styleBuilder?.applyStyle(inputTextView)

            parentView!!.updateViewLayout(view, view.layoutParams)
            val i = addedViews.indexOf(view)
            if (i > -1) addedViews[i] = ViewParam(view, textTypeface, styleBuilder)
        }*/
    }

    /**
     * Adds emoji to the [PhotoEditorView] which you drag,rotate and scale using pinch
     * if [PhotoEditor.Builder.setPinchTextScalable] enabled
     *
     * @param emojiName unicode in form of string to display emoji
     */
    fun addEmoji(emojiName: String) {
        addEmoji(null, emojiName)
    }

    /**
     * Adds emoji to the [PhotoEditorView] which you drag,rotate and scale using pinch
     * if [PhotoEditor.Builder.setPinchTextScalable] enabled
     *
     * @param emojiTypeface typeface for custom font to show emoji unicode in specific font
     * @param emojiName     unicode in form of string to display emoji
     */
    fun addEmoji(emojiTypeface: Typeface?, emojiName: String) {
        brushDrawingView?.brushDrawingMode = false
        val viewParam = ViewParam()
        val emojiRootView = getLayout(ViewType.EMOJI, viewParam)
        emojiRootView?.let { rootView ->
            val emojiTextView = rootView.findViewById<TextView>(R.id.tvPhotoEditorText)
            val frmBorder = rootView.findViewById<FrameLayout>(R.id.frmBorder)
            val imgClose = rootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)

            emojiTextView?.apply {
                typeface = emojiTypeface
                textSize = 56f
                text = emojiName
            }

            val multiTouchListener = multiTouchListener
            multiTouchListener.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
                override fun onClick() {
                    val isBackgroundVisible = frmBorder?.tag != null && frmBorder.tag as Boolean
                    frmBorder?.setBackgroundResource(if (isBackgroundVisible) 0 else R.drawable.rounded_border_tv)
                    imgClose?.visibility = if (isBackgroundVisible) View.GONE else View.VISIBLE
                    frmBorder?.tag = !isBackgroundVisible
                }

                override fun onLongClick() {}
            })
            rootView.setOnTouchListener(multiTouchListener)
            addViewToParent(ViewType.EMOJI, viewParam, null)

        }

    }


    /**
     * Add to root view from image,emoji and text to our parent view
     *
     * @param rootView     rootview of image,text and emoji
     * @param typeface
     * @param styleBuilder
     */
    private fun addViewToParent(
        rootView: View,
        viewType: ViewType,
        typeface: Typeface,
        styleBuilder: TextStyleBuilder,
        viewCharacteristic: ViewCharacteristic?
    ) {
        val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)

        parentView?.addView(rootView, params)
        if (viewCharacteristic != null) {
            rootView.rotation = viewCharacteristic.rotation
        }

        addedViews.add(ViewParam(rootView, typeface, styleBuilder))
        mOnPhotoEditorListener?.apply { onAddViewListener(viewType, addedViews.size) }
    }

    /**
     * Get root view by its type i.e image,text and emoji
     *
     * @param viewType  image,text or emoji
     * @param viewParam
     * @return rootview
     */
    private fun getLayout(viewType: ViewType, viewParam: ViewParam): View? {
        var rootView: View?
        when (viewType) {
            ViewType.TEXT -> {
                rootView = mLayoutInflater.inflate(R.layout.view_photo_editor_text, null)
                val txtText = rootView?.findViewById<TextView>(R.id.tvPhotoEditorText)
                if (txtText != null && mDefaultTextTypeface != null) {
                    txtText.gravity = Gravity.CENTER
                    if (mDefaultEmojiTypeface != null) {
                        txtText.typeface = mDefaultTextTypeface
                    }
                }
            }
            ViewType.IMAGE -> rootView =
                mLayoutInflater.inflate(R.layout.view_photo_editor_image, null)
            ViewType.EMOJI -> {
                rootView = mLayoutInflater.inflate(R.layout.view_photo_editor_text, null)
                val txtTextEmoji = rootView?.findViewById<TextView>(R.id.tvPhotoEditorText)
                if (txtTextEmoji != null) {
                    if (mDefaultEmojiTypeface != null) {
                        txtTextEmoji.typeface = mDefaultEmojiTypeface
                    }
                    txtTextEmoji.gravity = Gravity.CENTER
                    txtTextEmoji.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
            }
            else -> rootView = null
        }
        viewParam.view = rootView
        if (rootView != null) {
            //We are setting tag as ViewType to identify what type of the view it is
            //when we remove the view from stack i.e onRemoveViewListener(ViewType viewType, int numberOfAddedViews);
            rootView.tag = viewType
            val imgClose = rootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            val finalRootView = rootView
            imgClose?.setOnClickListener { viewUndo(finalRootView, viewType, viewParam) }
        }

        return rootView
    }

    /**
     * Enable/Disable drawing mode to draw on [PhotoEditorView]
     *
     * @param brushDrawingMode true if mode is enabled
     */
    fun setBrushDrawingMode(brushDrawingMode: Boolean) {
        brushDrawingView?.brushDrawingMode = brushDrawingMode
    }

    /**
     * set opacity/transparency of brush while painting on [BrushDrawingView]
     *
     * @param opacity opacity is in form of percentage
     */
    fun setOpacity(@IntRange(from = 0, to = 100) opacity: Int) {
        var opacity = opacity
        if (brushDrawingView != null) {
            opacity = (opacity / 100.0 * 255.0).toInt()
            brushDrawingView.opacity = opacity
        }
    }

    /**
     * set the eraser size
     * <br></br>
     * **Note :** Eraser size is different from the normal brush size
     *
     * @param brushEraserSize size of eraser
     */
    fun setBrushEraserSize(brushEraserSize: Float) {
        brushDrawingView?.setBrushEraserSize(brushEraserSize)
    }

    internal fun setBrushEraserColor(@ColorInt color: Int) {
        brushDrawingView?.setBrushEraserColor(color)
    }

    /**
     *
     *
     * Its enables eraser mode after that whenever user drags on screen this will erase the existing
     * paint
     * <br></br>
     * **Note** : This eraser will work on paint views only
     *
     *
     */
    fun brushEraser() {
        brushDrawingView?.brushEraser()
    }

    /*private void viewUndo() {
        if (addedViews.size() > 0) {
            parentView.removeView(addedViews.remove(addedViews.size() - 1));
            if (mOnPhotoEditorListener != null)
                mOnPhotoEditorListener.onRemoveViewListener(addedViews.size());
        }
    }*/

    private fun viewUndo(removedView: View, viewType: ViewType, viewParam: ViewParam) {
        if (addedViews.size > 0) {
            if (addedViews.contains(viewParam)) {
                parentView?.removeView(removedView)
                addedViews.remove(viewParam)
                redoViews.add(removedView)
                mOnPhotoEditorListener?.onRemoveViewListener(
                    viewType,
                    addedViews.size
                )

            }
        }
    }

    /**
     * Undo the last operation perform on the [PhotoEditor]
     *
     * @return true if there nothing more to undo
     */
    fun undo(): Boolean {
        if (addedViews.size > 0) {
            val removeView = addedViews[addedViews.size - 1].view
            if (removeView is BrushDrawingView) {
                return brushDrawingView != null && brushDrawingView.undo()
            } else {
                addedViews.removeAt(addedViews.size - 1)
                parentView?.removeView(removeView)
                //todo
                // redoViews.add(removeView)
            }
            val viewTag = removeView?.tag
            if (viewTag != null && viewTag is ViewType) {
                mOnPhotoEditorListener?.onRemoveViewListener(viewTag, addedViews.size)
            }
        }
        return addedViews.size != 0
    }

    /**
     * Redo the last operation perform on the [PhotoEditor]
     *
     * @return true if there nothing more to redo
     */
    fun redo(): Boolean {
        if (redoViews.size > 0) {
            val redoView = redoViews[redoViews.size - 1]
            if (redoView is BrushDrawingView) {
                return brushDrawingView != null && brushDrawingView.redo()
            } else {
                redoViews.removeAt(redoViews.size - 1)
                parentView?.addView(redoView)
                addedViews.add(ViewParam(redoView, null, null))
            }
            val viewTag = redoView.tag
            if (viewTag != null && viewTag is ViewType) {
                mOnPhotoEditorListener?.onAddViewListener(viewTag, addedViews.size)
            }
        }
        return redoViews.size != 0
    }

    private fun clearBrushAllViews() {
        brushDrawingView?.clearAll()
    }

    /**
     * Removes all the edited operations performed [PhotoEditorView]
     * This will also clear the undo and redo stack
     */
    fun clearAllViews() {
        for (i in addedViews.indices) {
            parentView?.removeView(addedViews[i].view)
            addedViews[i].view = null
            addedViews[i].textStyleBuilder = null
            addedViews[i].typeface = null
        }
        //todo return
        /* if (addedViews.contains(brushDrawingView)) {
             parentView!!.addView(brushDrawingView)
         }*/
        addedViews.clear()
        redoViews.clear()
        clearBrushAllViews()
    }

    /**
     * Remove all helper boxes from views
     */
    @UiThread
    fun clearHelperBox() {
        for (i in 0 until parentView!!.childCount) {

            val childAt = parentView.getChildAt(i)
            val frmBorder = childAt.findViewById<FrameLayout>(R.id.frmBorder)
            frmBorder?.setBackgroundResource(0)
            val imgClose = childAt.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            if (imgClose != null) {
                imgClose.visibility = View.GONE
            }
        }
    }

    /**
     * Setup of custom effect using effect type and set parameters values
     *
     * @param customEffect [CustomEffect.Builder.setParameter]
     */
    fun setFilterEffect(customEffect: CustomEffect) {
        parentView?.setFilterEffect(customEffect)
    }

    /**
     * Set pre-define filter available
     *
     * @param filterType type of filter want to apply [PhotoEditor]
     */
    fun setFilterEffect(filterType: PhotoFilter) {
        parentView?.setFilterEffect(filterType)
    }

    fun uploadViews(list: List<ViewCharacteristic>?) {
        if (list == null) {
            throw NullPointerException("Dont upload NULL list")
        } else {
            for (viewCharacteristic in list) {
                when (viewCharacteristic.viewType) {
                    ViewType.TEXT -> addTextWithParameters(viewCharacteristic)
                    ViewType.EMOJI -> addEmojiWithParametrs(viewCharacteristic)
                    else -> Log.v(
                        TAG,
                        "I cant load this type of view -  ${viewCharacteristic.viewType}"
                    )
                }
            }
        }
    }

    private fun addEmojiWithParametrs(viewCharacteristic: ViewCharacteristic) {
        val viewParam = ViewParam()
        brushDrawingView?.brushDrawingMode = false
        val emojiRootView = getLayout(ViewType.EMOJI, viewParam)
        emojiRootView?.let { rootView ->
            val emojiTextView = rootView.findViewById<TextView>(R.id.tvPhotoEditorText)
            val frmBorder = rootView.findViewById<FrameLayout>(R.id.frmBorder)
            val imgClose = rootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            val emojiTypeface = viewCharacteristic.typeface
            emojiTextView?.typeface = emojiTypeface
            emojiTextView.textSize = 56f
            emojiTextView.text = viewCharacteristic.content
            val multiTouchListener = multiTouchListener
            multiTouchListener.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
                override fun onClick() {
                    val isBackgroundVisible = frmBorder.tag != null && frmBorder.tag as Boolean
                    frmBorder.setBackgroundResource(if (isBackgroundVisible) 0 else R.drawable.rounded_border_tv)
                    imgClose.visibility = if (isBackgroundVisible) View.GONE else View.VISIBLE
                    frmBorder.tag = !isBackgroundVisible
                }

                override fun onLongClick() {}
            })
            emojiRootView.setOnTouchListener(multiTouchListener)
            addViewToParent(ViewType.EMOJI, viewParam, viewCharacteristic)
        }

    }

    private fun addTextWithParameters(viewCharacteristic: ViewCharacteristic) {
        val styleBuilder = TextStyleBuilder()
        val viewParam = ViewParam()
        styleBuilder.withTextColor(viewCharacteristic.color)
        viewCharacteristic.typeface?.let {
            styleBuilder.withTextFont(it)
            viewParam.typeface = it
        }
        viewParam.textStyleBuilder = styleBuilder
        viewParam.colorView = viewCharacteristic.color

        brushDrawingView?.brushDrawingMode = false
        val textRootView = getLayout(ViewType.TEXT, viewParam)
        textRootView?.let { rootView ->
            val textInputTv = rootView.findViewById<TextView>(R.id.tvPhotoEditorText)
            val imgClose = rootView.findViewById<ImageView>(R.id.imgPhotoEditorClose)
            val frmBorder = rootView.findViewById<FrameLayout>(R.id.frmBorder)

            textInputTv.text = viewCharacteristic.content
            styleBuilder.applyStyle(textInputTv)

            val multiTouchListener = multiTouchListener
            multiTouchListener.setOnGestureControl(object : MultiTouchListener.OnGestureControl {
                override fun onClick() {
                    val isBackgroundVisible = frmBorder.tag != null && frmBorder.tag as Boolean
                    frmBorder.setBackgroundResource(if (isBackgroundVisible) 0 else R.drawable.rounded_border_tv)
                    imgClose.visibility = if (isBackgroundVisible) View.GONE else View.VISIBLE
                    frmBorder.tag = !isBackgroundVisible
                }

                override fun onLongClick() {
                    val textInput = textInputTv.text.toString()
                    val currentTextColor = textInputTv.currentTextColor
                    mOnPhotoEditorListener?.onEditTextChangeListener(
                        rootView,
                        textInput,
                        currentTextColor
                    )
                }
            })

            rootView.setOnTouchListener(multiTouchListener)
            addViewToParent(ViewType.TEXT, viewParam, viewCharacteristic)
        }


    }

    /**
     * A callback to save the edited image asynchronously
     */
    interface OnSaveListener {

        /**
         * Call when edited image is saved successfully on given path
         *
         * @param imagePath path on which image is saved
         */
        fun onSuccess(imagePath: String)

        /**
         * Call when failed to saved image on given path
         *
         * @param exception exception thrown while saving image
         */
        fun onFailure(exception: Exception)
    }

    /**
     * Save the edited image on given path
     *
     * @param imagePath      path on which image to be saved
     * @param onSaveListener callback for saving image
     * @see OnSaveListener
     */
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    fun saveAsFile(imagePath: String, onSaveListener: OnSaveListener) {
        saveAsFile(imagePath, SaveSettings(), onSaveListener)
    }

    /**
     * Save the edited image on given path
     *
     * @param imagePath      path on which image to be saved
     * @param saveSettings   builder for multiple save options [SaveSettings]
     * @param onSaveListener callback for saving image
     * @see OnSaveListener
     */
    @SuppressLint("StaticFieldLeak")
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    fun saveAsFile(
        imagePath: String,
        saveSettings: SaveSettings,
        onSaveListener: OnSaveListener
    ) {
        Log.d(TAG, "Image Path: $imagePath")
        parentView?.saveFilter(object : OnSaveBitmap {
            override fun onBitmapReady(saveBitmap: Bitmap) {
                object : AsyncTask<String, String, Exception>() {

                    override fun onPreExecute() {
                        super.onPreExecute()
                        clearHelperBox()
                        parentView.isDrawingCacheEnabled = false
                    }

                    @SuppressLint("MissingPermission", "WrongThread")
                    override fun doInBackground(vararg strings: String): Exception? {
                        // Create a media file name
                        val file = File(imagePath)
                        try {
                            val out = FileOutputStream(file, false)
                            parentView.isDrawingCacheEnabled = true
                            val drawingCache = if (saveSettings.isTransparencyEnabled == true)
                                BitmapUtil.removeTransparency(parentView.drawingCache)
                            else
                                parentView.drawingCache
                            saveSettings.compressQuality?.let {
                                drawingCache.compress(
                                    saveSettings.compressFormat,
                                    it,
                                    out
                                )
                            }
                            out.flush()
                            out.close()
                            Log.d(TAG, "Filed Saved Successfully")
                            return null
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d(TAG, "Failed to save File")
                            return e
                        }

                    }

                    override fun onPostExecute(e: Exception?) {
                        super.onPostExecute(e)
                        if (e == null) {
                            //Clear all views if its enabled in save settings
                            if (saveSettings.isClearViewsEnabled == true) clearAllViews()
                            onSaveListener.onSuccess(imagePath)
                        } else {
                            onSaveListener.onFailure(e)
                        }
                    }

                }.execute()
            }

            override fun onFailure(e: Exception) {
                onSaveListener.onFailure(e)
            }
        })
    }

    /**
     * Save the edited image as bitmap
     *
     * @param onSaveBitmap callback for saving image as bitmap
     * @see OnSaveBitmap
     */
    @SuppressLint("StaticFieldLeak")
    fun saveAsBitmap(onSaveBitmap: OnSaveBitmap) {
        saveAsBitmap(SaveSettings(), onSaveBitmap)
    }

    /**
     * Save the edited image as bitmap
     *
     * @param saveSettings builder for multiple save options [SaveSettings]
     * @param onSaveBitmap callback for saving image as bitmap
     * @see OnSaveBitmap
     */
    @SuppressLint("StaticFieldLeak")
    fun saveAsBitmap(
        saveSettings: SaveSettings,
        onSaveBitmap: OnSaveBitmap
    ) {
        parentView?.saveFilter(object : OnSaveBitmap {
            override fun onBitmapReady(saveBitmap: Bitmap) {
                object : AsyncTask<String, String, Bitmap>() {
                    override fun onPreExecute() {
                        super.onPreExecute()
                        clearHelperBox()
                        parentView.isDrawingCacheEnabled = false
                    }

                    @SuppressLint("WrongThread")
                    override fun doInBackground(vararg strings: String): Bitmap? {
                        parentView.isDrawingCacheEnabled = true
                        return if (saveSettings.isTransparencyEnabled == true)
                            BitmapUtil.removeTransparency(parentView.drawingCache)
                        else
                            parentView.drawingCache
                    }

                    override fun onPostExecute(bitmap: Bitmap?) {
                        super.onPostExecute(bitmap)
                        if (bitmap != null) {
                            if (saveSettings.isClearViewsEnabled == true) clearAllViews()
                            onSaveBitmap.onBitmapReady(bitmap)
                        } else {
                            onSaveBitmap.onFailure(Exception("Failed to load the bitmap"))
                        }
                    }

                }.execute()
            }

            override fun onFailure(e: Exception) {
                onSaveBitmap.onFailure(e)
            }
        })
    }

    /**
     * Callback on editing operation perform on [PhotoEditorView]
     *
     * @param onPhotoEditorListener [OnPhotoEditorListener]
     */
    fun setOnPhotoEditorListener(onPhotoEditorListener: OnPhotoEditorListener) {
        this.mOnPhotoEditorListener = onPhotoEditorListener
    }


    override fun onViewAdd(brushDrawingView: BrushDrawingView) {
        if (redoViews.size > 0) {
            redoViews.removeAt(redoViews.size - 1)
        }
        addedViews.add(ViewParam(brushDrawingView, null, null))
        mOnPhotoEditorListener?.onAddViewListener(ViewType.BRUSH_DRAWING, addedViews.size)

    }


    override fun onViewRemoved(brushDrawingView: BrushDrawingView) {
        if (addedViews.size > 0) {
            val removeView = addedViews.removeAt(addedViews.size - 1).view
            if (removeView !is BrushDrawingView) {
                parentView?.removeView(removeView)
            }
            //todo
            //redoViews.add(removeView)
        }

        mOnPhotoEditorListener?.apply {
            onRemoveViewListener(ViewType.BRUSH_DRAWING, addedViews.size)
        }

    }

    override fun onStartDrawing() {

        mOnPhotoEditorListener?.apply {
            onStartViewChangeListener(ViewType.BRUSH_DRAWING)
        }
    }

    override fun onStopDrawing() {
        mOnPhotoEditorListener?.apply {
            onStopViewChangeListener(ViewType.BRUSH_DRAWING)
        }
    }


    /**
     * Builder pattern to define [PhotoEditor] Instance
     */
    class Builder
    /**
     * Building a PhotoEditor which requires a Context and PhotoEditorView
     * which we have setup in our xml layout
     *
     * @param context         context
     * @param photoEditorView [PhotoEditorView]
     */
        (val context: Context, val parentView: PhotoEditorView) {
        val imageView: ImageView? = parentView.source
        var deleteView: View? = null
        val brushDrawingView: BrushDrawingView? = parentView.brushDrawingView
        var textTypeface: Typeface? = null
        var emojiTypeface: Typeface? = null
        //By Default pinch zoom on text is enabled
        var isTextPinchZoomable = true

        internal fun setDeleteView(deleteView: View): Builder {
            this.deleteView = deleteView
            return this
        }

        /**
         * set default text font to be added on image
         *
         * @param textTypeface typeface for custom font
         * @return [Builder] instant to build [PhotoEditor]
         */
        fun setDefaultTextTypeface(textTypeface: Typeface): Builder {
            this.textTypeface = textTypeface
            return this
        }

        /**
         * set default font specific to add emojis
         *
         * @param emojiTypeface typeface for custom font
         * @return [Builder] instant to build [PhotoEditor]
         */
        fun setDefaultEmojiTypeface(emojiTypeface: Typeface): Builder {
            this.emojiTypeface = emojiTypeface
            return this
        }

        /**
         * set false to disable pinch to zoom on text insertion.By deafult its true
         *
         * @param isTextPinchZoomable flag to make pinch to zoom
         * @return [Builder] instant to build [PhotoEditor]
         */
        fun setPinchTextScalable(isTextPinchZoomable: Boolean): Builder {
            this.isTextPinchZoomable = isTextPinchZoomable
            return this
        }

        /**
         * @return build PhotoEditor instance
         */
        fun build(): PhotoEditor {
            return PhotoEditor(this)
        }
    }


    private fun getViewCharacteristic(viewParam: ViewParam): ViewCharacteristic? {
        val view = viewParam.view
        view?.let { v ->
            val scaleX = v.scaleX
            val scaleY = v.scaleY
            val alpha = v.alpha
            val rotation = v.rotation
            val typeface = viewParam.typeface
            val textStyleBuilder = viewParam.textStyleBuilder
            val viewType = v.tag as ViewType
            val content = getContentByViewType(viewType, v)
            return ViewCharacteristic(
                scaleX,
                scaleY,
                alpha,
                rotation,
                typeface,
                viewType,
                textStyleBuilder,
                content,
                v.translationX,
                v.translationY,
                v.pivotX,
                v.pivotY,
                viewParam.colorView ?: 0
            )
        } ?: return null

    }

    private fun getContentByViewType(viewType: ViewType, view: View): String {
        val tv: TextView? = when (viewType) {
            ViewType.TEXT -> view.findViewById(R.id.tvPhotoEditorText)
            ViewType.EMOJI -> view.findViewById(R.id.tvPhotoEditorText)
            else -> null
        }
        return tv?.text?.toString() ?: ""
    }


    companion object {

        private val TAG = "PhotoEditor"

        private fun convertEmoji(emoji: String): String {
            val returnedEmoji: String
            returnedEmoji = try {
                val convertEmojiToInt = Integer.parseInt(emoji.substring(2), 16)
                String(Character.toChars(convertEmojiToInt))
            } catch (e: NumberFormatException) {
                ""
            }

            return returnedEmoji
        }

        /**
         * Provide the list of emoji in form of unicode string
         *
         * @param context context
         * @return list of emoji unicode
         */
        fun getEmojis(context: Context): ArrayList<String> {
            val convertedEmojiList = ArrayList<String>()
            val emojiList = context.resources.getStringArray(R.array.photo_editor_emoji)
            for (emojiUnicode in emojiList) {
                convertedEmojiList.add(convertEmoji(emojiUnicode))
            }
            return convertedEmojiList
        }
    }
}
