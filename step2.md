# 安卓记事本应用前端开发全面指南（续）

## 第三阶段：主要屏幕实现

现在我们已经完成了基础UI组件的开发，接下来将实现应用的主要屏幕。

### 1. 主页面（笔记列表/网格）

主页面是用户与应用交互的核心界面，需要实现笔记列表/网格视图切换、分类筛选和搜索功能。

#### 主页面布局（XML版本）:

```xml
<!-- res/layout/fragment_home.xml -->
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
        android:background="?attr/colorSurface"
        app:liftOnScroll="true">

        <androidx.appcompat.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:title="@string/app_name"
            app:titleTextAppearance="?attr/textAppearanceHeadlineSmall"
            app:menu="@menu/menu_home"
            app:layout_scrollFlags="scroll|enterAlways" />

        <com.google.android.material.tabs.TabLayout
            android:id="@+id/tab_layout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="?attr/colorSurface"
            app:tabMode="scrollable"
            app:tabGravity="start" />

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipe_refresh"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/notes_recycler_view"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:clipToPadding="false"
                android:padding="8dp"
                tools:listitem="@layout/item_note" />

            <include
                android:id="@+id/empty_view"
                layout="@layout/layout_empty_state"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:visibility="gone" />

        </FrameLayout>

    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab_add_note"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:contentDescription="@string/create_new_note"
        app:srcCompat="@drawable/ic_add" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### 主页面实现（Kotlin部分）:

```kotlin
// ui/screen/home/HomeFragment.kt
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        setupCategoryTabs()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
                    true
                }
                R.id.action_view_mode -> {
                    toggleViewMode()
                    true
                }
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onNoteClick = { note ->
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToEditorFragment(noteId = note.id)
                )
            },
            onNoteLongClick = { note ->
                showNoteOptionsBottomSheet(note)
            }
        )
        
        binding.notesRecyclerView.apply {
            adapter = notesAdapter
            layoutManager = viewModel.getLayoutManager(requireContext())
            itemAnimator = DefaultItemAnimator()
            
            // 添加滑动操作
            val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    val note = notesAdapter.getNoteAt(position)
                    viewModel.moveNoteToTrash(note)
                    
                    Snackbar.make(
                        binding.root,
                        R.string.note_moved_to_trash,
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.undo) {
                        viewModel.restoreNoteFromTrash(note)
                    }.show()
                }
            }
            
            ItemTouchHelper(swipeHandler).attachToRecyclerView(this)
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshNotes()
        }
    }
    
    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToEditorFragment()
            )
        }
    }
    
    private fun setupCategoryTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val categoryId = tab.tag as? String
                viewModel.setSelectedCategory(categoryId)
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun observeViewModel() {
        // 观察笔记列表
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                notesAdapter.submitList(notes)
                updateEmptyState(notes.isEmpty())
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        // 观察分类列表
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                updateCategoryTabs(categories)
            }
        }
        
        // 观察视图模式
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewMode.collect { viewMode ->
                updateViewModeIcon(viewMode)
                binding.notesRecyclerView.layoutManager = viewModel.getLayoutManager(requireContext())
                notesAdapter.setViewMode(viewMode)
            }
        }
    }
    
    private fun updateCategoryTabs(categories: List<Category>) {
        binding.tabLayout.removeAllTabs()
        
        // 添加"全部"选项卡
        binding.tabLayout.addTab(
            binding.tabLayout.newTab()
                .setText(R.string.all_notes)
                .setTag(null)
        )
        
        // 添加分类选项卡
        categories.forEach { category ->
            binding.tabLayout.addTab(
                binding.tabLayout.newTab()
                    .setText(category.name)
                    .setTag(category.id)
                    .setIcon(R.drawable.ic_folder) // 可选的图标
            )
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.notesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun toggleViewMode() {
        viewModel.toggleViewMode()
    }
    
    private fun updateViewModeIcon(viewMode: ViewMode) {
        val menuItem = binding.toolbar.menu.findItem(R.id.action_view_mode)
        menuItem.setIcon(
            if (viewMode == ViewMode.GRID) 
                R.drawable.ic_view_list 
            else 
                R.drawable.ic_view_grid
        )
    }
    
    private fun showNoteOptionsBottomSheet(note: Note) {
        val bottomSheet = NoteOptionsBottomSheet.newInstance(note.id)
        bottomSheet.show(childFragmentManager, "NoteOptionsBottomSheet")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

#### 主页面 ViewModel:

```kotlin
// ui/screen/home/HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // 当前选中的分类
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    
    // 视图模式（列表/网格）
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode
    
    // 获取所有分类
    val categories = categoryRepository.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    
    // 根据分类获取笔记
    val notes = _selectedCategoryId
        .flatMapLatest { categoryId ->
            if (categoryId == null) {
                // 如果没有选中分类，获取所有活动笔记
                noteRepository.getActiveNotes()
            } else {
                // 获取特定分类的笔记
                noteRepository.getNotesByCategory(categoryId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    
    init {
        // 从偏好设置中加载视图模式
        viewModelScope.launch {
            preferencesRepository.getViewMode().collect { savedViewMode ->
                _viewMode.value = savedViewMode
            }
        }
    }
    
    fun setSelectedCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }
    
    fun toggleViewMode() {
        val newViewMode = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        _viewMode.value = newViewMode
        
        // 保存新的视图模式到偏好设置
        viewModelScope.launch {
            preferencesRepository.saveViewMode(newViewMode)
        }
    }
    
    fun getLayoutManager(context: Context): RecyclerView.LayoutManager {
        return if (_viewMode.value == ViewMode.GRID) {
            GridLayoutManager(context, 2)
        } else {
            LinearLayoutManager(context)
        }
    }
    
    fun refreshNotes() {
        viewModelScope.launch {
            noteRepository.refreshNotes()
        }
    }
    
    fun moveNoteToTrash(note: Note) {
        viewModelScope.launch {
            noteRepository.moveToTrash(note.id)
        }
    }
    
    fun restoreNoteFromTrash(note: Note) {
        viewModelScope.launch {
            noteRepository.restoreFromTrash(note.id)
        }
    }
}

enum class ViewMode {
    LIST, GRID
}
```

#### 主页面（Compose版本）:

```kotlin
// ui/screen/home/HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNoteClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    
    val refreshState = rememberPullRefreshState(
        refreshing = viewModel.isRefreshing,
        onRefresh = viewModel::refreshNotes
    )
    
    Scaffold(
        topBar = {
            HomeAppBar(
                onSearchClick = onSearchClick,
                onViewModeToggle = viewModel::toggleViewMode,
                onSettingsClick = onSettingsClick,
                viewMode = viewMode
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNoteClick("") }, // 空ID表示创建新笔记
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Note"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 分类选项卡
            CategoryTabs(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = viewModel::setSelectedCategory
            )
            
            // 笔记列表/网格
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(refreshState)
            ) {
                if (notes.isEmpty()) {
                    EmptyState(
                        message = "No notes found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    when (viewMode) {
                        ViewMode.LIST -> NotesList(
                            notes = notes,
                            onNoteClick = { note -> onNoteClick(note.id) },
                            onNoteLongClick = { note -> viewModel.showNoteOptions(note) }
                        )
                        ViewMode.GRID -> NotesGrid(
                            notes = notes,
                            onNoteClick = { note -> onNoteClick(note.id) },
                            onNoteLongClick = { note -> viewModel.showNoteOptions(note) }
                        )
                    }
                }
                
                PullRefreshIndicator(
                    refreshing = viewModel.isRefreshing,
                    state = refreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun HomeAppBar(
    onSearchClick: () -> Unit,
    onViewModeToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    viewMode: ViewMode
) {
    TopAppBar(
        title = {
            Text(
                text = "NotePro",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
            
            IconButton(onClick = onViewModeToggle) {
                Icon(
                    imageVector = if (viewMode == ViewMode.GRID) 
                        Icons.Default.ViewList 
                    else 
                        Icons.Default.ViewModule,
                    contentDescription = "Toggle View Mode"
                )
            }
            
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun CategoryTabs(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = categories.indexOfFirst { it.id == selectedCategoryId }.let { 
            if (it == -1) 0 else it + 1 
        },
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        // "全部"选项卡
        Tab(
            selected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) },
            text = { Text("All Notes") }
        )
        
        // 分类选项卡
        categories.forEach { category ->
            Tab(
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) },
                text = { Text(category.name) },
                icon = { 
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
fun NotesList(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteLongClick: (Note) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = notes,
            key = { it.id }
        ) { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note) },
                onLongClick = { onNoteLongClick(note) }
            )
        }
    }
}

@Composable
fun NotesGrid(
    notes: List<Note>,
    onNoteClick: (Note) -> Unit,
    onNoteLongClick: (Note) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = notes,
            key = { it.id }
        ) { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note) },
                onLongClick = { onNoteLongClick(note) }
            )
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NoteAdd,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
```

### 2. 笔记编辑界面

编辑界面是用户创建和修改笔记的核心场所，需要实现富文本编辑、分类标签选择和颜色定制功能。

#### 编辑页面布局（XML版本）:

```xml
<!-- res/layout/fragment_editor.xml -->
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
            app:title="@string/edit_note"
            app:menu="@menu/menu_editor" />

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/title_layout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="@string/note_title"
                app:hintEnabled="true"
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/title_edit_text"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="textCapSentences"
                    android:maxLines="1" />

            </com.google.android.material.textfield.TextInputLayout>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/note_color"
                android:textAppearance="?attr/textAppearanceBodyMedium"
                android:layout_marginTop="16dp"
                android:layout_marginBottom="8dp" />

            <com.example.notepro.ui.component.common.ColorPickerView
                android:id="@+id/color_picker"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="16dp" />

            <com.google.android.material.textfield.TextInputLayout
                android:id="@+id/category_layout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="@string/note_category"
                style="@style/Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu">

                <AutoCompleteTextView
                    android:id="@+id/category_dropdown"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="none" />

            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.chip.ChipGroup
                android:id="@+id/tags_chip_group"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                app:chipSpacingHorizontal="8dp">

                <com.google.android.material.chip.Chip
                    android:id="@+id/add_tag_chip"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/add_tag"
                    app:chipIcon="@drawable/ic_add"
                    style="@style/Widget.Material3.Chip.Input" />

            </com.google.android.material.chip.ChipGroup>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/note_content"
                android:textAppearance="?attr/textAppearanceBodyMedium"
                android:layout_marginTop="16dp"
                android:layout_marginBottom="8dp" />

            <com.example.notepro.ui.component.editor.MarkdownEditorView
                android:id="@+id/markdown_editor"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="200dp" />

        </LinearLayout>

    </androidx.core.widget.NestedScrollView>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab_save"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:contentDescription="@string/save_note"
        app:srcCompat="@drawable/ic_save" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### 编辑页面实现（Kotlin部分）:

```kotlin
// ui/screen/editor/EditorFragment.kt
class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EditorViewModel by viewModels()
    private lateinit var categoryAdapter: ArrayAdapter<Category>
    
    private val args: EditorFragmentArgs by navArgs()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupColorPicker()
        setupCategoryDropdown()
        setupTagsChipGroup()
        setupSaveButton()
        
        // 如果是编辑现有笔记，加载笔记数据
        args.noteId?.let { noteId ->
            viewModel.loadNote(noteId)
        }
        
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share -> {
                    shareNote()
                    true
                }
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupColorPicker() {
        binding.colorPicker.setOnColorSelectedListener { color ->
            viewModel.setNoteColor(color)
        }
    }
    
    private fun setupCategoryDropdown() {
        categoryAdapter = object : ArrayAdapter<Category>(
            requireContext(),
            R.layout.item_dropdown,
            mutableListOf()
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position)?.name
                return view
            }
            
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position)?.name
                return view
            }
        }
        
        binding.categoryDropdown.setAdapter(categoryAdapter)
        
        binding.categoryDropdown.setOnItemClickListener { _, _, position, _ ->
            val category = categoryAdapter.getItem(position)
            category?.let {
                viewModel.setNoteCategory(it)
            }
        }
    }
    
    private fun setupTagsChipGroup() {
        binding.addTagChip.setOnClickListener {
            showAddTagDialog()
        }
    }
    
    private fun setupSaveButton() {
        binding.fabSave.setOnClickListener {
            saveNote()
        }
    }
    
    private fun observeViewModel() {
        // 观察当前笔记
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentNote.collect { note ->
                note?.let { updateUiWithNote(it) }
            }
        }
        
        // 观察分类列表
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                categoryAdapter.clear()
                categoryAdapter.addAll(categories)
            }
        }
        
        // 观察标签列表
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedTags.collect { tags ->
                updateTagsChips(tags)
            }
        }
        
        // 观察保存状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                when (state) {
                    is SaveState.Success ->