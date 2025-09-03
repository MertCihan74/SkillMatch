package com.parthenios.skillmatch.ui.auth

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class RegisterPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 6

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> EmailFragment()
            1 -> PersonalInfoFragment()
            2 -> BirthdayFragment()
            3 -> CityFragment()
            4 -> KnownSkillsFragment()
            5 -> WantedSkillsFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
