package com.example.notepro.ui.component.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notepro.R

class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val colorAdapter = ColorAdapter()
    private var onColorSelectedListener: ((Int) -> Unit)? = null
    
    init {
        layoutManager = GridLayoutManager(context, 5)
        adapter = colorAdapter
        
        // 设置默认颜色列表
        colorAdapter.submitList(DEFAULT_COLORS)
        
        colorAdapter.onColorClickListener = { color ->
            onColorSelectedListener?.invoke(color)
            colorAdapter.setSelectedColor(color)
        }
    }
    
    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        onColorSelectedListener = listener
    }
    
    fun setSelectedColor(color: Int) {
        colorAdapter.setSelectedColor(color)
    }
    
    fun setColors(colors: List<Int>) {
        colorAdapter.submitList(colors)
    }
    
    companion object {
        val DEFAULT_COLORS = listOf(
            R.color.note_color_1,
            R.color.note_color_2,
            R.color.note_color_3,
            R.color.note_color_4,
            R.color.note_color_5,
            R.color.note_color_6,
            R.color.note_color_7,
            R.color.note_color_8
        ).map { colorResId ->
            // Resolve the resource ID to actual color integer
            android.graphics.Color.parseColor(
                when (colorResId) {
                    R.color.note_color_1 -> "#FAE2B4"
                    R.color.note_color_2 -> "#FFD6C4"
                    R.color.note_color_3 -> "#D4EEFF"
                    R.color.note_color_4 -> "#E0FFC9"
                    R.color.note_color_5 -> "#FFD6E8"
                    R.color.note_color_6 -> "#E9D4FF"
                    R.color.note_color_7 -> "#D6F5FF"
                    R.color.note_color_8 -> "#FFD6D6"
                    else -> "#FFFFFF"
                }
            )
        }
    }
    
    private class ColorAdapter : ListAdapter<Int, ColorViewHolder>(ColorDiffCallback()) {
        var onColorClickListener: ((Int) -> Unit)? = null
        private var selectedColor: Int? = null
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_color, parent, false)
            return ColorViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            val color = getItem(position)
            holder.bind(color, color == selectedColor, onColorClickListener)
        }
        
        fun setSelectedColor(color: Int) {
            val oldSelected = selectedColor
            selectedColor = color
            
            if (oldSelected != null) {
                val oldPosition = currentList.indexOf(oldSelected)
                if (oldPosition != -1) notifyItemChanged(oldPosition)
            }
            
            val newPosition = currentList.indexOf(color)
            if (newPosition != -1) notifyItemChanged(newPosition)
        }
    }
    
    private class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorView: View = itemView.findViewById(R.id.color_view)
        private val selectedIndicator: ImageView = itemView.findViewById(R.id.selected_indicator)
        
        fun bind(color: Int, isSelected: Boolean, onClickListener: ((Int) -> Unit)?) {
            colorView.setBackgroundColor(color)
            selectedIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            // 计算文本颜色（黑或白）
            val luminance = ColorUtils.calculateLuminance(color)
            selectedIndicator.setColorFilter(
                if (luminance > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
            
            itemView.setOnClickListener {
                onClickListener?.invoke(color)
            }
        }
    }
    
    private class ColorDiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }
        
        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }
    }
} 