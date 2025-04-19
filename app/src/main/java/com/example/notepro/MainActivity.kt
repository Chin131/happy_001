package com.example.notepro

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.notepro.databinding.ActivityMainBinding
import com.example.notepro.ui.screen.Scrollable
import com.example.notepro.ui.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply splash screen
        installSplashScreen()
        
        // Apply theme before setting content view
        themeManager.applyDynamicColorIfAvailable(this)
        
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
        handleSharedContent(intent)
        observeThemeChanges()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup bottom navigation with nav controller
        binding.bottomNavigation.setupWithNavController(navController)
        
        // FAB click listener for adding new notes
        binding.fabAddNote.setOnClickListener {
            navController.navigate(R.id.action_global_noteEditorFragment)
        }
        
        // Handle destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.categoriesFragment, R.id.searchFragment -> {
                    binding.bottomAppBar.performShow()
                    binding.fabAddNote.show()
                }
                else -> {
                    binding.bottomAppBar.performHide()
                    binding.fabAddNote.hide()
                }
            }
        }
        
        // Double tap on bottom navigation items scrolls content to top
        setupBottomNavDoubleTapListeners()
    }
    
    private fun setupBottomNavDoubleTapListeners() {
        val navItems = mapOf(
            R.id.homeFragment to binding.bottomNavigation.menu.findItem(R.id.homeFragment),
            R.id.categoriesFragment to binding.bottomNavigation.menu.findItem(R.id.categoriesFragment),
            R.id.searchFragment to binding.bottomNavigation.menu.findItem(R.id.searchFragment)
        )
        
        navItems.forEach { (fragmentId, menuItem) ->
            var lastClickTime = 0L
            menuItem?.setOnMenuItemClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 500 && navController.currentDestination?.id == fragmentId) {
                    // Double tap detected - scroll to top if fragment implements Scrollable
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                        ?.childFragmentManager?.fragments?.firstOrNull()
                    if (currentFragment is Scrollable) {
                        currentFragment.scrollToTop()
                    }
                    true
                } else {
                    lastClickTime = currentTime
                    false
                }
            }
        }
    }
    
    private fun handleSharedContent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            
            // Use the global action to navigate to editor with shared content
            val bundle = Bundle().apply {
                putString("content", sharedText)
            }
            navController.navigate(R.id.action_global_noteEditorFragment, bundle)
        }
    }
    
    private fun observeThemeChanges() {
        lifecycleScope.launch {
            themeManager.themeMode.collectLatest { themeMode ->
                themeManager.applyTheme(themeMode)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedContent(intent)
    }
} 