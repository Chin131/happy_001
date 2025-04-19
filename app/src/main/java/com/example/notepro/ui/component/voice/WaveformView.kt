package com.example.notepro.ui.component.voice

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/**
 * A view that displays an animated waveform to represent audio recording
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.parseColor("#2196F3")  // Blue color
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val amplitudes = FloatArray(40) { 0f }
    private var animator: ValueAnimator? = null
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2
        
        val barWidth = width / amplitudes.size
        
        for (i in amplitudes.indices) {
            val startX = i * barWidth
            val amplitude = amplitudes[i]
            
            // Draw the bar
            canvas.drawLine(
                startX + barWidth / 2,
                centerY - amplitude,
                startX + barWidth / 2,
                centerY + amplitude,
                paint
            )
        }
    }
    
    fun startAnimation() {
        stopAnimation()
        
        // Animate the waveform
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener {
                // Update amplitudes with random values
                for (i in amplitudes.indices) {
                    // Generate random amplitudes between 5 and 30
                    amplitudes[i] = Random.nextFloat() * 25f + 5f
                }
                invalidate()
            }
            duration = 100
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }
    
    fun stopAnimation() {
        animator?.cancel()
        animator = null
        
        // Reset amplitudes
        for (i in amplitudes.indices) {
            amplitudes[i] = 0f
        }
        invalidate()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
} 