package com.example.notepro.ui.component.common

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.view.animation.OvershootInterpolator
import com.google.android.material.button.MaterialButton

class AnimatedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {

    private val pressedScale = 0.95f
    private val defaultScale = 1.0f
    
    init {
        // Set button initial state
        scaleX = defaultScale
        scaleY = defaultScale
        
        // Set click state listeners
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    animate()
                        .scaleX(pressedScale)
                        .scaleY(pressedScale)
                        .setDuration(100)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    animate()
                        .scaleX(defaultScale)
                        .scaleY(defaultScale)
                        .setDuration(200)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            }
            false
        }
    }
} 