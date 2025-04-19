package com.example.notepro.ui.component

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.notepro.R

/**
 * A view that displays an empty state with animations
 */
class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView
    private val titleTextView: TextView
    private val messageTextView: TextView
    private val actionButton: View
    
    private var isAnimating = false
    private var animatorSet: AnimatorSet? = null
    
    init {
        LayoutInflater.from(context).inflate(R.layout.view_empty_state, this, true)
        
        imageView = findViewById(R.id.emptyStateImage)
        titleTextView = findViewById(R.id.emptyStateTitle)
        messageTextView = findViewById(R.id.emptyStateMessage)
        actionButton = findViewById(R.id.emptyStateAction)
        
        // Apply custom attributes if specified
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.EmptyStateView,
            0, 0
        ).apply {
            try {
                val title = getString(R.styleable.EmptyStateView_emptyStateTitle)
                val message = getString(R.styleable.EmptyStateView_emptyStateMessage)
                val imageResId = getResourceId(R.styleable.EmptyStateView_emptyStateImage, R.drawable.ic_empty_notes)
                val actionText = getString(R.styleable.EmptyStateView_emptyStateActionText)
                
                setTitle(title)
                setMessage(message)
                setImage(imageResId)
                setActionText(actionText)
                
                // Hide action button if no text is provided
                if (actionText.isNullOrEmpty()) {
                    actionButton.visibility = View.GONE
                }
            } finally {
                recycle()
            }
        }
    }
    
    /**
     * Set the title text
     */
    fun setTitle(title: String?) {
        titleTextView.text = title
        titleTextView.visibility = if (title.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
    
    /**
     * Set the message text
     */
    fun setMessage(message: String?) {
        messageTextView.text = message
        messageTextView.visibility = if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
    
    /**
     * Set the image resource
     */
    fun setImage(resourceId: Int) {
        imageView.setImageResource(resourceId)
    }
    
    /**
     * Set the action button text
     */
    fun setActionText(text: String?) {
        if (actionButton is TextView) {
            (actionButton as TextView).text = text
        }
        actionButton.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
    
    /**
     * Set the action button click listener
     */
    fun setActionClickListener(listener: OnClickListener) {
        actionButton.setOnClickListener(listener)
    }
    
    /**
     * Show the view with entrance animation
     */
    fun showWithAnimation() {
        visibility = View.VISIBLE
        cancelAnimation()
        
        // Prepare views for animation
        alpha = 0f
        imageView.scaleX = 0.5f
        imageView.scaleY = 0.5f
        titleTextView.translationY = 50f
        messageTextView.translationY = 50f
        actionButton.translationY = 50f
        
        // Create animations
        val fadeIn = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
        fadeIn.duration = 400
        
        val imageScaleX = ObjectAnimator.ofFloat(imageView, "scaleX", 0.5f, 1f)
        val imageScaleY = ObjectAnimator.ofFloat(imageView, "scaleY", 0.5f, 1f)
        imageScaleX.duration = 500
        imageScaleY.duration = 500
        
        val titleSlideUp = ObjectAnimator.ofFloat(titleTextView, "translationY", 50f, 0f)
        titleSlideUp.duration = 400
        titleSlideUp.startDelay = 100
        
        val messageSlideUp = ObjectAnimator.ofFloat(messageTextView, "translationY", 50f, 0f)
        messageSlideUp.duration = 400
        messageSlideUp.startDelay = 150
        
        val actionSlideUp = ObjectAnimator.ofFloat(actionButton, "translationY", 50f, 0f)
        actionSlideUp.duration = 400
        actionSlideUp.startDelay = 200
        
        // Play together
        animatorSet = AnimatorSet()
        animatorSet?.interpolator = AccelerateDecelerateInterpolator()
        animatorSet?.playTogether(fadeIn, imageScaleX, imageScaleY, titleSlideUp, messageSlideUp, actionSlideUp)
        animatorSet?.start()
        
        isAnimating = true
    }
    
    /**
     * Hide the view with exit animation
     */
    fun hideWithAnimation(onComplete: () -> Unit = {}) {
        if (visibility != View.VISIBLE) {
            onComplete()
            return
        }
        
        cancelAnimation()
        
        // Create fade out animation
        val fadeOut = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)
        fadeOut.duration = 300
        
        animatorSet = AnimatorSet()
        animatorSet?.interpolator = AccelerateDecelerateInterpolator()
        animatorSet?.play(fadeOut)
        animatorSet?.withEndAction {
            visibility = View.GONE
            isAnimating = false
            onComplete()
        }
        animatorSet?.start()
        
        isAnimating = true
    }
    
    private fun cancelAnimation() {
        if (isAnimating) {
            animatorSet?.cancel()
            isAnimating = false
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAnimation()
    }
} 