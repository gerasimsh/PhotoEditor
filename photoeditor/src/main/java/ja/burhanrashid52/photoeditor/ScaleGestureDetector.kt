/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ja.burhanrashid52.photoeditor

import android.util.Log
import android.view.MotionEvent
import android.view.View

/**
 * Detects transformation gestures involving more than one pointer ("multitouch")
 * using the supplied [MotionEvent]s. The [OnScaleGestureListener]
 * callback will notify users when a particular gesture event has occurred.
 * This class should only be used with [MotionEvent]s reported via touch.
 *
 * To use this class:
 *
 *  * Create an instance of the `ScaleGestureDetector` for your
 * [View]
 *
 */
internal class ScaleGestureDetector(private val mListener: OnScaleGestureListener) {
    /**
     * Returns `true` if a two-finger scale gesture is in progress.
     * @return `true` if a scale gesture is in progress, `false` otherwise.
     */
    var isInProgress: Boolean = false
        private set

    private var mPrevEvent: MotionEvent? = null
    private var mCurrEvent: MotionEvent? = null

    val currentSpanVector: Vector2D
    /**
     * Get the X coordinate of the current gesture's focal point.
     * If a gesture is in progress, the focal point is directly between
     * the two pointers forming the gesture.
     * If a gesture is ending, the focal point is the location of the
     * remaining pointer on the screen.
     * If [.isInProgress] would return false, the result of this
     * function is undefined.
     *
     * @return X coordinate of the focal point in pixels.
     */
    var focusX: Float = 0.toFloat()
        private set
    /**
     * Get the Y coordinate of the current gesture's focal point.
     * If a gesture is in progress, the focal point is directly between
     * the two pointers forming the gesture.
     * If a gesture is ending, the focal point is the location of the
     * remaining pointer on the screen.
     * If [.isInProgress] would return false, the result of this
     * function is undefined.
     *
     * @return Y coordinate of the focal point in pixels.
     */
    var focusY: Float = 0.toFloat()
        private set
    /**
     * Return the previous x distance between the two pointers forming the
     * gesture in progress.
     *
     * @return Previous distance between pointers in pixels.
     */
    var previousSpanX: Float = 0.toFloat()
        private set
    /**
     * Return the previous y distance between the two pointers forming the
     * gesture in progress.
     *
     * @return Previous distance between pointers in pixels.
     */
    var previousSpanY: Float = 0.toFloat()
        private set
    /**
     * Return the current x distance between the two pointers forming the
     * gesture in progress.
     *
     * @return Distance between pointers in pixels.
     */
    var currentSpanX: Float = 0.toFloat()
        private set
    /**
     * Return the current y distance between the two pointers forming the
     * gesture in progress.
     *
     * @return Distance between pointers in pixels.
     */
    var currentSpanY: Float = 0.toFloat()
        private set
    private var mCurrLen: Float = 0.toFloat()
    private var mPrevLen: Float = 0.toFloat()
    private var mScaleFactor: Float = 0.toFloat()
    private var mCurrPressure: Float = 0.toFloat()
    private var mPrevPressure: Float = 0.toFloat()
    /**
     * Return the time difference in milliseconds between the previous
     * accepted scaling event and the current scaling event.
     *
     * @return Time difference since the last scaling event in milliseconds.
     */
    var timeDelta: Long = 0
        private set

    private var mInvalidGesture: Boolean = false

    // Pointer IDs currently responsible for the two fingers controlling the gesture
    private var mActiveId0: Int = 0
    private var mActiveId1: Int = 0
    private var mActive0MostRecent: Boolean = false

    /**
     * Return the current distance between the two pointers forming the
     * gesture in progress.
     *
     * @return Distance between pointers in pixels.
     */
    private val currentSpan: Float
        get() {
            if (mCurrLen == -1f) {
                val cvx = currentSpanX
                val cvy = currentSpanY
                mCurrLen = Math.sqrt((cvx * cvx + cvy * cvy).toDouble()).toFloat()
            }
            return mCurrLen
        }

    /**
     * Return the previous distance between the two pointers forming the
     * gesture in progress.
     *
     * @return Previous distance between pointers in pixels.
     */
    private val previousSpan: Float
        get() {
            if (mPrevLen == -1f) {
                val pvx = previousSpanX
                val pvy = previousSpanY
                mPrevLen = Math.sqrt((pvx * pvx + pvy * pvy).toDouble()).toFloat()
            }
            return mPrevLen
        }

    /**
     * Return the scaling factor from the previous scale event to the current
     * event. This value is defined as
     * ([.getCurrentSpan] / [.getPreviousSpan]).
     *
     * @return The current scaling factor.
     */
    val scaleFactor: Float
        get() {
            if (mScaleFactor == -1f) {
                mScaleFactor = currentSpan / previousSpan
            }
            return mScaleFactor
        }

    /**
     * Return the event time of the current event being processed.
     *
     * @return Current event time in milliseconds.
     */
    val eventTime: Long
        get() = mCurrEvent?.eventTime ?: 0L

    /**
     * The listener for receiving notifications when gestures occur.
     * If you want to listen for all the different gestures then implement
     * this interface. If you only want to listen for a subset it might
     * be easier to extend [SimpleOnScaleGestureListener].
     *
     * An application will receive events in the following order:
     */
    internal interface OnScaleGestureListener {
        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         * retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        fun onScale(view: View, detector: ScaleGestureDetector): Boolean

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         * retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        fun onScaleBegin(view: View, detector: ScaleGestureDetector): Boolean

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         *
         * Once a scale has ended, [ScaleGestureDetector.getFocusX]
         * and [ScaleGestureDetector.getFocusY] will return the location
         * of the pointer remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         * retrieve extended info about event state.
         */
        fun onScaleEnd(view: View, detector: ScaleGestureDetector)
    }

    /**
     * A convenience class to extend when you only want to listen for a subset
     * of scaling-related events. This implements all methods in
     */
    internal open class SimpleOnScaleGestureListener : OnScaleGestureListener {

        override fun onScale(view: View, detector: ScaleGestureDetector): Boolean {
            return false
        }

        override fun onScaleBegin(view: View, detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScaleEnd(view: View, detector: ScaleGestureDetector) {
            // Intentionally empty
        }
    }

    init {
        currentSpanVector = Vector2D()
    }

    fun onTouchEvent(view: View, event: MotionEvent): Boolean {
        val action = event.actionMasked

        if (action == MotionEvent.ACTION_DOWN) {
            reset() // Start fresh
        }

        var handled = true
        if (mInvalidGesture) {
            handled = false
        } else if (!isInProgress) {
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    mActiveId0 = event.getPointerId(0)
                    mActive0MostRecent = true
                }

                MotionEvent.ACTION_UP -> reset()

                MotionEvent.ACTION_POINTER_DOWN -> {
                    // We have a new multi-finger gesture
                    if (mPrevEvent != null) mPrevEvent?.recycle()
                    mPrevEvent = MotionEvent.obtain(event)
                    timeDelta = 0

                    val index1 = event.actionIndex
                    var index0 = event.findPointerIndex(mActiveId0)
                    mActiveId1 = event.getPointerId(index1)
                    if (index0 < 0 || index0 == index1) {
                        // Probably someone sending us a broken event stream.
                        index0 = findNewActiveIndex(event, mActiveId1, -1)
                        mActiveId0 = event.getPointerId(index0)
                    }
                    mActive0MostRecent = false

                    setContext(view, event)

                    isInProgress = mListener.onScaleBegin(view, this)
                }
            }
        } else {
            // Transform gesture in progress - attempt to handle it
            when (action) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // End the old gesture and begin a new one with the most recent two fingers.
                    mListener.onScaleEnd(view, this)
                    val oldActive0 = mActiveId0
                    val oldActive1 = mActiveId1
                    reset()

                    mPrevEvent = MotionEvent.obtain(event)
                    mActiveId0 = if (mActive0MostRecent) oldActive0 else oldActive1
                    mActiveId1 = event.getPointerId(event.actionIndex)
                    mActive0MostRecent = false

                    var index0 = event.findPointerIndex(mActiveId0)
                    if (index0 < 0 || mActiveId0 == mActiveId1) {
                        // Probably someone sending us a broken event stream.
                        index0 = findNewActiveIndex(event, mActiveId1, -1)
                        mActiveId0 = event.getPointerId(index0)
                    }

                    setContext(view, event)

                    isInProgress = mListener.onScaleBegin(view, this)
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerCount = event.pointerCount
                    val actionIndex = event.actionIndex
                    val actionId = event.getPointerId(actionIndex)

                    var gestureEnded = false
                    if (pointerCount > 2) {
                        if (actionId == mActiveId0) {
                            val newIndex = findNewActiveIndex(event, mActiveId1, actionIndex)
                            if (newIndex >= 0) {
                                mListener.onScaleEnd(view, this)
                                mActiveId0 = event.getPointerId(newIndex)
                                mActive0MostRecent = true
                                mPrevEvent = MotionEvent.obtain(event)
                                setContext(view, event)
                                isInProgress = mListener.onScaleBegin(view, this)
                            } else {
                                gestureEnded = true
                            }
                        } else if (actionId == mActiveId1) {
                            val newIndex = findNewActiveIndex(event, mActiveId0, actionIndex)
                            if (newIndex >= 0) {
                                mListener.onScaleEnd(view, this)
                                mActiveId1 = event.getPointerId(newIndex)
                                mActive0MostRecent = false
                                mPrevEvent = MotionEvent.obtain(event)
                                setContext(view, event)
                                isInProgress = mListener.onScaleBegin(view, this)
                            } else {
                                gestureEnded = true
                            }
                        }
                        mPrevEvent?.recycle()
                        mPrevEvent = MotionEvent.obtain(event)
                        setContext(view, event)
                    } else {
                        gestureEnded = true
                    }

                    if (gestureEnded) {
                        // Gesture ended
                        setContext(view, event)

                        // Set focus point to the remaining finger
                        val activeId = if (actionId == mActiveId0) mActiveId1 else mActiveId0
                        val index = event.findPointerIndex(activeId)
                        focusX = event.getX(index)
                        focusY = event.getY(index)

                        mListener.onScaleEnd(view, this)
                        reset()
                        mActiveId0 = activeId
                        mActive0MostRecent = true
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    mListener.onScaleEnd(view, this)
                    reset()
                }

                MotionEvent.ACTION_UP -> reset()

                MotionEvent.ACTION_MOVE -> {
                    setContext(view, event)

                    // Only accept the event if our relative pressure is within
                    // a certain limit - this can help filter shaky data as a
                    // finger is lifted.
                    if (mCurrPressure / mPrevPressure > PRESSURE_THRESHOLD) {
                        val updatePrevious = mListener.onScale(view, this)

                        if (updatePrevious) {
                            mPrevEvent?.recycle()
                            mPrevEvent = MotionEvent.obtain(event)
                        }
                    }
                }
            }
        }

        return handled
    }

    private fun findNewActiveIndex(
        ev: MotionEvent,
        otherActiveId: Int,
        removedPointerIndex: Int
    ): Int {
        val pointerCount = ev.pointerCount

        // It's ok if this isn't found and returns -1, it simply won't match.
        val otherActiveIndex = ev.findPointerIndex(otherActiveId)

        // Pick a new id and update tracking state.
        for (i in 0 until pointerCount) {
            if (i != removedPointerIndex && i != otherActiveIndex) {
                return i
            }
        }
        return -1
    }

    private fun setContext(view: View, curr: MotionEvent) {
        if (mCurrEvent != null) {
            mCurrEvent?.recycle()
        }
        mCurrEvent = MotionEvent.obtain(curr)

        mCurrLen = -1f
        mPrevLen = -1f
        mScaleFactor = -1f
        currentSpanVector.set(0.0f, 0.0f)

        val prev = mPrevEvent
        prev?.let { p ->
            val prevIndex0 = p.findPointerIndex(mActiveId0)
            val prevIndex1 = p.findPointerIndex(mActiveId1)
            val currIndex0 = curr.findPointerIndex(mActiveId0)
            val currIndex1 = curr.findPointerIndex(mActiveId1)

            if (prevIndex0 < 0 || prevIndex1 < 0 || currIndex0 < 0 || currIndex1 < 0) {
                mInvalidGesture = true
                Log.e(TAG, "Invalid MotionEvent stream detected.", Throwable())
                if (isInProgress) {
                    mListener.onScaleEnd(view, this)
                }
                return
            }

            val px0 = p.getX(prevIndex0)
            val py0 = p.getY(prevIndex0)
            val px1 = p.getX(prevIndex1)
            val py1 = p.getY(prevIndex1)
            val cx0 = curr.getX(currIndex0)
            val cy0 = curr.getY(currIndex0)
            val cx1 = curr.getX(currIndex1)
            val cy1 = curr.getY(currIndex1)

            val pvx = px1 - px0
            val pvy = py1 - py0
            val cvx = cx1 - cx0
            val cvy = cy1 - cy0

            currentSpanVector.set(cvx, cvy)

            previousSpanX = pvx
            previousSpanY = pvy
            currentSpanX = cvx
            currentSpanY = cvy

            focusX = cx0 + cvx * 0.5f
            focusY = cy0 + cvy * 0.5f
            timeDelta = curr.eventTime - p.eventTime
            mCurrPressure = curr.getPressure(currIndex0) + curr.getPressure(currIndex1)
            mPrevPressure = p.getPressure(prevIndex0) + p.getPressure(prevIndex1)
        }

    }

    private fun reset() {
        if (mPrevEvent != null) {
            mPrevEvent?.recycle()
            mPrevEvent = null
        }
        if (mCurrEvent != null) {
            mCurrEvent?.recycle()
            mCurrEvent = null
        }
        isInProgress = false
        mActiveId0 = -1
        mActiveId1 = -1
        mInvalidGesture = false
    }

    companion object {
        private val TAG = "ScaleGestureDetector"

        /**
         * This value is the threshold ratio between our previous combined pressure
         * and the current combined pressure. We will only fire an onScale event if
         * the computed ratio between the current and previous event pressures is
         * greater than this value. When pressure decreases rapidly between events
         * the position values can often be imprecise, as it usually indicates
         * that the user is in the process of lifting a pointer off of the device.
         * Its value was tuned experimentally.
         */
        private val PRESSURE_THRESHOLD = 0.67f
    }
}
