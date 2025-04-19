# 安卓记事本应用前端开发全面指南（续）

继续编辑页面实现的代码：

```kotlin
                    is SaveState.Success -> {
                        Snackbar.make(
                            binding.root,
                            R.string.note_saved,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                    is SaveState.Error -> {
                        Snackbar.make(
                            binding.root,
                            state.message,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    else -> { /* 不处理其他状态 */ }
                }
            }
        }
    }
    
    private fun updateUiWithNote(note: Note) {
        binding.titleEditText.setText(note.title)
        binding.markdownEditor.setMarkdown(note.content)
        
        // 设置笔记颜色
        note.color?.let { color ->
            binding.colorPicker.setSelectedColor(color)
        }
        
        // 设置笔记分类
        note.category?.let { category ->
            val position = getPositionForCategory(category)
            if (position != -1) {
                binding.categoryDropdown.setText(category.name, false)
                binding.categoryDropdown.listSelection = position
            }
        }
    }
    
    private fun getPositionForCategory(category: Category): Int {
        for (i in 0 until categoryAdapter.count) {
            if (categoryAdapter.getItem(i)?.id == category.id) {
                return i
            }
        }
        return -1
    }
    
    private fun updateTagsChips(tags: List<Tag>) {
        // 清除除了"添加标签"以外的所有芯片
        val chipCount = binding.tagsChipGroup.childCount
        for (i in chipCount - 1 downTo 0) {
            val chip = binding.tagsChipGroup.getChildAt(i)
            if (chip.id != R.id.add_tag_chip) {
                binding.tagsChipGroup.removeView(chip)
            }
        }
        
        // 添加标签芯片
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag.name
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    viewModel.removeTag(tag)
                }
                
                // 设置标签颜色（如果有）
                tag.color?.let { color ->
                    chipBackgroundColor = ColorStateList.valueOf(color)
                    // 根据背景颜色计算文本颜色
                    val isDark = ColorUtils.calculateLuminance(color) < 0.5
                    setTextColor(if (isDark) Color.WHITE else Color.BLACK)
                    closeIconTint = ColorStateList.valueOf(if (isDark) Color.WHITE else Color.BLACK)
                }
            }
            
            // 将"添加标签"之前插入新标签
            binding.tagsChipGroup.addView(chip, binding.tagsChipGroup.childCount - 1)
        }
    }
    
    private fun showAddTagDialog() {
        val dialog = TagSelectorDialog.newInstance(viewModel.selectedTags.value.map { it.id })
        dialog.setTagSelectedListener { tag ->
            viewModel.addTag(tag)
        }
        dialog.show(childFragmentManager, "TagSelectorDialog")
    }
    
    private fun saveNote() {
        val title = binding.titleEditText.text.toString()
        val content = binding.markdownEditor.getMarkdown()
        
        viewModel.saveNote(title, content)
    }
    
    private fun shareNote() {
        val title = binding.titleEditText.text.toString()
        val content = binding.markdownEditor.getMarkdown()
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_note)))
    }
    
    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_note)
            .setMessage(R.string.delete_note_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteNote()
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

#### 编辑页面 ViewModel:

```kotlin
// ui/screen/editor/EditorViewModel.kt
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote
    
    private val _selectedTags = MutableStateFlow<List<Tag>>(emptyList())
    val selectedTags: StateFlow<List<Tag>> = _selectedTags
    
    private var selectedCategory: Category? = null
    private var selectedColor: Int? = null
    
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState
    
    // 获取所有分类
    val categories = categoryRepository.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    
    // 获取所有标签
    val tags = tagRepository.getAllTags().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    
    fun loadNote(noteId: String) {
        viewModelScope.launch {
            try {
                val note = noteRepository.getNoteById(noteId)
                _currentNote.value = note
                
                // 加载笔记的标签
                val noteTags = tagRepository.getTagsForNote(noteId)
                _selectedTags.value = noteTags
                
                // 设置笔记分类和颜色
                selectedCategory = note.category
                selectedColor = note.color
            } catch (e: Exception) {
                // 处理加载错误
                _saveState.value = SaveState.Error(e.message ?: "Failed to load note")
            }
        }
    }
    
    fun setNoteCategory(category: Category) {
        selectedCategory = category
    }
    
    fun setNoteColor(color: Int) {
        selectedColor = color
    }
    
    fun addTag(tag: Tag) {
        val currentTags = _selectedTags.value.toMutableList()
        if (!currentTags.any { it.id == tag.id }) {
            currentTags.add(tag)
            _selectedTags.value = currentTags
        }
    }
    
    fun removeTag(tag: Tag) {
        val currentTags = _selectedTags.value.toMutableList()
        currentTags.removeAll { it.id == tag.id }
        _selectedTags.value = currentTags
    }
    
    fun saveNote(title: String, content: String) {
        viewModelScope.launch {
            try {
                _saveState.value = SaveState.Saving
                
                val currentNoteId = _currentNote.value?.id
                
                if (currentNoteId != null) {
                    // 更新现有笔记
                    val updatedNote = _currentNote.value!!.copy(
                        title = title,
                        content = content,
                        categoryId = selectedCategory?.id,
                        color = selectedColor,
                        modifiedAt = System.currentTimeMillis()
                    )
                    
                    noteRepository.updateNote(updatedNote)
                    
                    // 更新笔记标签
                    tagRepository.updateNoteTags(currentNoteId, _selectedTags.value.map { it.id })
                } else {
                    // 创建新笔记
                    val newNote = Note(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        content = content,
                        categoryId = selectedCategory?.id,
                        color = selectedColor,
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis()
                    )
                    
                    val insertedId = noteRepository.insertNote(newNote)
                    
                    // 添加笔记标签
                    tagRepository.updateNoteTags(insertedId, _selectedTags.value.map { it.id })
                }
                
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Failed to save note")
            }
        }
    }
    
    fun deleteNote() {
        viewModelScope.launch {
            try {
                _currentNote.value?.let { note ->
                    noteRepository.moveToTrash(note.id)
                }
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Failed to delete note")
            }
        }
    }
}

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}
```

#### 编辑页面（Compose版本）:

```kotlin
// ui/screen/editor/EditorScreen.kt
@Composable
fun EditorScreen(
    viewModel: EditorViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val currentNote by viewModel.currentNote.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    
    val context = LocalContext.current
    
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    
    // 更新UI数据
    LaunchedEffect(currentNote) {
        currentNote?.let {
            title = it.title
            content = it.content
        }
    }
    
    // 处理保存状态
    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveState.Success -> {
                Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                onNavigateUp()
            }
            is SaveState.Error -> {
                Toast.makeText(context, (saveState as SaveState.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            EditorAppBar(
                isExistingNote = currentNote != null,
                onNavigateUp = onNavigateUp,
                onShare = {
                    shareNote(context, title, content)
                },
                onDelete = {
                    showDeleteConfirmation(context) {
                        viewModel.deleteNote()
                        onNavigateUp()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.saveNote(title, content) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Note"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题输入框
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 颜色选择器
            Text(
                text = "Note Color",
                style = MaterialTheme.typography.bodyMedium
            )
            
            ColorPickerGrid(
                colors = ColorPickerView.DEFAULT_COLORS.map { Color(it) },
                selectedColor = currentNote?.color?.let { Color(it) },
                onColorSelected = { viewModel.setNoteColor(it.toArgb()) }
            )
            
            // 分类下拉框
            ExposedDropdownMenuBox(
                modifier = Modifier.fillMaxWidth()
            ) {
                var expanded by remember { mutableStateOf(false) }
                var selectedCategory by remember { mutableStateOf<Category?>(currentNote?.category) }
                
                OutlinedTextField(
                    value = selectedCategory?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .clickable { expanded = true }
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category
                                viewModel.setNoteCategory(category)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // 标签选择
            Column {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    selectedTags.forEach { tag ->
                        TagChip(
                            tag = tag,
                            onDelete = { viewModel.removeTag(tag) }
                        )
                    }
                    
                    AddTagChip(
                        onClick = {
                            // 显示标签选择对话框
                            // 这里简化处理，实际实现需要弹出对话框
                        }
                    )
                }
            }
            
            // 内容编辑器
            Text(
                text = "Content",
                style = MaterialTheme.typography.bodyMedium
            )
            
            MarkdownEditor(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
    }
}

@Composable
fun EditorAppBar(
    isExistingNote: Boolean,
    onNavigateUp: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (isExistingNote) "Edit Note" else "New Note",
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Navigate Up"
                )
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share"
                )
            }
            
            if (isExistingNote) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun TagChip(
    tag: Tag,
    onDelete: () -> Unit
) {
    val chipColor = tag.color?.let { Color(it) } ?: MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (chipColor.luminance() > 0.5f) Color.Black else Color.White
    
    ElevatedFilterChip(
        selected = true,
        onClick = {},
        label = { Text(tag.name, color = textColor) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove Tag",
                tint = textColor,
                modifier = Modifier.clickable { onDelete() }
            )
        },
        colors = FilterChipDefaults.elevatedFilterChipColors(
            containerColor = chipColor,
            selectedContainerColor = chipColor
        )
    )
}

@Composable
fun AddTagChip(
    onClick: () -> Unit
) {
    ElevatedFilterChip(
        selected = false,
        onClick = onClick,
        label = { Text("Add Tag") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Tag"
            )
        }
    )
}

private fun shareNote(context: Context, title: String, content: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    
    context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
}

private fun showDeleteConfirmation(context: Context, onConfirm: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("Delete Note")
        .setMessage("Are you sure you want to delete this note?")
        .setPositiveButton("Delete") { _, _ -> onConfirm() }
        .setNegativeButton("Cancel", null)
        .show()
}
```

## 第四阶段：动效设计与实现

现在我们已经实现了主要的页面和功能，接下来将重点放在动效设计上，提升应用的用户体验。

### 1. 列表项动画

为笔记列表添加动画效果，使得列表操作更加流畅自然：

```kotlin
// util/animation/ItemAnimator.kt
class NoteItemAnimator : DefaultItemAnimator() {

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        holder.itemView.alpha = 0f
        holder.itemView.translationY = holder.itemView.height.toFloat() * 0.3f
        
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
        
        return true
    }
    
    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        holder.itemView.animate()
            .alpha(0f)
            .translationX(holder.itemView.width.toFloat())
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        
        return true
    }
}
```

### 2. 页面转场动画

使用共享元素转场动画，使得从列表到详情页的过渡更加平滑：

```kotlin
// 在NotesAdapter中设置共享元素转场
class NotesAdapter(
    private val onNoteClick: (Note, View) -> Unit,
    // 其他参数...
) : ListAdapter<Note, NoteViewHolder>(NoteDiffCallback()) {

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        
        // 设置共享元素转场的名称
        holder.noteCard.transitionName = "note_card_${note.id}"
        holder.titleText.transitionName = "note_title_${note.id}"
        
        holder.itemView.setOnClickListener {
            // 将视图元素传递给回调
            onNoteClick(note, holder.noteCard)
        }
        
        // 其他绑定代码...
    }
}

// 在HomeFragment中使用共享元素进行导航
class HomeFragment : Fragment() {
    
    // 其他代码...
    
    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onNoteClick = { note, sharedElement ->
                // 创建共享元素过渡
                val extras = FragmentNavigatorExtras(
                    sharedElement to "note_card_transition"
                )
                
                // 使用共享元素导航
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToEditorFragment(noteId = note.id),
                    extras
                )
            },
            // 其他参数...
        )
        
        // 其他设置...
    }
}

// 在EditorFragment中设置转场
class EditorFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用延迟转场
        postponeEnterTransition()
        
        // 设置共享元素转场
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 300L
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().getColor(R.color.background))
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 在视图准备就绪后启动延迟的转场
        view.doOnPreDraw { startPostponedEnterTransition() }
        
        // 其他代码...
    }
}
```

### 3. 状态转换动画

使用MotionLayout实现更复杂的状态转换动画，如笔记卡片展开/折叠的动画效果：

```xml
<!-- res/layout/item_note_expandable.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.motion.widget.MotionLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/motion_layout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:layoutDescription="@xml/scene_note_item">

    <com.google.android.material.card.MaterialCardView
        android:id="@+id/note_card"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="8dp"
        app:cardCornerRadius="12dp"
        app:cardElevation="2dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">

        <androidx.constraintlayout.widget.ConstraintLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="16dp">

            <TextView
                android:id="@+id/note_title"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:ellipsize="end"
                android:maxLines="1"
                android:textAppearance="?attr/textAppearanceTitleMedium"
                app:layout_constraintEnd_toStartOf="@+id/expand_button"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent"
                tools:text="Meeting Notes" />

            <TextView
                android:id="@+id/note_content"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:ellipsize="end"
                android:maxLines="2"
                android:textAppearance="?attr/textAppearanceBodyMedium"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@id/note_title"
                tools:text="This is a sample note content with multiple lines of text to demonstrate how the card will look with actual content." />

            <TextView
                android:id="@+id/note_date"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:textAppearance="?attr/textAppearanceBodySmall"
                android:textColor="?attr/colorOnSurfaceVariant"
                android:alpha="0.7"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@id/note_content"
                tools:text="Nov 15, 2023" />

            <ImageButton
                android:id="@+id/expand_button"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="@string/expand_note"
                android:padding="8dp"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintTop_toTopOf="parent"
                app:srcCompat="@drawable/ic_expand_more" />

            <LinearLayout
                android:id="@+id/actions_container"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:orientation="horizontal"
                android:visibility="gone"
                app:layout_constraintBottom_toBottomOf="parent"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toBottomOf="@id/note_date">

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/edit_button"
                    style="@style/Widget.Material3.Button.TextButton"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/edit" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/share_button"
                    style="@style/Widget.Material3.Button.TextButton"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/share" />

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/delete_button"
                    style="@style/Widget.Material3.Button.TextButton"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/delete" />
            </LinearLayout>
        </androidx.constraintlayout.widget.ConstraintLayout>
    </com.google.android.material.card.MaterialCardView>
</androidx.constraintlayout.motion.widget.MotionLayout>
```

```xml
<!-- res/xml/scene_note_item.xml -->
<?xml version="1.0" encoding="utf-8"?>
<MotionScene xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:motion="http://schemas.android.com/apk/res-auto">

    <Transition
        motion:constraintSetEnd="@+id/expanded"
        motion:constraintSetStart="@+id/collapsed"
        motion:duration="300">
        <OnClick
            motion:clickAction="toggle"
            motion:targetId="@id/expand_button" />
    </Transition>

    <ConstraintSet android:id="@+id/collapsed">
        <Constraint
            android:id="@id/note_content"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:ellipsize="end"
            android:maxLines="2"
            motion:layout_constraintEnd_toEndOf="parent"
            motion:layout_constraintStart_toStartOf="parent"
            motion:layout_constraintTop_toBottomOf="@id/note_title" />
            
        <Constraint
            android:id="@id/expand_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:rotation="0"
            motion:layout_constraintEnd_toEndOf="parent"
            motion:layout_constraintTop_toTopOf="parent" />
            
        <Constraint
            android:id="@id/actions_container"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:visibility="gone"
            motion:layout_constraintTop_toBottomOf="@id/note_date" />
    </ConstraintSet>

    <ConstraintSet android:id="@+id/expanded">
        <Constraint
            android:id="@id/note_content"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:ellipsize="end"
            android:maxLines="10"
            motion:layout_constraintEnd_toEndOf="parent"
            motion:layout_constraintStart_toStartOf="parent"
            motion:layout_constraintTop_toBottomOf="@id/note_title" />
            
        <Constraint
            android:id="@id/expand_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:rotation="180"
            motion:layout_constraintEnd_toEndOf="parent"
            motion:layout_constraintTop_toTopOf="parent" />
            
        <Constraint
            android:id="@id/actions_container"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:visibility="visible"
            motion:layout_constraintTop_toBottomOf="@id/note_date" />
    </ConstraintSet>
</MotionScene>
```

### 4. 微交互动画

添加精致的微交互