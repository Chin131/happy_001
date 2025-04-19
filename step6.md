# 安卓记事本应用前端开发全面指南（续）

继续实现语音转文本功能：

```kotlin
    private fun startRecording() {
        // 检查权限
        if (!hasAudioPermission()) {
            requestAudioPermission()
            return
        }
        
        try {
            // 准备录音文件
            audioFilePath = createAudioFilePath()
            
            // 初始化MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            
            // 更新UI状态
            isRecording = true
            binding.recordButton.setImageResource(R.drawable.ic_stop)
            binding.statusText.text = context.getString(R.string.recording)
            binding.waveformView.visibility = View.VISIBLE
            binding.waveformView.startAnimation()
            
            // 启动语音识别
            startSpeechRecognition()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.statusText.text = context.getString(R.string.error_recording)
        }
    }
    
    private fun stopRecording(showError: Boolean = false) {
        try {
            // 停止录音
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            // 停止语音识别
            speechRecognizer.stopListening()
            
            // 更新UI状态
            isRecording = false
            binding.recordButton.setImageResource(R.drawable.ic_mic)
            if (!showError) {
                binding.statusText.text = context.getString(R.string.processing)
            }
            binding.waveformView.stopAnimation()
            binding.waveformView.visibility = View.INVISIBLE
            
            // 如果不是因为错误停止，则执行转换
            if (!showError && audioFilePath != null) {
                // 转换录音为文本
                convertAudioToText(audioFilePath!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.statusText.text = context.getString(R.string.error_stopping)
        }
    }
    
    private fun startSpeechRecognition() {
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            speechRecognizer.startListening(recognizerIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.statusText.text = context.getString(R.string.error_recognition)
        }
    }
    
    private fun convertAudioToText(audioFilePath: String) {
        // 在实际应用中，可能需要使用更高级的语音识别服务或API
        // 这里我们假设直接使用本地转换结果
        binding.statusText.text = context.getString(R.string.voice_recognition_complete)
    }
    
    private fun createAudioFilePath(): String {
        val audioDir = File(context.filesDir, "audio")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AUDIO_$timestamp.3gp"
        return File(audioDir, fileName).absolutePath
    }
    
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestAudioPermission() {
        // 在Fragment或Activity中请求权限
        binding.statusText.text = context.getString(R.string.permission_required)
    }
    
    fun setOnTextRecognizedListener(listener: (String) -> Unit) {
        onTextRecognizedListener = listener
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        // 清理资源
        mediaRecorder?.release()
        mediaRecorder = null
        
        speechRecognizer.destroy()
    }
}
```

在编辑页面中集成语音输入功能：

```kotlin
// ui/screen/editor/EditorFragment.kt

// 在setupViews方法中添加语音输入按钮
private fun setupViews() {
    // ... 其他视图设置代码
    
    // 设置语音输入按钮
    binding.voiceInputButton.setOnClickListener {
        showVoiceInputDialog()
    }
}

private fun showVoiceInputDialog() {
    val dialogView = LayoutInflater.from(requireContext())
        .inflate(R.layout.dialog_voice_input, null)
    
    val voiceRecorderView = dialogView.findViewById<VoiceRecorderView>(R.id.voice_recorder)
    
    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.voice_input)
        .setView(dialogView)
        .setPositiveButton(R.string.done, null)
        .setNegativeButton(R.string.cancel, null)
        .create()
    
    // 设置语音识别结果监听器
    voiceRecorderView.setOnTextRecognizedListener { recognizedText ->
        // 将识别的文本插入到当前光标位置
        val currentContent = binding.markdownEditor.getMarkdown()
        val cursorPosition = binding.markdownEditor.getSelectionStart()
        
        val newContent = StringBuilder(currentContent)
            .insert(cursorPosition, recognizedText)
            .toString()
        
        binding.markdownEditor.setMarkdown(newContent)
        
        // 关闭对话框
        dialog.dismiss()
    }
    
    dialog.show()
}
```

### 5. 分类与标签管理

下面实现分类与标签管理功能：

#### 分类管理页面布局:

```xml
<!-- res/layout/fragment_categories.xml -->
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
            app:title="@string/categories_and_tags"
            app:menu="@menu/menu_categories" />

        <com.google.android.material.tabs.TabLayout
            android:id="@+id/tab_layout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:tabGravity="fill"
            app:tabMode="fixed">

            <com.google.android.material.tabs.TabItem
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/categories" />

            <com.google.android.material.tabs.TabItem
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/tags" />

        </com.google.android.material.tabs.TabLayout>

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/view_pager"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior" />

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab_add"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:contentDescription="@string/add_category_or_tag"
        app:srcCompat="@drawable/ic_add" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### 分类列表页面布局:

```xml
<!-- res/layout/fragment_category_list.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/category_recycler_view"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:clipToPadding="false"
        android:padding="8dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        tools:listitem="@layout/item_category" />

    <include
        android:id="@+id/empty_view"
        layout="@layout/layout_empty_state"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

#### 分类管理Fragment实现:

```kotlin
// ui/screen/category/CategoriesFragment.kt
@AndroidEntryPoint
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewPagerAdapter: CategoryPagerAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupViewPager()
        setupFab()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_sort -> {
                    showSortDialog()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupViewPager() {
        viewPagerAdapter = CategoryPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        
        // 连接TabLayout与ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.categories)
                1 -> getString(R.string.tags)
                else -> null
            }
        }.attach()
    }
    
    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            when (binding.viewPager.currentItem) {
                0 -> showAddCategoryDialog()
                1 -> showAddTagDialog()
            }
        }
    }
    
    private fun showSortDialog() {
        val sortOptions = arrayOf(
            getString(R.string.sort_by_name),
            getString(R.string.sort_by_notes_count),
            getString(R.string.sort_by_date_created)
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_by)
            .setItems(sortOptions) { _, which ->
                val sortType = when (which) {
                    0 -> SortType.NAME
                    1 -> SortType.NOTES_COUNT
                    2 -> SortType.DATE_CREATED
                    else -> SortType.NAME
                }
                
                // 通知当前显示的Fragment更新排序
                val currentFragment = viewPagerAdapter.getFragment(binding.viewPager.currentItem)
                if (currentFragment is Sortable) {
                    currentFragment.applySorting(sortType)
                }
            }
            .show()
    }
    
    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_category, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.category_name_input)
        val colorPicker = dialogView.findViewById<ColorPickerView>(R.id.color_picker)
        
        var selectedColor: Int = ColorPickerView.DEFAULT_COLORS.first()
        colorPicker.setOnColorSelectedListener { color ->
            selectedColor = color
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_category)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    // 获取当前CategoryListFragment实例
                    val fragment = viewPagerAdapter.getFragment(0)
                    if (fragment is CategoryListFragment) {
                        fragment.addCategory(name, selectedColor)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showAddTagDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_tag, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.tag_name_input)
        val colorPicker = dialogView.findViewById<ColorPickerView>(R.id.color_picker)
        
        var selectedColor: Int = ColorPickerView.DEFAULT_COLORS.first()
        colorPicker.setOnColorSelectedListener { color ->
            selectedColor = color
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_tag)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    // 获取当前TagListFragment实例
                    val fragment = viewPagerAdapter.getFragment(1)
                    if (fragment is TagListFragment) {
                        fragment.addTag(name, selectedColor)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// 分页适配器
class CategoryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    
    private val fragmentsMap = mutableMapOf<Int, Fragment>()
    
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CategoryListFragment().also { fragmentsMap[position] = it }
            1 -> TagListFragment().also { fragmentsMap[position] = it }
            else -> throw IndexOutOfBoundsException("Invalid position: $position")
        }
    }
    
    fun getFragment(position: Int): Fragment? {
        return fragmentsMap[position]
    }
}

// 排序接口
interface Sortable {
    fun applySorting(sortType: SortType)
}

enum class SortType {
    NAME, NOTES_COUNT, DATE_CREATED
}
```

#### 分类列表Fragment实现:

```kotlin
// ui/screen/category/CategoryListFragment.kt
@AndroidEntryPoint
class CategoryListFragment : Fragment(), Sortable {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onCategoryClick = { category ->
                navigateToCategoryNotes(category)
            },
            onCategoryLongClick = { category ->
                showCategoryOptionsDialog(category)
            }
        )
        
        binding.categoryRecyclerView.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = DefaultItemAnimator()
            
            // 添加分割线
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                categoryAdapter.submitList(categories)
                updateEmptyState(categories.isEmpty())
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.categoryRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        
        if (isEmpty) {
            binding.emptyView.messageText.text = getString(R.string.no_categories)
            binding.emptyView.lottieAnimation.setAnimation(R.raw.empty_folder)
            binding.emptyView.lottieAnimation.playAnimation()
        }
    }
    
    private fun navigateToCategoryNotes(category: Category) {
        findNavController().navigate(
            CategoriesFragmentDirections.actionCategoriesFragmentToHomeFragment(categoryId = category.id)
        )
    }
    
    private fun showCategoryOptionsDialog(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(category.name)
            .setItems(
                arrayOf(
                    getString(R.string.edit),
                    getString(R.string.delete)
                )
            ) { _, which ->
                when (which) {
                    0 -> showEditCategoryDialog(category)
                    1 -> confirmDeleteCategory(category)
                }
            }
            .show()
    }
    
    private fun showEditCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_category, null)
        
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.category_name_input)
        val colorPicker = dialogView.findViewById<ColorPickerView>(R.id.color_picker)
        
        // 预填充现有分类信息
        nameInput.setText(category.name)
        category.color?.let { colorPicker.setSelectedColor(it) }
        
        var selectedColor = category.color
        colorPicker.setOnColorSelectedListener { color ->
            selectedColor = color
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_category)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    viewModel.updateCategory(category.copy(name = name, color = selectedColor))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun confirmDeleteCategory(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_category)
            .setMessage(getString(R.string.delete_category_confirmation, category.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    fun addCategory(name: String, color: Int) {
        viewModel.addCategory(name, color)
    }
    
    override fun applySorting(sortType: SortType) {
        viewModel.setSortType(sortType)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

#### 分类ViewModel实现:

```kotlin
// ui/screen/category/CategoryViewModel.kt
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // 当前排序类型
    private val _sortType = MutableStateFlow(SortType.NAME)
    
    // 按照排序类型获取分类列表
    val categories = _sortType.flatMapLatest { sortType ->
        when (sortType) {
            SortType.NAME -> categoryRepository.getAllCategoriesByName()
            SortType.NOTES_COUNT -> categoryRepository.getAllCategoriesByNotesCount()
            SortType.DATE_CREATED -> categoryRepository.getAllCategoriesByDateCreated()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    
    init {
        // 从偏好设置中加载排序类型
        viewModelScope.launch {
            preferencesRepository.getCategorySortType().collect { savedSortType ->
                _sortType.value = savedSortType
            }
        }
    }
    
    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
        
        // 保存排序类型到偏好设置
        viewModelScope.launch {
            preferencesRepository.saveCategorySortType(sortType)
        }
    }
    
    fun addCategory(name: String, color: Int) {
        viewModelScope.launch {
            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                name = name,
                color = color,
                createdAt = System.currentTimeMillis()
            )
            categoryRepository.insertCategory(newCategory)
        }
    }
    
    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }
    
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category.id)
        }
    }
}
```

## 第八阶段：集成组件和导航实现

现在我们已经实现了应用的所有主要功能，接下来将把它们集成到一个完整的应用中，并实现导航流程。

### 1. 导航图实现

使用Navigation组件创建应用的导航图：

```xml
<!-- res/navigation/nav_graph.xml -->
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/nav_graph"
    app:startDestination="@id/homeFragment">

    <fragment
        android:id="@+id/homeFragment"
        android:name="com.example.notepro.ui.screen.home.HomeFragment"
        android:label="@string/home"
        tools:layout="@layout/fragment_home">
        <action
            android:id="@+id/action_homeFragment_to_editorFragment"
            app:destination="@id/editorFragment" />
        <action
            android:id="@+id/action_homeFragment_to_searchFragment"
            app:destination="@id/searchFragment" />
        <action
            android:id="@+id/action_homeFragment_to_settingsFragment"
            app:destination="@id/settingsFragment" />
        <action
            android:id="@+id/action_homeFragment_to_categoriesFragment"
            app:destination="@id/categoriesFragment" />
        <argument
            android:name="categoryId"
            app:argType="string"
            app:nullable="true"
            android:defaultValue="@null" />
    </fragment>

    <fragment
        android:id="@+id/editorFragment"
        android:name="com.example.notepro.ui.screen.editor.EditorFragment"
        android:label="@string/editor"
        tools:layout="@layout/fragment_editor">
        <argument
            android:name="noteId"
            app:argType="string"
            app:nullable="true"
            android:defaultValue="@null" />
    </fragment>

    <fragment
        android:id="@+id/searchFragment"
        android:name="com.example.notepro.ui.screen.search.SearchFragment"
        android:label="@string/search"
        tools:layout="@layout/fragment_search">
        <action
            android:id="@+id/action_searchFragment_to_editorFragment"
            app:destination="@id/editorFragment" />
    </fragment>

    <fragment
        android:id="@+id/settingsFragment"
        android:name="com.example.notepro.ui.screen.settings.SettingsFragment"
        android:label="@string/settings"
        tools:layout="@layout/fragment_settings" />

    <fragment
        android:id="@+id/categoriesFragment"
        android:name="com.example.notepro.ui.screen.category.CategoriesFragment"
        android:label="@string/categories_and_tags"
        tools:layout="@layout/fragment_categories">
        <action
            android:id="@+id/action_categoriesFragment_to_homeFragment"
            app:destination="@id/homeFragment" />
    </fragment>

    <fragment
        android:id="@+id/archiveFragment"
        android:name="com.example.notepro.ui.screen.archive.ArchiveFragment"
        android:label="@string/archive"
        tools:layout="@layout/fragment_archive">
        <action
            android:id="@+id/action_archiveFragment_to_editorFragment"
            app:destination="@id/editorFragment" />
    </fragment>

    <fragment
        android:id="@+id/trashFragment"
        android:name="com.example.notepro.ui.screen.trash.TrashFragment"
        android:label="@string/trash"
        tools:layout="@layout/fragment_trash">
        <action
            android:id="@+id/action_trashFragment_to_editorFragment"
            app:destination="@id/editorFragment" />
    </fragment>
</navigation>
```

### 2. 主活动实现

创建应用的主活动，它将作为导航的容器并包含底部导航栏：

```xml
<!-- res/layout/activity_main.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_navigation"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        app:menu="@menu/menu_bottom_navigation" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    private lateinit var navController: NavController
    
    // 应用锁定功能的处理
    private var isAppLocked = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 在设置内容视图之前应用主题
        themeManager.applyDynamicColorIfAvailable(this)
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置导航控制器
        navController = findNavController(R.id.nav_host_fragment)
        
        // 设置底部导航
        setupBottomNavigation()
        
        // 检查应用锁定状态
        checkAppLockStatus()
    }