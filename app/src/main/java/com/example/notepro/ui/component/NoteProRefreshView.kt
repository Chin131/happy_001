package com.example.notepro.ui.component

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.notepro.R

/**
 * Custom refresh view with animated transitions for SwipeRefreshLayout
 */
class NoteProRefreshView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPrimary)
        style = Paint.Style.FILL
    }
    
    private val path = Path()
    private val bounds = RectF()
    
    private var rotationAnimator: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null
    
    private var currentRotation = 0f
    private var currentScale = 0f
    
    private var isRefreshing = false
    
    /**
     * Connect this view to a SwipeRefreshLayout
     */
    fun attachToSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout) {
        swipeRefreshLayout.setOnRefreshListener {
            startRefreshingAnimation()
        }
        
        // Custom progress view
        swipeRefreshLayout.setProgressViewOffset(
            false,
            resources.getDimensionPixelSize(R.dimen.refresh_offset_start),
            resources.getDimensionPixelSize(R.dimen.refresh_offset_end)
        )
        
        swipeRefreshLayout.progressViewEndTarget = false,
            resources.getDimensionPixelSize(R.dimen.refresh_offset_end)
    }
    
    fun startRefreshingAnimation() {
        isRefreshing = true
        startRotationAnimation()
        startScaleAnimation()
    }
    
    fun stopRefreshingAnimation() {
        isRefreshing = false
        rotationAnimator?.cancel()
        scaleAnimator?.cancel()
        
        // Animate scale down
        ValueAnimator.ofFloat(currentScale, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                currentScale = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    private fun startRotationAnimation() {
        rotationAnimator?.cancel()
        
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animator ->
                currentRotation = animator.animatedValue as Float
                invalidate()
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    currentRotation = 0f
                }
            })
            
            start()
        }
    }
    
    private fun startScaleAnimation() {
        scaleAnimator?.cancel()
        
        scaleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animator ->
                currentScale = animator.animatedValue as Float
                invalidate()
            }
            
            start()
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val size = Math.min(width, height) * 0.4f * currentScale
        
        canvas.save()
        canvas.rotate(currentRotation, centerX, centerY)
        
        // Draw a stylized note icon
        bounds.set(centerX - size, centerY - size, centerX + size, centerY + size)
        path.reset()
        
        // Draw note shape
        path.moveTo(bounds.left + bounds.width() * 0.25f, bounds.top)
        path.lineTo(bounds.right, bounds.top)
        path.lineTo(bounds.right, bounds.bottom)
        path.lineTo(bounds.left, bounds.bottom)
        path.lineTo(bounds.left, bounds.top + bounds.height() * 0.25f)
        path.lineTo(bounds.left + bounds.width() * 0.25f, bounds.top)
        path.close()
        
        canvas.drawPath(path, paint)
        
        // Draw lines representing text
        val lineSpacing = size * 0.2f
        val lineStart = bounds.left + size * 0.2f
        val lineEnd = bounds.right - size * 0.2f
        val firstLineY = bounds.top + size * 0.4f
        
        paint.strokeWidth = size * 0.05f
        
        for (i in 0..2) {
            val lineY = firstLineY + i * lineSpacing
            canvas.drawLine(lineStart, lineY, lineEnd, lineY, paint)
        }
        
        canvas.restore()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rotationAnimator?.cancel()
        scaleAnimator?.cancel()
    }
} 