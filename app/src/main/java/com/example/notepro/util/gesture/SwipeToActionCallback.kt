package com.example.notepro.util.gesture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.notepro.R
import com.example.notepro.ui.screen.home.NotesAdapter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SwipeToActionCallback(
    private val context: Context,
    private val onSwipeDelete: (position: Int) -> Unit,
    private val onSwipeArchive: (position: Int) -> Unit,
    private val onSwipePin: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
    private val archiveIcon = ContextCompat.getDrawable(context, R.drawable.ic_archive)
    private val pinIcon = ContextCompat.getDrawable(context, R.drawable.ic_pin)
    
    private val deletePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.delete_background)
    }
    
    private val archivePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.archive_background)
    }
    
    private val pinPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pin_background)
    }
    
    private val iconMargin = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_margin)
    private val iconSize = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_size)
    
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false
    
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
        
        when (direction) {
            ItemTouchHelper.LEFT -> onSwipeDelete(position)
            ItemTouchHelper.RIGHT -> {
                // If swiped more than 50% to the right, archive; otherwise pin
                val adapter = viewHolder.bindingAdapter as? NotesAdapter
                val note = adapter?.getNoteAt(position) ?: return
                
                if (note.isPinned) {
                    onSwipeArchive(position)
                } else {
                    onSwipePin(position)
                }
            }
        }
    }
    
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.4f
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
        val isCanceled = dX == 0f && !isCurrentlyActive
        
        if (isCanceled) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }
        
        val adapter = viewHolder.bindingAdapter as? NotesAdapter
        val position = viewHolder.bindingAdapterPosition
        val note = adapter?.getNoteAt(position)
        val isPinned = note?.isPinned ?: false
        
        // Draw background
        when {
            dX < 0 -> { // Swipe Left (Delete)
                val background = Rect(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                c.drawRect(background, deletePaint)
                
                // Draw icon
                deleteIcon?.let {
                    val iconMarginRight = max(iconMargin, abs(dX.toInt()) / 4)
                    val iconLeft = itemView.right - iconMarginRight - iconSize
                    val iconTop = itemView.top + (itemView.height - iconSize) / 2
                    val iconRight = itemView.right - iconMarginRight
                    val iconBottom = iconTop + iconSize
                    
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }
            }
            dX > 0 -> { // Swipe Right (Archive or Pin)
                val background = Rect(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
                )
                
                val paint = if (isPinned) archivePaint else pinPaint
                c.drawRect(background, paint)
                
                // Draw icon
                val icon = if (isPinned) archiveIcon else pinIcon
                icon?.let {
                    val iconMarginLeft = max(iconMargin, abs(dX.toInt()) / 4)
                    val iconLeft = itemView.left + iconMarginLeft
                    val iconTop = itemView.top + (itemView.height - iconSize) / 2
                    val iconRight = itemView.left + iconMarginLeft + iconSize
                    val iconBottom = iconTop + iconSize
                    
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }
            }
        }
        
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
} 