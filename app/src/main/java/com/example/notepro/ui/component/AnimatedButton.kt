package com.example.notepro.ui.component

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatButton
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.example.notepro.R

/**
 * Custom button with press animations and ripple effects
 */
class AnimatedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Animation properties
    private var pressedScale = 0.96f
    private var currentScale = 1.0f
    private var rippleRadius = 0f
    private var maxRippleRadius = 0f
    private var rippleAlpha = 0
    private var rippleCenterX = 0f
    private var rippleCenterY = 0f
    
    // Animators
    private var scaleAnimator: ValueAnimator? = null
    private var rippleAnimator: ValueAnimator? = null
    
    // Button properties
    private var cornerRadius = 16f
    private var buttonColor = 0
    private var rippleColor = 0
    private val buttonRect = RectF()
    
    init {
        // Set up default values
        buttonColor = ContextCompat.getColor(context, R.color.colorPrimary)
        rippleColor = Color.WHITE
        
        // Apply custom attributes if provided
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AnimatedButton,
            0, 0
        ).apply {
            try {
                buttonColor = getColor(
                    R.styleable.AnimatedButton_buttonColor,
                    ContextCompat.getColor(context, R.color.colorPrimary)
                )
                rippleColor = getColor(
                    R.styleable.AnimatedButton_rippleColor,
                    Color.WHITE
                )
                cornerRadius = getDimension(
                    R.styleable.AnimatedButton_cornerRadius,
                    16f
                )
                pressedScale = getFloat(
                    R.styleable.AnimatedButton_pressedScale,
                    0.96f
                )
            } finally {
                recycle()
            }
        }
        
        // Initialize paints
        backgroundPaint.color = buttonColor
        backgroundPaint.style = Paint.Style.FILL
        
        ripplePaint.color = rippleColor
        ripplePaint.style = Paint.Style.FILL
        ripplePaint.alpha = 0
        
        // Enable click feedback without system overlay
        isClickable = true
        isFocusable = true
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Calculate maximum ripple radius based on button size
        maxRippleRadius = Math.hypot(width.toDouble(), height.toDouble()).toFloat()
        
        // Update button rect
        buttonRect.set(0f, 0f, width.toFloat(), height.toFloat())
    }
    
    override fun onDraw(canvas: Canvas) {
        // Save canvas state
        val save = canvas.save()
        
        // Apply scale transformation
        if (currentScale != 1.0f) {
            val scaleFactor = currentScale
            canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        }
        
        // Draw button background with rounded corners
        canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, backgroundPaint)
        
        // Draw ripple effect if active
        if (rippleAlpha > 0) {
            ripplePaint.alpha = rippleAlpha
            canvas.drawCircle(rippleCenterX, rippleCenterY, rippleRadius, ripplePaint)
        }
        
        // Restore canvas
        canvas.restoreToCount(save)
        
        // Draw text and other content
        super.onDraw(canvas)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Store touch position for ripple
                rippleCenterX = event.x
                rippleCenterY = event.y
                
                // Start press animation
                animateScale(1.0f, pressedScale)
                startRippleAnimation()
                return true
            }
            MotionEvent.ACTION_UP -> {
                // Animate back to normal size
                animateScale(currentScale, 1.0f)
                
                // Check if touch is within bounds to trigger click
                if (isPressed && isEnabled) {
                    performClick()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                // Animate back to normal on cancel
                animateScale(currentScale, 1.0f)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun animateScale(fromScale: Float, toScale: Float) {
        // Cancel any running animation
        scaleAnimator?.cancel()
        
        // Create new scale animation
        scaleAnimator = ValueAnimator.ofFloat(fromScale, toScale).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                currentScale = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    private fun startRippleAnimation() {
        // Cancel any running ripple animation
        rippleAnimator?.cancel()
        
        // Reset ripple properties
        rippleRadius = 0f
        rippleAlpha = 70
        
        // Create new ripple animation
        rippleAnimator = ValueAnimator.ofFloat(0f, maxRippleRadius).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                rippleRadius = animation.animatedValue as Float
                // Fade out as the ripple grows
                rippleAlpha = (70 * (1 - animation.animatedFraction)).toInt()
                invalidate()
            }
            doOnEnd {
                rippleAlpha = 0
                invalidate()
            }
            start()
        }
    }
    
    /**
     * Set the background color of the button
     */
    fun setButtonColor(color: Int) {
        buttonColor = color
        backgroundPaint.color = color
        invalidate()
    }
    
    /**
     * Set the ripple color
     */
    fun setRippleColor(color: Int) {
        rippleColor = color
        ripplePaint.color = color
        invalidate()
    }
    
    /**
     * Set the corner radius
     */
    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Clean up animators
        scaleAnimator?.cancel()
        rippleAnimator?.cancel()
        scaleAnimator = null
        rippleAnimator = null
    }
} 