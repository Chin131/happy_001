# 安卓记事本应用前端开发全面指南（续）

继续主活动的实现：

```kotlin
    private fun setupBottomNavigation() {
        binding.bottomNavigation.apply {
            // 设置导航项选择监听器
            setOnItemSelectedListener { item ->
                navigateToDestination(item.itemId)
                true
            }
            
            // 设置重选监听器，用于回到顶部或刷新
            setOnItemReselectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home, R.id.nav_archive, R.id.nav_trash -> {
                        // 找到当前可见的Fragment并调用其scrollToTop方法
                        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                        val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                        
                        if (currentFragment is Scrollable) {
                            currentFragment.scrollToTop()
                        }
                    }
                }
            }
        }
        
        // 监听导航变化，更新底部导航栏选中状态
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // 根据目的地显示或隐藏底部导航栏
            when (destination.id) {
                R.id.homeFragment, R.id.archiveFragment, R.id.trashFragment -> {
                    showBottomNavigation()
                    
                    // 更新选中的导航项
                    val menuItemId = when (destination.id) {
                        R.id.homeFragment -> R.id.nav_home
                        R.id.archiveFragment -> R.id.nav_archive
                        R.id.trashFragment -> R.id.nav_trash
                        else -> null
                    }
                    
                    menuItemId?.let { binding.bottomNavigation.selectedItemId = it }
                }
                else -> hideBottomNavigation()
            }
        }
    }
    
    private fun navigateToDestination(itemId: Int) {
        // 清除回退栈，避免导航项目之间的堆叠
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(navController.graph.startDestinationId, false)
            .build()
        
        // 根据选择的项导航到不同目的地
        when (itemId) {
            R.id.nav_home -> navController.navigate(R.id.homeFragment, null, navOptions)
            R.id.nav_archive -> navController.navigate(R.id.archiveFragment, null, navOptions)
            R.id.nav_trash -> navController.navigate(R.id.trashFragment, null, navOptions)
        }
    }
    
    private fun showBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.bottomNavigation.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }
    
    private fun hideBottomNavigation() {
        binding.bottomNavigation.animate()
            .translationY(binding.bottomNavigation.height.toFloat())
            .setDuration(300)
            .withEndAction {
                binding.bottomNavigation.visibility = View.GONE
            }
            .start()
    }
    
    private fun checkAppLockStatus() {
        val preferencesRepository = PreferencesRepository(this)
        
        // 检查应用是否需要锁定
        lifecycleScope.launch {
            preferencesRepository.getAppLockEnabled().collect { enabled ->
                isAppLocked = enabled
                
                if (enabled) {
                    // 显示应用锁定界面
                    showAppLockScreen()
                }
            }
        }
    }
    
    private fun showAppLockScreen() {
        // 创建锁屏对话框，要求用户输入密码
        val dialogView = layoutInflater.inflate(R.layout.dialog_app_lock, null)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.password_input)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_lock)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(R.string.unlock, null)
            .create()
        
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val password = passwordInput.text.toString()
                
                // 验证密码
                lifecycleScope.launch {
                    val preferencesRepository = PreferencesRepository(this@MainActivity)
                    val isCorrect = preferencesRepository.verifyAppLockPassword(password)
                    
                    if (isCorrect) {
                        dialog.dismiss()
                        isAppLocked = false
                    } else {
                        passwordInput.error = getString(R.string.incorrect_password)
                        passwordInput.setText("")
                    }
                }
            }
        }
        
        dialog.show()
    }
    
    override fun onResume() {
        super.onResume()
        
        // 当应用恢复前台状态时，检查是否需要显示锁屏
        if (isAppLocked) {
            showAppLockScreen()
        }
    }
    
    // 实现返回键处理
    override fun onBackPressed() {
        val currentDestination = navController.currentDestination?.id
        
        // 如果当前在主页、归档或回收站页面，且按下返回键，则显示退出确认对话框
        if (currentDestination == R.id.homeFragment || 
            currentDestination == R.id.archiveFragment || 
            currentDestination == R.id.trashFragment) {
            showExitConfirmation()
        } else {
            super.onBackPressed()
        }
    }
    
    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.exit_app)
            .setMessage(R.string.exit_confirmation)
            .setPositiveButton(R.string.exit) { _, _ ->
                finishAffinity()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

// 滚动到顶部接口
interface Scrollable {
    fun scrollToTop()
}
```

### 3. 归档页面实现

实现归档页面，用于查看和管理已归档的笔记：

```kotlin
// ui/screen/archive/ArchiveFragment.kt
@AndroidEntryPoint
class ArchiveFragment : Fragment(), Scrollable {

    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ArchiveViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onNoteClick = { note ->
                findNavController().navigate(
                    ArchiveFragmentDirections.actionArchiveFragmentToEditorFragment(noteId = note.id)
                )
            },
            onNoteLongClick = { note ->
                showNoteOptionsBottomSheet(note)
            }
        )
        
        binding.notesRecyclerView.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = NoteItemAnimator()
            
            // 添加滑动操作
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
                            // 右滑取消归档
                            viewModel.unarchiveNote(note)
                            showUndoSnackbar(note, R.string.note_unarchived)
                        }
                    }
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
    
    private fun observeViewModel() {
        // 观察归档笔记列表
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.archivedNotes.collect { notes ->
                notesAdapter.submitList(notes)
                updateEmptyState(notes.isEmpty())
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.notesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        
        if (isEmpty) {
            binding.emptyView.messageText.text = getString(R.string.no_archived_notes)
            binding.emptyView.lottieAnimation.setAnimation(R.raw.empty_archive)
            binding.emptyView.lottieAnimation.playAnimation()
        }
    }
    
    private fun showNoteOptionsBottomSheet(note: Note) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_archived_note_options, null)
        
        // 设置选项点击监听器
        view.findViewById<TextView>(R.id.option_unarchive).setOnClickListener {
            viewModel.unarchiveNote(note)
            bottomSheetDialog.dismiss()
        }
        
        view.findViewById<TextView>(R.id.option_delete).setOnClickListener {
            viewModel.moveNoteToTrash(note)
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }
    
    private fun showUndoSnackbar(note: Note, messageResId: Int) {
        Snackbar.make(
            binding.root,
            messageResId,
            Snackbar.LENGTH_LONG
        ).setAction(R.string.undo) {
            when (messageResId) {
                R.string.note_moved_to_trash -> viewModel.restoreNoteFromTrash(note)
                R.string.note_unarchived -> viewModel.archiveNote(note)
            }
        }.show()
    }
    
    override fun scrollToTop() {
        binding.notesRecyclerView.smoothScrollToPosition(0)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 4. 回收站页面实现

实现回收站页面，用于查看和管理已删除的笔记：

```kotlin
// ui/screen/trash/TrashFragment.kt
@AndroidEntryPoint
class TrashFragment : Fragment(), Scrollable {

    private var _binding: FragmentTrashBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TrashViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_empty_trash -> {
                    showEmptyTrashConfirmation()
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
                    TrashFragmentDirections.actionTrashFragmentToEditorFragment(noteId = note.id)
                )
            },
            onNoteLongClick = { note ->
                showNoteOptionsBottomSheet(note)
            }
        )
        
        binding.notesRecyclerView.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = NoteItemAnimator()
            
            // 添加滑动操作
            val swipeHandler = object : SwipeToActionCallback(requireContext()) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    val note = notesAdapter.getNoteAt(position)
                    
                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            // 左滑永久删除
                            viewModel.deleteNotePermanently(note)
                            showUndoSnackbar(note, R.string.note_deleted_permanently)
                        }
                        ItemTouchHelper.RIGHT -> {
                            // 右滑恢复
                            viewModel.restoreNoteFromTrash(note)
                            showUndoSnackbar(note, R.string.note_restored)
                        }
                    }
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
    
    private fun observeViewModel() {
        // 观察回收站笔记列表
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.trashedNotes.collect { notes ->
                notesAdapter.submitList(notes)
                updateEmptyState(notes.isEmpty())
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.notesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        
        if (isEmpty) {
            binding.emptyView.messageText.text = getString(R.string.trash_empty)
            binding.emptyView.lottieAnimation.setAnimation(R.raw.empty_trash)
            binding.emptyView.lottieAnimation.playAnimation()
        }
    }
    
    private fun showNoteOptionsBottomSheet(note: Note) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_trash_note_options, null)
        
        // 设置选项点击监听器
        view.findViewById<TextView>(R.id.option_restore).setOnClickListener {
            viewModel.restoreNoteFromTrash(note)
            bottomSheetDialog.dismiss()
        }
        
        view.findViewById<TextView>(R.id.option_delete_permanently).setOnClickListener {
            showDeletePermanentlyConfirmation(note)
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }
    
    private fun showDeletePermanentlyConfirmation(note: Note) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_permanently)
            .setMessage(R.string.delete_permanently_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteNotePermanently(note)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showEmptyTrashConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.empty_trash)
            .setMessage(R.string.empty_trash_confirmation)
            .setPositiveButton(R.string.empty) { _, _ ->
                viewModel.emptyTrash()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showUndoSnackbar(note: Note, messageResId: Int) {
        if (messageResId == R.string.note_deleted_permanently) {
            // 永久删除的笔记无法恢复
            Snackbar.make(
                binding.root,
                messageResId,
                Snackbar.LENGTH_SHORT
            ).show()
        } else {
            Snackbar.make(
                binding.root,
                messageResId,
                Snackbar.LENGTH_LONG
            ).setAction(R.string.undo) {
                when (messageResId) {
                    R.string.note_restored -> viewModel.moveNoteToTrash(note)
                }
            }.show()
        }
    }
    
    override fun scrollToTop() {
        binding.notesRecyclerView.smoothScrollToPosition(0)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

## 第九阶段：应用发布准备

在将应用发布到应用商店之前，需要完成以下工作：

### 1. 应用图标与启动屏幕

创建高质量的应用图标和启动屏幕：

```xml
<!-- res/drawable/ic_launcher_foreground.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108"
    android:tint="#FFFFFF">
    <group android:scaleX="2.61"
        android:scaleY="2.61"
        android:translateX="22.68"
        android:translateY="22.68">
        <path
            android:fillColor="@android:color/white"
            android:pathData="M18,2H6C4.9,2 4,2.9 4,4v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4C20,2.9 19.1,2 18,2zM9,4h2v5l-1,-0.75L9,9V4zM18,20H6V4h1v9l3,-2.25L13,13V4h5V20z"/>
    </group>
</vector>
```

创建启动屏幕：

```xml
<!-- res/values/splash.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.NotePro.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/primary</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
        <item name="windowSplashScreenAnimationDuration">1000</item>
        <item name="postSplashScreenTheme">@style/Theme.NotePro</item>
    </style>
</resources>
```

在MainActivity中应用启动屏幕：

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // ... 其他代码

    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用启动屏幕
        installSplashScreen()
        
        // 在设置内容视图之前应用主题
        themeManager.applyDynamicColorIfAvailable(this)
        
        super.onCreate(savedInstanceState)
        
        // ... 其他代码
    }
    
    // ... 其他代码
}
```

### 2. 应用本地化

创建多语言支持：

```xml
<!-- res/values/strings.xml (英文) -->
<resources>
    <string name="app_name">NotePro</string>
    <string name="home">Home</string>
    <string name="archive">Archive</string>
    <string name="trash">Trash</string>
    <!-- 其他字符串... -->
</resources>

<!-- res/values-zh/strings.xml (中文) -->
<resources>
    <string name="app_name">NotePro记事本</string>
    <string name="home">首页</string>
    <string name="archive">归档</string>
    <string name="trash">回收站</string>
    <!-- 其他字符串... -->
</resources>

<!-- res/values-ja/strings.xml (日文) -->
<resources>
    <string name="app_name">NotePro ノート</string>
    <string name="home">ホーム</string>
    <string name="archive">アーカイブ</string>
    <string name="trash">ゴミ箱</string>
    <!-- 其他字符串... -->
</resources>
```

### 3. 应用版本与发布配置

在build.gradle文件中配置版本信息：

```gradle
android {
    defaultConfig {
        applicationId "com.example.notepro"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
        
        // 设置支持的语言
        resConfigs "en", "zh", "ja"
    }
    
    signingConfigs {
        release {
            // 从环境变量或属性文件中获取签名信息
            storeFile file(RELEASE_STORE_FILE)
            storePassword RELEASE_STORE_PASSWORD
            keyAlias RELEASE_KEY_ALIAS
            keyPassword RELEASE_KEY_PASSWORD
        }
    }
    
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
        }
        
        debug {
            applicationIdSuffix ".debug"
            versionNameSuffix "-debug"
        }
    }
}
```

### 4. Play商店资产准备

准备Google Play商店所需的资产：

- 功能图片（Feature Graphic）：1024 x 500 px
- 应用图标：512 x 512 px
- 手机截图：至少3张，建议分辨率1080 x 1920 px
- 平板截图：如果支持平板，至少一张
- 简短描述：80个字符以内
- 完整描述：4000个字符以内
- 隐私政策URL
- 应用分类：生产力工具

### 5. 清单文件完善

完善AndroidManifest.xml文件：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.example.notepro">

    <!-- 权限声明 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    
    <application
        android:name=".NoteProApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.NotePro"
        android:localeConfig="@xml/locales_config"
        tools:targetApi="34">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.NotePro.Splash">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            
            <!-- 添加内容共享处理 -->
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
        </activity>
        
        <!-- 注册内容提供者 -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
        
        <!-- 应用备份 -->
        <meta-data
            android:name="com.google.android.backup.api_key"
            android:value="YOUR_BACKUP_API_KEY" />
    </application>
</manifest>
```

## 总结与最佳实践

通过本指南，我们已经全面实现了NotePro安卓记事本应用的前端部分。下面是一些关键的最佳实践总结：

### 1. 架构与设计
- **MVVM架构**：清晰分离UI、业务逻辑和数据访问
- **组件化设计**：独立、可复用的UI和功能组件
- **Material Design 3**：现代化界面风格，支持动态颜色

### 2. 性能优化
- **列表渲染优化**：使用DiffUtil和高效的ViewHolder
- **延迟加载**：仅加载用户当前可见的内容
- **资源优化**：图像压缩、代码混淆和资源压缩

### 3. 用户体验增强
- **流畅的动画**：共享元素转场、列表动画和微交互
- **手势支持**：滑动删除、归档和拖放排序
- **深色模式**：全面支持浅色/深色主题切换

### 4. 功能实现
- **富文本编辑**：支持格式化的笔记编辑体验
- **分类与标签**：灵活的笔记组织系统
- **搜索功能**：高效的全文搜索和结果高亮
- **同步与备份**：数据安全保障机制

### 5. 开发效率
- **模块化代码**：易于维护和扩展的代码结构
- **完整测试**：单元测试和UI测试保障质量
- **渐进式实现**：由简单到复杂的功能实现策略

### 发布清单
在发布前，确保完成以下检查：
1. **权限审核**：确保只请求必要的权限
2. **资源压缩**：减小APK大小
3. **性能测试**：在不同设备上进行测试
4. **安全审核**：检查敏感数据处理
5. **本地化审核**：确保所有字符串都已本地化
6. **可访问性检查**：支持辅助功能

通过这份全面的开发指南，前端开发团队可以系统地实现一个功能丰富、用户体验出色的专业记事本应用。这些实践和模式也适用于其他类型的安卓应用开发，可以作为一个通用的前端开发参考框架。