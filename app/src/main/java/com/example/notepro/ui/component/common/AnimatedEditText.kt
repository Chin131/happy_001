package com.example.notepro.ui.component.common

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.textfield.TextInputEditText

class AnimatedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextInputEditText(context, attrs, defStyleAttr) {

    private val focusedElevation = 8f
    private val defaultElevation = 2f
    
    init {
        // Set initial elevation
        elevation = defaultElevation
        
        // Add focus listener
        setOnFocusChangeListener { _, hasFocus ->
            val targetElevation = if (hasFocus) focusedElevation else defaultElevation
            
            ValueAnimator.ofFloat(elevation, targetElevation).apply {
                duration = 300
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { animator ->
                    elevation = animator.animatedValue as Float
                }
                start()
            }
        }
    }
} 