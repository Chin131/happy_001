package com.example.notepro.ui.screen.category

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter for the ViewPager2 in the CategoriesFragment
 * Manages CategoryListFragment and TagListFragment
 */
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