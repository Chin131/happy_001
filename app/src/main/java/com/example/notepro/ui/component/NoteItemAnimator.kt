package com.example.notepro.ui.component

import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom item animator for RecyclerView that provides smooth animations for notes list
 */
class NoteItemAnimator : DefaultItemAnimator() {

    init {
        // Set durations for animations
        addDuration = 300
        removeDuration = 300
        changeDuration = 300
        moveDuration = 300
        
        // Don't move items immediately when they change
        supportsChangeAnimations = true
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        // Reset view initial state
        holder.itemView.alpha = 0f
        holder.itemView.translationY = holder.itemView.height.toFloat() * 0.3f
        
        // Create animation
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(addDuration)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(null)
            .withEndAction {
                dispatchAddFinished(holder)
            }
            .start()
        
        return false // We handle the animation ourselves
    }
    
    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        // Create animation
        holder.itemView.animate()
            .alpha(0f)
            .translationX(holder.itemView.width.toFloat())
            .setDuration(removeDuration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(null)
            .withEndAction {
                dispatchRemoveFinished(holder)
            }
            .start()
        
        return false // We handle the animation ourselves
    }
    
    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        if (oldHolder === newHolder) {
            // Item changed but still same instance, just use a fade through
            oldHolder.itemView.animate()
                .alpha(0f)
                .setDuration(changeDuration / 2)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    oldHolder.itemView.animate()
                        .alpha(1f)
                        .setDuration(changeDuration / 2)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .setListener(null)
                        .withEndAction {
                            dispatchChangeFinished(oldHolder, true)
                        }
                        .start()
                }
                .start()
        } else {
            // Old item is animating out, new item animating in
            oldHolder.itemView.animate()
                .alpha(0f)
                .translationX(-oldHolder.itemView.width.toFloat())
                .setDuration(changeDuration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(null)
                .withEndAction {
                    dispatchChangeFinished(oldHolder, true)
                }
                .start()
                
            newHolder.itemView.translationX = newHolder.itemView.width.toFloat()
            newHolder.itemView.alpha = 0f
            newHolder.itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(changeDuration)
                .setInterpolator(FastOutSlowInInterpolator())
                .setListener(null)
                .withEndAction {
                    dispatchChangeFinished(newHolder, false)
                }
                .start()
        }
        
        return false // We handle the animation ourselves
    }
    
    override fun animateMove(
        holder: RecyclerView.ViewHolder,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        
        holder.itemView.translationX = -deltaX.toFloat()
        holder.itemView.translationY = -deltaY.toFloat()
        
        holder.itemView.animate()
            .translationX(0f)
            .translationY(0f)
            .setDuration(moveDuration)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(null)
            .withEndAction {
                dispatchMoveFinished(holder)
            }
            .start()
            
        return false // We handle the animation ourselves
    }
    
    override fun endAnimation(item: RecyclerView.ViewHolder) {
        item.itemView.animate().cancel()
        super.endAnimation(item)
    }
    
    override fun endAnimations() {
        for (i in (itemPendingAnimations.size() - 1) downTo 0) {
            val holder = itemPendingAnimations.keyAt(i)
            endAnimation(holder)
        }
        super.endAnimations()
    }
} 