package com.example.notepro.ui.component

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.notepro.R
import com.google.android.material.textfield.TextInputEditText

/**
 * A custom TextInputEditText that animates its border when focused
 */
class AnimatedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {

    private val focusedBorderWidth = 2f
    private val unfocusedBorderWidth = 1f
    private val animationDuration = 150L
    private var borderDrawable: GradientDrawable? = null
    private var focusedElevation = 4f
    private var defaultElevation = 1f

    init {
        setupBorder()
        setupFocusListeners()
        
        // Set initial elevation
        elevation = defaultElevation
    }

    private fun setupBorder() {
        borderDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = resources.getDimension(R.dimen.input_corner_radius)
            setStroke(
                unfocusedBorderWidth.toInt(),
                ContextCompat.getColor(context, R.color.colorOutline)
            )
            setColor(ContextCompat.getColor(context, R.color.colorSurface))
        }
        background = borderDrawable
    }

    private fun setupFocusListeners() {
        setOnFocusChangeListener { _, hasFocus ->
            animateBorder(hasFocus)
            animateElevation(hasFocus)
        }
    }

    private fun animateBorder(focused: Boolean) {
        val targetColor = if (focused) {
            ContextCompat.getColor(context, R.color.colorPrimary)
        } else {
            ContextCompat.getColor(context, R.color.colorOutline)
        }

        val targetWidth = if (focused) focusedBorderWidth else unfocusedBorderWidth

        val borderWidthProperty = object : android.util.FloatProperty<AnimatedEditText>("borderWidth") {
            override fun setValue(view: AnimatedEditText, value: Float) {
                borderDrawable?.setStroke(value.toInt(), targetColor)
                invalidate()
            }

            override fun get(view: AnimatedEditText): Float {
                return if (focused) unfocusedBorderWidth else focusedBorderWidth
            }
        }

        val widthAnimator = ObjectAnimator.ofFloat(
            this, 
            borderWidthProperty, 
            if (focused) unfocusedBorderWidth else focusedBorderWidth,
            targetWidth
        )

        AnimatorSet().apply {
            play(widthAnimator)
            duration = animationDuration
            interpolator = DecelerateInterpolator()
            start()
        }
    }
    
    private fun animateElevation(focused: Boolean) {
        val targetElevation = if (focused) focusedElevation else defaultElevation
        
        ObjectAnimator.ofFloat(this, "elevation", elevation, targetElevation).apply {
            duration = animationDuration
            interpolator = DecelerateInterpolator()
            start()
        }
    }
} 