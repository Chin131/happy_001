# 安卓记事本应用前端开发全面指南

基于您的要求，我将提供一个全面且系统化的指南，帮助前端开发团队逐步实现NotePro安卓记事本应用的前端部分。这个指南将涵盖项目设置、架构实现、UI组件开发到最终测试的完整流程。

## 第一阶段：项目设置与基础架构

### 1. 项目初始化与依赖配置

首先，需要创建一个基于MVVM架构的Android项目，并配置必要的依赖：

```gradle
// app/build.gradle
plugins {
    id 'com.android.application'
    id 'kotlin-android'
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
}

android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.example.notepro"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
        
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildFeatures {
        viewBinding true
        compose true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion "1.5.1"
    }
    
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    // Core Android dependencies
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // Architecture Components
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.5'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.5'
    
    // Room for local database
    implementation 'androidx.room:room-runtime:2.6.0'
    implementation 'androidx.room:room-ktx:2.6.0'
    kapt 'androidx.room:room-compiler:2.6.0'
    
    // Dagger Hilt for dependency injection
    implementation 'com.google.dagger:hilt-android:2.48'
    kapt 'com.google.dagger:hilt-android-compiler:2.48'
    
    // Coroutines for asynchronous programming
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // DataStore for preferences
    implementation 'androidx.datastore:datastore-preferences:1.0.0'
    
    // Compose UI (for modern UI components)
    implementation 'androidx.compose.ui:ui:1.5.4'
    implementation 'androidx.compose.material3:material3:1.1.2'
    implementation 'androidx.compose.ui:ui-tooling-preview:1.5.4'
    implementation 'androidx.activity:activity-compose:1.8.1'
    debugImplementation 'androidx.compose.ui:ui-tooling:1.5.4'
    
    // Rich text editor support
    implementation 'io.noties.markwon:core:4.6.2'
    implementation 'io.noties.markwon:editor:4.6.2'
    
    // Image loading and caching
    implementation 'io.coil-kt:coil:2.4.0'
    implementation 'io.coil-kt:coil-compose:2.4.0'
    
    // Animation libraries
    implementation 'com.airbnb.android:lottie:6.1.0'
    implementation 'com.airbnb.android:lottie-compose:6.1.0'
    
    // Testing dependencies
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4:1.5.4'
}
```

### 2. 项目结构与包组织

创建一个良好组织的项目结构，遵循MVVM架构：

```
com.example.notepro
├── data
│   ├── local
│   │   ├── dao
│   │   ├── entity
│   │   └── database
│   ├── remote
│   │   ├── api
│   │   ├── dto
│   │   └── service
│   └── repository
├── di
│   ├── module
│   └── qualifier
├── domain
│   ├── model
│   └── usecase
├── ui
│   ├── component
│   │   ├── common
│   │   ├── editor
│   │   ├── note
│   │   └── theme
│   ├── screen
│   │   ├── home
│   │   ├── editor
│   │   ├── category
│   │   ├── search
│   │   └── settings
│   └── theme
└── util
    ├── extension
    └── helper
```

### 3. 应用主题系统设置

创建应用的主题系统，支持浅色/深色模式和Material You动态颜色：

```kotlin
// ui/theme/Theme.kt
@Composable
fun NoteProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

对于XML视图系统，创建对应的主题资源：

```xml
<!-- res/values/themes.xml -->
<resources>
    <!-- Base application theme -->
    <style name="Theme.NotePro" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Primary brand color -->
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryVariant">@color/primary_variant</item>
        <item name="colorOnPrimary">@color/white</item>
        <!-- Secondary brand color -->
        <item name="colorSecondary">@color/secondary</item>
        <item name="colorSecondaryVariant">@color/secondary_variant</item>
        <item name="colorOnSecondary">@color/black</item>
        <!-- Status bar color -->
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">true</item>
        <!-- 其他主题属性 -->
    </style>
</resources>
```

## 第二阶段：核心UI组件开发

### 1. 基础UI组件库

首先开发一套基础UI组件，这些组件将在整个应用中重复使用：

#### 笔记卡片组件（Compose版本）

```kotlin
// ui/component/note/NoteCard.kt
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(note.color ?: MaterialTheme.colorScheme.surfaceVariant.toArgb())
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (note.title.isNotEmpty()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 展示创建/修改时间
                Text(
                    text = formatDateTime(note.modifiedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                // 如果有标签，显示第一个标签
                note.tags.firstOrNull()?.let { tag ->
                    Chip(
                        onClick = { /* 标签点击处理 */ },
                        colors = ChipDefaults.chipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ),
                        label = {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
}

// 格式化时间的辅助函数
private fun formatDateTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
```

#### 笔记卡片组件（XML版本）

```xml
<!-- res/layout/item_note.xml -->
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/note_card"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp">

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
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            tools:text="Meeting Notes" />

        <TextView
            android:id="@+id/note_content"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:ellipsize="end"
            android:maxLines="6"
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

        <com.google.android.material.chip.Chip
            android:id="@+id/note_tag"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textAppearance="?attr/textAppearanceLabelSmall"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toBottomOf="@id/note_content"
            tools:text="Work" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</com.google.android.material.card.MaterialCardView>
```

### 2. 颜色选择器组件

自定义颜色选择器是记事本中的重要组件，用于笔记和分类的颜色定制：

```kotlin
// ui/component/common/ColorPicker.kt
@Composable
fun ColorPickerGrid(
    colors: List<Color>,
    selectedColor: Color?,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = 5
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(colors.size) { index ->
            val color = colors[index]
            ColorItem(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun ColorItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White
            )
        }
    }
}
```

XML版本的颜色选择器：

```kotlin
// ui/component/common/ColorPickerView.kt
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
        
        // 设置颜色列表
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
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#673AB7"), // Deep Purple
            Color.parseColor("#3F51B5"), // Indigo
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#03A9F4"), // Light Blue
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#009688"), // Teal
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#8BC34A"), // Light Green
            Color.parseColor("#CDDC39"), // Lime
            Color.parseColor("#FFEB3B"), // Yellow
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#FF9800")  // Orange
        )
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
                if (luminance > 0.5) Color.BLACK else Color.WHITE
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
```

### 3. 富文本编辑器实现

富文本编辑器是记事本应用的核心组件，下面提供一个基于Markdown的富文本编辑实现：

```kotlin
// ui/component/editor/MarkdownEditor.kt
class MarkdownEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewMarkdownEditorBinding
    
    private val markwon: Markwon
    private val editor: MarkwonEditor
    private val textWatcher: TextWatcher
    
    var onTextChangedListener: ((String) -> Unit)? = null
    
    init {
        orientation = VERTICAL
        
        // 初始化Markwon
        markwon = Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .build()
        
        editor = MarkwonEditor.create(markwon)
        
        // 初始化视图绑定
        binding = ViewMarkdownEditorBinding.inflate(LayoutInflater.from(context), this, true)
        
        // 配置编辑器
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    onTextChangedListener?.invoke(it.toString())
                    updatePreview(it.toString())
                }
            }
        }
        
        binding.editorEditText.addTextChangedListener(textWatcher)
        
        // 设置工具栏点击事件
        setupToolbar()
    }
    
    private fun setupToolbar() {
        // 粗体按钮
        binding.toolbarBold.setOnClickListener {
            insertMarkdownSyntax("**", "**", "粗体文本")
        }
        
        // 斜体按钮
        binding.toolbarItalic.setOnClickListener {
            insertMarkdownSyntax("*", "*", "斜体文本")
        }
        
        // 标题按钮
        binding.toolbarHeading.setOnClickListener {
            insertMarkdownSyntax("# ", "", "标题")
        }
        
        // 列表按钮
        binding.toolbarList.setOnClickListener {
            insertMarkdownSyntax("- ", "", "列表项")
        }
        
        // 代码按钮
        binding.toolbarCode.setOnClickListener {
            insertMarkdownSyntax("`", "`", "代码")
        }
        
        // 链接按钮
        binding.toolbarLink.setOnClickListener {
            insertMarkdownSyntax("[", "](https://)", "链接文本")
        }
        
        // 切换预览
        binding.toolbarPreview.setOnClickListener {
            togglePreviewMode()
        }
    }
    
    private fun insertMarkdownSyntax(prefix: String, suffix: String, defaultText: String) {
        val editText = binding.editorEditText
        val start = editText.selectionStart
        val end = editText.selectionEnd
        
        val editable = editText.text
        if (start != end) {
            // 选中了文本，在选中的文本前后插入语法
            val selectedText = editable.subSequence(start, end).toString()
            editable.replace(start, end, prefix + selectedText + suffix)
            editText.setSelection(start + prefix.length, start + prefix.length + selectedText.length)
        } else {
            // 没有选中文本，插入带有默认文本的语法
            editable.insert(start, prefix + defaultText + suffix)
            editText.setSelection(start + prefix.length, start + prefix.length + defaultText.length)
        }
    }
    
    private fun updatePreview(markdown: String) {
        if (binding.previewTextView.visibility == View.VISIBLE) {
            markwon.setMarkdown(binding.previewTextView, markdown)
        }
    }
    
    private fun togglePreviewMode() {
        if (binding.editorEditText.visibility == View.VISIBLE) {
            // 切换到预览模式
            binding.editorEditText.visibility = View.GONE
            binding.previewTextView.visibility = View.VISIBLE
            binding.toolbarPreview.setImageResource(R.drawable.ic_edit)
            
            // 更新预览内容
            updatePreview(binding.editorEditText.text.toString())
        } else {
            // 切换到编辑模式
            binding.editorEditText.visibility = View.VISIBLE
            binding.previewTextView.visibility = View.GONE
            binding.toolbarPreview.setImageResource(R.drawable.ic_preview)
        }
    }
    
    fun setMarkdown(markdown: String) {
        binding.editorEditText.setText(markdown)
    }
    
    fun getMarkdown(): String {
        return binding.editorEditText.text.toString()
    }
}
```

Compose版本的富文本编辑器：

```kotlin
// ui/component/editor/MarkdownEditorCompose.kt
@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPreview by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 工具栏
        MarkdownToolbar(
            onBoldClick = { insertMarkdownSyntax("**", "**", "粗体文本", value, onValueChange) },
            onItalicClick = { insertMarkdownSyntax("*", "*", "斜体文本", value, onValueChange) },
            onHeadingClick = { insertMarkdownSyntax("# ", "", "标题", value, onValueChange) },
            onListClick = { insertMarkdownSyntax("- ", "", "列表项", value, onValueChange) },
            onCodeClick = { insertMarkdownSyntax("`", "`", "代码", value, onValueChange) },
            onLinkClick = { insertMarkdownSyntax("[", "](https://)", "链接文本", value, onValueChange) },
            onPreviewClick = { showPreview = !showPreview }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 编辑区域或预览区域
        if (showPreview) {
            MarkdownPreview(
                markdown = value,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        } else {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun MarkdownToolbar(
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onHeadingClick: () -> Unit,
    onListClick: () -> Unit,
    onCodeClick: () -> Unit,
    onLinkClick: () -> Unit,
    onPreviewClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolbarButton(
            icon = Icons.Default.FormatBold,
            contentDescription = "Bold",
            onClick = onBoldClick
        )
        
        ToolbarButton(
            icon = Icons.Default.FormatItalic,
            contentDescription = "Italic",
            onClick = onItalicClick
        )
        
        ToolbarButton(
            icon = Icons.Default.Title,
            contentDescription = "Heading",
            onClick = onHeadingClick
        )
        
        ToolbarButton(
            icon = Icons.Default.FormatListBulleted,
            contentDescription = "List",
            onClick = onListClick
        )
        
        ToolbarButton(
            icon = Icons.Default.Code,
            contentDescription = "Code",
            onClick = onCodeClick
        )
        
        ToolbarButton(
            icon = Icons.Default.Link,
            contentDescription = "Link",
            onClick = onLinkClick
        )
        
        ToolbarButton(
            icon = Icons.Default.Visibility,
            contentDescription = "Preview",
            onClick = onPreviewClick
        )
    }
}

@Composable
fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 将Markdown语法插入文本的辅助函数
private fun insertMarkdownSyntax(
    prefix: String,
    suffix: String,
    defaultText: String,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    // 简单实现，实际应用中需要考虑文本选择范围
    onValueChange(currentValue + prefix + defaultText + suffix)
}

// Markdown预览组件
@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier
) {
    // 简化