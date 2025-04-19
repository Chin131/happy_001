package com.example.notepro.ui.component.editor

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import com.example.notepro.R
import com.google.android.material.button.MaterialButton
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher

/**
 * A custom view that provides Markdown editing functionality.
 */
class MarkdownEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val editText: EditText
    private val formatToolbar: HorizontalScrollView
    private val boldButton: MaterialButton
    private val italicButton: MaterialButton
    private val listButton: MaterialButton
    private val checklistButton: MaterialButton
    private val headerButton: MaterialButton
    private val quoteButton: MaterialButton
    
    private val markwon: Markwon = Markwon.create(context)
    private val editor: MarkwonEditor = MarkwonEditor.create(markwon)
    
    private var onTextChangedListener: ((String) -> Unit)? = null
    
    init {
        orientation = VERTICAL
        
        // Inflate the layout
        LayoutInflater.from(context).inflate(R.layout.view_markdown_editor, this, true)
        
        // Initialize views
        editText = findViewById(R.id.markdown_edit_text)
        formatToolbar = findViewById(R.id.format_toolbar)
        boldButton = findViewById(R.id.button_bold)
        italicButton = findViewById(R.id.button_italic)
        listButton = findViewById(R.id.button_list)
        checklistButton = findViewById(R.id.button_checklist)
        headerButton = findViewById(R.id.button_header)
        quoteButton = findViewById(R.id.button_quote)
        
        // Set up Markwon editor
        editText.addTextChangedListener(MarkwonEditorTextWatcher.withProcess(editor))
        
        // Add text change listener
        editText.addTextChangedListener {
            onTextChangedListener?.invoke(it?.toString() ?: "")
        }
        
        // Set up format buttons
        setupFormatButtons()
    }
    
    private fun setupFormatButtons() {
        boldButton.setOnClickListener {
            insertMarkdownSyntax("**", "**", R.string.bold_text)
        }
        
        italicButton.setOnClickListener {
            insertMarkdownSyntax("*", "*", R.string.italic_text)
        }
        
        listButton.setOnClickListener {
            insertAtLineStart("- ", R.string.list_item)
        }
        
        checklistButton.setOnClickListener {
            insertAtLineStart("- [ ] ", R.string.checklist_item)
        }
        
        headerButton.setOnClickListener {
            insertAtLineStart("# ", R.string.header_text)
        }
        
        quoteButton.setOnClickListener {
            insertAtLineStart("> ", R.string.quote_text)
        }
    }
    
    private fun insertMarkdownSyntax(prefix: String, suffix: String, defaultTextResId: Int) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val text = editText.text
        
        if (start == end) {
            // No selection, insert with default text
            val defaultText = context.getString(defaultTextResId)
            text.insert(start, "$prefix$defaultText$suffix")
            editText.setSelection(start + prefix.length, start + prefix.length + defaultText.length)
        } else {
            // Wrap selection with markdown syntax
            text.insert(end, suffix)
            text.insert(start, prefix)
            editText.setSelection(start + prefix.length, end + prefix.length)
        }
    }
    
    private fun insertAtLineStart(prefix: String, defaultTextResId: Int) {
        val start = editText.selectionStart
        val text = editText.text
        
        // Find the start of the line
        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }
        
        // Insert prefix at the beginning of the line
        text.insert(lineStart, prefix)
        
        if (start == lineStart) {
            // Cursor was at the beginning of the line, insert default text
            val defaultText = context.getString(defaultTextResId)
            text.insert(lineStart + prefix.length, defaultText)
            editText.setSelection(lineStart + prefix.length, lineStart + prefix.length + defaultText.length)
        }
    }
    
    fun setText(text: String) {
        editText.setText(text)
    }
    
    fun getText(): String {
        return editText.text.toString()
    }
    
    fun setOnTextChangedListener(listener: (String) -> Unit) {
        onTextChangedListener = listener
    }
} 