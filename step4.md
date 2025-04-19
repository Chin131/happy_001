# 安卓记事本应用前端开发全面指南（续）

## 第四阶段：动效设计与实现（续）

### 4. 微交互动画

添加精致的微交互动画，提升应用的整体质感和用户体验：

#### 按钮状态动画：

```kotlin
// ui/component/common/AnimatedButton.kt
class AnimatedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialButton(context, attrs, defStyleAttr) {

    private val pressedScale = 0.95f
    private val defaultScale = 1.0f
    
    init {
        // 设置按钮初始状态
        scaleX = defaultScale
        scaleY = defaultScale
        
        // 设置点击状态监听
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    animate()
                        .scaleX(pressedScale)
                        .scaleY(pressedScale)
                        .setDuration(100)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    animate()
                        .scaleX(defaultScale)
                        .scaleY(defaultScale)
                        .setDuration(200)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            }
            false
        }
    }
}
```

#### 输入框焦点动画：

```kotlin
// ui/component/common/AnimatedEditText.kt
class AnimatedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextInputEditText(context, attrs, defStyleAttr) {

    private val focusedElevation = 8f
    private val defaultElevation = 2f
    
    init {
        // 设置初始高度
        elevation = defaultElevation
        
        // 添加焦点监听器
        setOnFocusChangeListener { _, hasFocus ->
            val targetElevation = if (hasFocus) focusedElevation else defaultElevation
            
            ValueAnimator.ofFloat(elevation, targetElevation).apply {
                duration = 300
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { animator ->
                    elevation = animator.animatedValue as Float
                }
                start()
            }
        }
    }
}
```

#### 列表加载动画（Compose版本）：

```kotlin
// ui/component/common/LoadingAnimation.kt
@Composable
fun AnimatedItemsVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = fadeIn() + expandVertically(),
    exitTransition: ExitTransition = fadeOut() + shrinkVertically(),
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enterTransition,
        exit = exitTransition,
        content = content
    )
}

@Composable
fun <T> AnimatedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    itemContent: @Composable (T) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        itemsIndexed(items) { index, item ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = index * 50
                    )
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        delayMillis = index * 50
                    ),
                    initialOffsetY = { it / 2 }
                )
            ) {
                itemContent(item)
            }
        }
    }
}
```

#### 滑动刷新动画：

自定义下拉刷新动画，提升应用品质感：

```kotlin
// ui/component/common/NoteProRefreshView.kt
class NoteProRefreshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), SwipeRefreshLayout.OnRefreshListener {

    private val binding: ViewCustomRefreshBinding = ViewCustomRefreshBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    
    private val lottieView = binding.lottieAnimation
    private var onRefreshListener: (() -> Unit)? = null
    
    init {
        // 设置Lottie动画
        lottieView.setAnimation(R.raw.refresh_animation)
        lottieView.repeatCount = LottieDrawable.INFINITE
    }
    
    override fun onRefresh() {
        // 播放动画
        lottieView.playAnimation()
        
        // 通知刷新监听器
        onRefreshListener?.invoke()
    }
    
    fun setOnRefreshListener(listener: () -> Unit) {
        onRefreshListener = listener
    }
    
    fun setRefreshing(refreshing: Boolean) {
        if (refreshing) {
            visibility = View.VISIBLE
            lottieView.playAnimation()
        } else {
            lottieView.cancelAnimation()
            visibility = View.GONE
        }
    }
}
```

### 5. 无数据状态动画

创建吸引人的空状态动画，提高用户体验：

```kotlin
// ui/component/common/EmptyStateView.kt
class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewEmptyStateBinding = ViewEmptyStateBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    
    init {
        // 设置默认动画
        binding.lottieAnimation.setAnimation(R.raw.empty_animation)
    }
    
    fun setEmptyStateType(type: EmptyStateType) {
        val (animation, message) = when (type) {
            EmptyStateType.NOTES -> Pair(R.raw.empty_notes, R.string.empty_notes)
            EmptyStateType.SEARCH -> Pair(R.raw.empty_search, R.string.empty_search)
            EmptyStateType.CATEGORY -> Pair(R.raw.empty_folder, R.string.empty_category)
            EmptyStateType.TRASH -> Pair(R.raw.empty_trash, R.string.empty_trash)
        }
        
        binding.lottieAnimation.setAnimation(animation)
        binding.messageText.setText(message)
        binding.lottieAnimation.playAnimation()
    }
}

enum class EmptyStateType {
    NOTES, SEARCH, CATEGORY, TRASH
}
```

对应的Compose版本：

```kotlin
// ui/component/common/EmptyState.kt
@Composable
fun EmptyState(
    type: EmptyStateType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val (animationRes, messageRes) = when (type) {
        EmptyStateType.NOTES -> Pair(R.raw.empty_notes, R.string.empty_notes)
        EmptyStateType.SEARCH -> Pair(R.raw.empty_search, R.string.empty_search)
        EmptyStateType.CATEGORY -> Pair(R.raw.empty_folder, R.string.empty_category)
        EmptyStateType.TRASH -> Pair(R.raw.empty_trash, R.string.empty_trash)
    }
    
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes)).value,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(200.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(id = messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

enum class EmptyStateType {
    NOTES, SEARCH, CATEGORY, TRASH
}
```

## 第五阶段：设置与主题实现

### 1. 设置界面实现

实现应用设置界面，允许用户自定义应用行为和外观：

```xml
<!-- res/layout/fragment_settings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
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
            app:title="@string/settings" />

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <!-- 外观设置 -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_margin="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/appearance"
                        android:textAppearance="?attr/textAppearanceTitleMedium"
                        android:layout_marginBottom="16dp" />

                    <!-- 主题选择 -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:layout_marginBottom="16dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/theme"
                            android:textAppearance="?attr/textAppearanceBodyLarge" />

                        <RadioGroup
                            android:id="@+id/theme_radio_group"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="vertical"
                            android:layout_marginTop="8dp">

                            <RadioButton
                                android:id="@+id/theme_light"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:text="@string/theme_light" />

                            <RadioButton
                                android:id="@+id/theme_dark"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:text="@string/theme_dark" />

                            <RadioButton
                                android:id="@+id/theme_system"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:text="@string/theme_system" />

                            <RadioButton
                                android:id="@+id/theme_dynamic"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:text="@string/theme_dynamic"
                                android:enabled="@bool/dynamic_color_supported" />

                        </RadioGroup>
                    </LinearLayout>

                    <!-- 视图模式 -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/default_view"
                            android:textAppearance="?attr/textAppearanceBodyLarge" />

                        <RadioGroup
                            android:id="@+id/view_mode_radio_group"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="vertical"
                            android:layout_marginTop="8dp">

                            <RadioButton
                                android:id="@+id/view_list"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:text="@string/view_list" />

                            <RadioButton
                                android:id="@+id/view_grid"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:text="@string/view_grid" />

                        </RadioGroup>
                    </LinearLayout>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- 同步设置 -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="16dp"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/sync_and_backup"
                        android:textAppearance="?attr/textAppearanceTitleMedium"
                        android:layout_marginBottom="16dp" />

                    <!-- 自动同步开关 -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginBottom="16dp">

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/auto_sync"
                                android:textAppearance="?attr/textAppearanceBodyLarge" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/auto_sync_description"
                                android:textAppearance="?attr/textAppearanceBodySmall"
                                android:textColor="?attr/colorOnSurfaceVariant" />
                        </LinearLayout>

                        <com.google.android.material.switchmaterial.SwitchMaterial
                            android:id="@+id/auto_sync_switch"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content" />
                    </LinearLayout>

                    <!-- 手动备份按钮 -->
                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/backup_button"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="@string/backup_now" />

                    <!-- 恢复备份按钮 -->
                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/restore_button"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="@string/restore_backup"
                        style="@style/Widget.Material3.Button.OutlinedButton" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- 安全设置 -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="16dp"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/security"
                        android:textAppearance="?attr/textAppearanceTitleMedium"
                        android:layout_marginBottom="16dp" />

                    <!-- 应用锁定开关 -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginBottom="16dp">

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/app_lock"
                                android:textAppearance="?attr/textAppearanceBodyLarge" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/app_lock_description"
                                android:textAppearance="?attr/textAppearanceBodySmall"
                                android:textColor="?attr/colorOnSurfaceVariant" />
                        </LinearLayout>

                        <com.google.android.material.switchmaterial.SwitchMaterial
                            android:id="@+id/app_lock_switch"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content" />
                    </LinearLayout>

                    <!-- 更改密码按钮 -->
                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/change_password_button"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="@string/change_password"
                        android:enabled="false" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- 关于应用 -->
            <com.google.android.material.card.MaterialCardView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginHorizontal="16dp"
                android:layout_marginBottom="16dp"
                app:cardCornerRadius="12dp"
                app:cardElevation="2dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/about"
                        android:textAppearance="?attr/textAppearanceTitleMedium"
                        android:layout_marginBottom="16dp" />

                    <!-- 版本信息 -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginBottom="16dp">

                        <TextView
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="@string/version"
                            android:textAppearance="?attr/textAppearanceBodyLarge" />

                        <TextView
                            android:id="@+id/version_text"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textAppearance="?attr/textAppearanceBodyMedium"
                            android:textColor="?attr/colorOnSurfaceVariant"
                            tools:text="1.0.0" />
                    </LinearLayout>

                    <!-- 隐私政策 -->
                    <TextView
                        android:id="@+id/privacy_policy"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="@string/privacy_policy"
                        android:textAppearance="?attr/textAppearanceBodyLarge"
                        android:padding="8dp"
                        android:background="?attr/selectableItemBackground"
                        android:layout_marginBottom="8dp" />

                    <!-- 使用条款 -->
                    <TextView
                        android:id="@+id/terms_of_service"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="@string/terms_of_service"
                        android:textAppearance="?attr/textAppearanceBodyLarge"
                        android:padding="8dp"
                        android:background="?attr/selectableItemBackground" />
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

```kotlin
// ui/screen/settings/SettingsFragment.kt
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupThemeSettings()
        setupViewModeSettings()
        setupSyncSettings()
        setupSecuritySettings()
        setupAboutSection()
        
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupThemeSettings() {
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.theme_light -> ThemeMode.LIGHT
                R.id.theme_dark -> ThemeMode.DARK
                R.id.theme_system -> ThemeMode.SYSTEM
                R.id.theme_dynamic -> ThemeMode.DYNAMIC
                else -> ThemeMode.SYSTEM
            }
            viewModel.setThemeMode(themeMode)
        }
    }
    
    private fun setupViewModeSettings() {
        binding.viewModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val viewMode = when (checkedId) {
                R.id.view_list -> ViewMode.LIST
                R.id.view_grid -> ViewMode.GRID
                else -> ViewMode.LIST
            }
            viewModel.setDefaultViewMode(viewMode)
        }
    }
    
    private fun setupSyncSettings() {
        binding.autoSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoSync(isChecked)
        }
        
        binding.backupButton.setOnClickListener {
            viewModel.backupNow()
        }
        
        binding.restoreButton.setOnClickListener {
            showRestoreConfirmationDialog()
        }
    }
    
    private fun setupSecuritySettings() {
        binding.appLockSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showSetupAppLockDialog()
            } else {
                viewModel.disableAppLock()
            }
        }
        
        binding.changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
    }
    
    private fun setupAboutSection() {
        // 设置版本号
        binding.versionText.text = BuildConfig.VERSION_NAME
        
        // 设置隐私政策点击
        binding.privacyPolicy.setOnClickListener {
            openUrl(getString(R.string.privacy_policy_url))
        }
        
        // 设置使用条款点击
        binding.termsOfService.setOnClickListener {
            openUrl(getString(R.string.terms_of_service_url))
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.themeMode.collect { themeMode ->
                updateThemeRadioButton(themeMode)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.defaultViewMode.collect { viewMode ->
                updateViewModeRadioButton(viewMode)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.autoSync.collect { autoSync ->
                binding.autoSyncSwitch.isChecked = autoSync
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.appLockEnabled.collect { enabled ->
                binding.appLockSwitch.isChecked = enabled
                binding.changePasswordButton.isEnabled = enabled
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.backupState.collect { state ->
                handleBackupState(state)
            }
        }
    }
    
    private fun updateThemeRadioButton(themeMode: ThemeMode) {
        val buttonId = when (themeMode) {
            ThemeMode.LIGHT -> R.id.theme_light
            ThemeMode.DARK -> R.id.theme_dark
            ThemeMode.SYSTEM -> R.id.theme_system
            ThemeMode.DYNAMIC -> R.id.theme_dynamic
        }
        binding.themeRadioGroup.check(buttonId)
    }
    
    private fun updateViewModeRadioButton(viewMode: ViewMode) {
        val buttonId = when (viewMode) {
            ViewMode.LIST -> R.id.view_list
            ViewMode.GRID -> R.id.view_grid
        }
        binding.viewModeRadioGroup.check(buttonId)
    }
    
    private fun handleBackupState(state: BackupState) {
        when (state) {
            is BackupState.Idle -> {
                // 空闲状态，不需要任何操作
            }
            is BackupState.InProgress -> {
                // 显示进度对话框
                showProgressDialog(getString(R.string.backup_in_progress))
            }
            is BackupState.Success -> {
                // 关闭进度对话框，显示成功消息
                dismissProgressDialog()
                Snackbar.make(
                    binding.root,
                    state.message,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            is BackupState.Error -> {
                // 关闭进度对话框，显示错误消息
                dismissProgressDialog()
                Snackbar.make(
                    binding.root,
                    state.message,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun showSetupAppLockDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_setup_app_lock, null)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.password_input)
        val confirmInput = dialogView.findViewById<TextInputEditText>(R.id.confirm_password_input)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.setup_app_lock)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val password = passwordInput.text.toString()
                val confirmPassword = confirmInput.text.toString()
                
                if (password.isEmpty()) {
                    showError(getString(R.string.password_empty))
                    binding.appLockSwitch.isChecked = false
                } else if (password != confirmPassword) {
                    showError(getString(R.string.passwords_dont_match))
                    binding.appLockSwitch.isChecked = false
                } else {
                    viewModel.enableAppLock(password)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                binding.appLockSwitch.isChecked = false
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val current