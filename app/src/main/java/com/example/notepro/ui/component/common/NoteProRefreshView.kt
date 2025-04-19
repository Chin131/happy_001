package com.example.notepro.ui.component.common

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.notepro.R

/**
 * Custom SwipeRefreshLayout with a branded animation using a rotating icon
 */
class NoteProRefreshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {
    
    private val refreshIconView: View
    private var rotationAnimator: ObjectAnimator? = null

    init {
        // Set up the custom refresh view
        val customRefreshView = LayoutInflater.from(context)
            .inflate(R.layout.view_custom_refresh, null) as FrameLayout
        
        refreshIconView = customRefreshView.findViewById(R.id.refresh_icon)
        
        // Configure SwipeRefreshLayout
        setColorSchemeResources(R.color.colorPrimary)
        setProgressBackgroundColorSchemeResource(R.color.colorBackground)
        
        // Set custom animation
        setOnRefreshListener {
            startRotationAnimation()
        }
    }
    
    /**
     * Starts the rotation animation of the refresh icon
     */
    private fun startRotationAnimation() {
        rotationAnimator?.cancel()
        
        rotationAnimator = ObjectAnimator.ofFloat(refreshIconView, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    /**
     * Stops the rotation animation
     */
    override fun setRefreshing(refreshing: Boolean) {
        super.setRefreshing(refreshing)
        
        if (!refreshing) {
            rotationAnimator?.cancel()
            rotationAnimator = null
        } else if (rotationAnimator == null) {
            startRotationAnimation()
        }
    }
} 