# 安卓记事本应用前端开发全面指南（续）

## 第七阶段：高级功能实现

现在我们已经完成了应用的核心功能、UI优化和测试，接下来将实现一些高级功能，进一步提升用户体验和应用竞争力。

### 1. 搜索功能实现

强大的搜索功能是一个好记事本应用的必备特性，以下是完整的搜索功能实现：

#### 搜索页面布局:

```xml
<!-- res/layout/fragment_search.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/appbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="?attr/colorSurface">

        <androidx.appcompat.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:navigationIcon="@drawable/ic_back"
            app:title="@string/search">

            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/search_input_layout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginEnd="16dp"
                app:endIconMode="clear_text"
                app:endIconDrawable="@drawable/ic_clear"
                app:startIconDrawable="@drawable/ic_search"
                app:boxStrokeWidth="0dp"
                app:boxStrokeWidthFocused="0dp"
                app:hintEnabled="false">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/search_edit_text"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="text"
                    android:imeOptions="actionSearch"
                    android:hint="@string/search_notes"
                    android:background="@android:color/transparent" />

            </com.google.android.material.textfield.TextInputLayout>

        </androidx.appcompat.widget.Toolbar>

        <com.google.android.material.chip.ChipGroup
            android:id="@+id/filter_chip_group"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginHorizontal="16dp"
            android:layout_marginBottom="8dp"
            app:singleSelection="false"
            app:chipSpacingHorizontal="8dp">

            <com.google.android.material.chip.Chip
                android:id="@+id/filter_title"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/title"
                app:chipIcon="@drawable/ic_title"
                android:checkable="true"
                android:checked="true"
                style="@style/Widget.Material3.Chip.Filter" />

            <com.google.android.material.chip.Chip
                android:id="@+id/filter_content"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/content"
                app:chipIcon="@drawable/ic_notes"
                android:checkable="true"
                android:checked="true"
                style="@style/Widget.Material3.Chip.Filter" />

            <com.google.android.material.chip.Chip
                android:id="@+id/filter_tags"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/tags"
                app:chipIcon="@drawable/ic_tag"
                android:checkable="true"
                android:checked="false"
                style="@style/Widget.Material3.Chip.Filter" />

        </com.google.android.material.chip.ChipGroup>

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/search_results_recycler_view"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:clipToPadding="false"
            android:padding="8dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            tools:listitem="@layout/item_note" />

        <include
            android:id="@+id/empty_search_view"
            layout="@layout/layout_empty_search"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:visibility="gone"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <com.airbnb.lottie.LottieAnimationView
            android:id="@+id/loading_animation"
            android:layout_width="200dp"
            android:layout_height="200dp"
            android:visibility="gone"
            app:lottie_autoPlay="true"
            app:lottie_loop="true"
            app:lottie_rawRes="@raw/loading_animation"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### 搜索Fragment实现:

```kotlin
// ui/screen/search/SearchFragment.kt
@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchAdapter: NotesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupSearchInput()
        setupFilterChips()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupSearchInput() {
        binding.searchEditText.apply {
            // 设置文本变化监听
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setSearchQuery(s.toString())
                }
            })
            
            // 设置键盘搜索按钮监听
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // 隐藏键盘
                    hideKeyboard()
                    return@setOnEditorActionListener true
                }
                false
            }
            
            // 获取焦点并显示键盘
            requestFocus()
            showKeyboard()
        }
    }
    
    private fun setupFilterChips() {
        // 标题过滤器
        binding.filterTitle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTitleFilter(isChecked)
        }
        
        // 内容过滤器
        binding.filterContent.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setContentFilter(isChecked)
        }
        
        // 标签过滤器
        binding.filterTags.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTagsFilter(isChecked)
        }
    }
    
    private fun setupRecyclerView() {
        searchAdapter = NotesAdapter(
            onNoteClick = { note ->
                findNavController().navigate(
                    SearchFragmentDirections.actionSearchFragmentToEditorFragment(noteId = note.id)
                )
            },
            onNoteLongClick = { note ->
                showNoteOptionsBottomSheet(note)
            }
        )
        
        binding.searchResultsRecyclerView.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = NoteItemAnimator()
        }
    }
    
    private fun observeViewModel() {
        // 观察搜索结果
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                searchAdapter.submitList(results)
                updateEmptyState(results.isEmpty())
            }
        }
        
        // 观察加载状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingAnimation.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptySearchView.root.visibility = if (isEmpty && viewModel.hasActiveQuery()) View.VISIBLE else View.GONE
        binding.searchResultsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun showNoteOptionsBottomSheet(note: Note) {
        val bottomSheet = NoteOptionsBottomSheet.newInstance(note.id)
        bottomSheet.show(childFragmentManager, "NoteOptionsBottomSheet")
    }
    
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }
    
    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

#### 搜索ViewModel实现:

```kotlin
// ui/screen/search/SearchViewModel.kt
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    
    // 过滤器状态
    private val _titleFilter = MutableStateFlow(true)
    private val _contentFilter = MutableStateFlow(true)
    private val _tagsFilter = MutableStateFlow(false)
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // 搜索结果
    val searchResults = combine(
        _searchQuery,
        _titleFilter,
        _contentFilter,
        _tagsFilter
    ) { query, titleFilter, contentFilter, tagsFilter ->
        // 返回过滤器和查询组合的四元组
        FilterCriteria(query, titleFilter, contentFilter, tagsFilter)
    }
    .flatMapLatest { criteria ->
        // 当查询为空且没有过滤器变化时，返回空列表
        if (criteria.query.isBlank()) {
            return@flatMapLatest flowOf(emptyList())
        }
        
        _isLoading.value = true
        
        // 执行搜索
        val results = noteRepository.searchNotes(
            query = criteria.query,
            searchInTitle = criteria.titleFilter,
            searchInContent = criteria.contentFilter,
            searchInTags = criteria.tagsFilter
        )
        
        _isLoading.value = false
        results
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    
    // 设置搜索查询
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    // 设置标题过滤器
    fun setTitleFilter(enabled: Boolean) {
        _titleFilter.value = enabled
    }
    
    // 设置内容过滤器
    fun setContentFilter(enabled: Boolean) {
        _contentFilter.value = enabled
    }
    
    // 设置标签过滤器
    fun setTagsFilter(enabled: Boolean) {
        _tagsFilter.value = enabled
    }
    
    // 检查是否有活动的搜索查询
    fun hasActiveQuery(): Boolean {
        return _searchQuery.value.isNotBlank()
    }
    
    // 过滤条件数据类
    data class FilterCriteria(
        val query: String,
        val titleFilter: Boolean,
        val contentFilter: Boolean,
        val tagsFilter: Boolean
    )
}
```

#### 搜索高亮适配器:

```kotlin
// ui/component/search/SearchResultAdapter.kt
class SearchResultAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : ListAdapter<NoteWithHighlights, SearchResultViewHolder>(SearchResultDiffCallback()) {

    private var searchQuery: String = ""
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val noteWithHighlights = getItem(position)
        holder.bind(noteWithHighlights, searchQuery, onNoteClick, onNoteLongClick)
    }
    
    fun setSearchQuery(query: String) {
        this.searchQuery = query
        notifyDataSetChanged() // 需要重新绑定所有项以应用新的高亮
    }
}

class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val noteCard: MaterialCardView = itemView.findViewById(R.id.note_card)
    private val titleText: TextView = itemView.findViewById(R.id.note_title)
    private val contentText: TextView = itemView.findViewById(R.id.note_content)
    private val dateText: TextView = itemView.findViewById(R.id.note_date)
    
    fun bind(
        noteWithHighlights: NoteWithHighlights,
        searchQuery: String,
        onNoteClick: (Note) -> Unit,
        onNoteLongClick: (Note) -> Unit
    ) {
        val note = noteWithHighlights.note
        
        // 设置卡片背景颜色
        note.color?.let { color ->
            noteCard.setCardBackgroundColor(color)
        }
        
        // 设置标题（带高亮）
        titleText.text = if (noteWithHighlights.highlightedTitle != null) {
            noteWithHighlights.highlightedTitle
        } else {
            highlightText(note.title, searchQuery)
        }
        
        // 设置内容（带高亮）
        contentText.text = if (noteWithHighlights.highlightedContent != null) {
            noteWithHighlights.highlightedContent
        } else {
            highlightText(note.content, searchQuery)
        }
        
        // 设置日期
        dateText.text = formatDateTime(note.modifiedAt)
        
        // 设置点击事件
        noteCard.setOnClickListener { onNoteClick(note) }
        noteCard.setOnLongClickListener { 
            onNoteLongClick(note)
            true
        }
    }
    
    private fun highlightText(text: String, query: String): SpannableString {
        if (query.isBlank()) return SpannableString(text)
        
        val spannableString = SpannableString(text)
        val pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        
        while (matcher.find()) {
            spannableString.setSpan(
                BackgroundColorSpan(Color.YELLOW),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(Color.BLACK),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        return spannableString
    }
    
    private fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

// 搜索结果差异比较
class SearchResultDiffCallback : DiffUtil.ItemCallback<NoteWithHighlights>() {
    override fun areItemsTheSame(oldItem: NoteWithHighlights, newItem: NoteWithHighlights): Boolean {
        return oldItem.note.id == newItem.note.id
    }
    
    override fun areContentsTheSame(oldItem: NoteWithHighlights, newItem: NoteWithHighlights): Boolean {
        return oldItem == newItem
    }
}

// 带高亮文本的笔记数据类
data class NoteWithHighlights(
    val note: Note,
    val highlightedTitle: SpannableString? = null,
    val highlightedContent: SpannableString? = null
)
```

### 2. 手势控制实现

添加手势控制，提升应用的交互体验：

```kotlin
// util/gesture/SwipeToActionCallback.kt
abstract class SwipeToActionCallback(
    private val context: Context,
    private val swipeDirection: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) : ItemTouchHelper.SimpleCallback(0, swipeDirection) {

    private val deleteColor = ContextCompat.getColor(context, R.color.delete_background)
    private val archiveColor = ContextCompat.getColor(context, R.color.archive_background)
    private val pinColor = ContextCompat.getColor(context, R.color.pin_background)
    
    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
    private val archiveIcon = ContextCompat.getDrawable(context, R.drawable.ic_archive)
    private val pinIcon = ContextCompat.getDrawable(context, R.drawable.ic_pin)
    
    private val iconMargin = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_margin)
    private val iconSize = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_size)
    
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false // 不支持上下拖动
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
        val itemHeight = itemView.height
        
        if (dX == 0f) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }
        
        // 设置背景颜色和图标
        val (backgroundColor, icon) = when {
            dX < 0 -> Pair(deleteColor, deleteIcon) // 左滑删除
            dX > 0 -> Pair(archiveColor, archiveIcon) // 右滑归档
            else -> Pair(Color.TRANSPARENT, null)
        }
        
        // 绘制背景
        val background = RectF(
            itemView.left.toFloat(),
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )
        val paint = Paint().apply { color = backgroundColor }
        c.drawRect(background, paint)
        
        // 绘制图标
        icon?.let {
            val iconTop = itemView.top + (itemHeight - iconSize) / 2
            val iconBottom = iconTop + iconSize
            
            if (dX < 0) { // 左滑
                val iconLeft = itemView.right - iconMargin - iconSize
                val iconRight = itemView.right - iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            } else { // 右滑
                val iconLeft = itemView.left + iconMargin
                val iconRight = itemView.left + iconMargin + iconSize
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            }
            
            icon.draw(c)
        }
        
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
    
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.4f // 需要滑动40%才触发操作
    }
}

// 在HomeFragment中使用
private fun setupSwipeActions() {
    val swipeHandler = object : SwipeToActionCallback(requireContext()) {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            val note = notesAdapter.getNoteAt(position)
            
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    // 左滑删除
                    viewModel.moveNoteToTrash(note)
                    showUndoSnackbar(note, R.string.note_moved_to_trash)
                }
                ItemTouchHelper.RIGHT -> {
                    // 右滑归档
                    viewModel.archiveNote(note)
                    showUndoSnackbar(note, R.string.note_archived)
                }
            }
        }
    }
    
    ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.notesRecyclerView)
}

private fun showUndoSnackbar(note: Note, messageResId: Int) {
    Snackbar.make(
        binding.root,
        messageResId,
        Snackbar.LENGTH_LONG
    ).setAction(R.string.undo) {
        when (messageResId) {
            R.string.note_moved_to_trash -> viewModel.restoreNoteFromTrash(note)
            R.string.note_archived -> viewModel.unarchiveNote(note)
        }
    }.show()
}
```

### 3. 拖放排序功能

实现笔记的拖放排序功能：

```kotlin
// util/gesture/DragToReorderCallback.kt
class DragToReorderCallback(
    private val adapter: NotesAdapter
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    0
) {
    
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition
        
        // 通知适配器项目被移动
        adapter.moveItem(fromPosition, toPosition)
        return true
    }
    
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // 不支持滑动操作
    }
    
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            viewHolder?.itemView?.alpha = 0.9f
            viewHolder?.itemView?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(100)?.start()
        }
    }
    
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        
        viewHolder.itemView.alpha = 1.0f
        viewHolder.itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        
        // 通知外部排序已完成
        adapter.onReorderFinished()
    }
}

// 在NotesAdapter中添加
fun moveItem(fromPosition: Int, toPosition: Int) {
    val currentList = currentList.toMutableList()
    val item = currentList.removeAt(fromPosition)
    currentList.add(toPosition, item)
    
    // 提交新的列表
    submitList(currentList)
}

private var onReorderListener: ((List<Note>) -> Unit)? = null

fun setOnReorderListener(listener: (List<Note>) -> Unit) {
    onReorderListener = listener
}

fun onReorderFinished() {
    onReorderListener?.invoke(currentList)
}

// 在HomeFragment中启用拖放排序
private fun setupDragToReorder() {
    notesAdapter.setOnReorderListener { reorderedList ->
        viewModel.updateNotesOrder(reorderedList)
    }
    
    val dragCallback = DragToReorderCallback(notesAdapter)
    val touchHelper = ItemTouchHelper(dragCallback)
    touchHelper.attachToRecyclerView(binding.notesRecyclerView)
}
```

### 4. 语音转文本功能

添加语音输入功能，使笔记录入更加便捷：

```kotlin
// ui/component/voice/VoiceRecorderView.kt
class VoiceRecorderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewVoiceRecorderBinding = ViewVoiceRecorderBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    
    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(context)
    }
    
    private var onTextRecognizedListener: ((String) -> Unit)? = null
    
    init {
        setupRecordButton()
        setupSpeechRecognizer()
    }
    
    private fun setupRecordButton() {
        binding.recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }
    
    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                binding.statusText.text = context.getString(R.string.listening)
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { recognizedText ->
                    onTextRecognizedListener?.invoke(recognizedText)
                    binding.statusText.text = context.getString(R.string.voice_recognition_complete)
                }
            }
            
            // 其他必要的回调方法实现...
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> R.string.error_audio
                    SpeechRecognizer.ERROR_CLIENT -> R.string.error_client
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.error_permission
                    SpeechRecognizer.ERROR_NETWORK -> R.string.error_network
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.error_timeout
                    SpeechRecognizer.ERROR_NO_MATCH -> R.string.error_no_match
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> R.string.error_busy
                    SpeechRecognizer.ERROR_SERVER -> R.string.error_server
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.error_speech_timeout
                    else -> R.string.error_unknown
                }
                
                binding.statusText.text = context.getString(errorMessage)
                stopRecording(showError = true)
            }
        })
    }
    
    private fun startRecording() {
        // 检查权限
        if (!hasAudioPermission()) {
            requestAudioPermission()
            return
        }
        
        try {
            // 准备录音文件
            audioFilePath = createAudioFilePath()
            