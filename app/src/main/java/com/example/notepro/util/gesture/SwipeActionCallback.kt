package com.example.notepro.util.gesture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.notepro.R
import kotlin.math.abs
import kotlin.math.min

/**
 * A callback for implementing swipe actions on RecyclerView items.
 * Supports left and right swipe with different actions and icons.
 */
class SwipeActionCallback(
    private val context: Context,
    private val onSwipeLeft: ((Int) -> Unit)? = null,
    private val onSwipeRight: ((Int) -> Unit)? = null,
    private val leftBackgroundColor: Int = ContextCompat.getColor(context, R.color.delete_background),
    private val rightBackgroundColor: Int = ContextCompat.getColor(context, R.color.archive_background),
    private val leftIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_delete),
    private val rightIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_archive)
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val iconMargin = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_margin)
    private val iconSize = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_size)
    private val background = ColorDrawable()
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun onMove(
        recyclerView: RecyclerView, 
        viewHolder: RecyclerView.ViewHolder, 
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        when (direction) {
            ItemTouchHelper.LEFT -> onSwipeLeft?.invoke(position)
            ItemTouchHelper.RIGHT -> onSwipeRight?.invoke(position)
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.4f // Require 40% swipe to trigger action
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return defaultValue * 0.5f // Easier to escape from swipe
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return defaultValue * 0.5f // Lower threshold for swipe velocity
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw background based on swipe direction
        when {
            dX < 0 -> { // Swipe Left
                background.color = leftBackgroundColor
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)

                // Draw icon
                leftIcon?.let {
                    val iconLeft = itemView.right - iconMargin - iconSize
                    val iconRight = itemView.right - iconMargin
                    val iconTop = itemView.top + (itemHeight - iconSize) / 2
                    val iconBottom = iconTop + iconSize

                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }
            }
            dX > 0 -> { // Swipe Right
                background.color = rightBackgroundColor
                background.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
                )
                background.draw(c)

                // Draw icon
                rightIcon?.let {
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + iconSize
                    val iconTop = itemView.top + (itemHeight - iconSize) / 2
                    val iconBottom = iconTop + iconSize

                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }
            }
        }

        // Calculate alpha based on swipe distance
        val alpha = 1.0f - min(1f, abs(dX) / (itemView.width * 0.5f))
        itemView.alpha = alpha

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        c.drawRect(left, top, right, bottom, clearPaint)
    }
} 