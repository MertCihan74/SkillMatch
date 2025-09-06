package com.parthenios.skillmatch

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.parthenios.skillmatch.auth.AuthRepository
import com.parthenios.skillmatch.databinding.ActivityMainBinding
import com.parthenios.skillmatch.ui.auth.LoginActivity
import com.parthenios.skillmatch.ui.explore.ExploreFragment
import com.parthenios.skillmatch.ui.matching.MatchingFragment
import com.parthenios.skillmatch.ui.profile.ProfileFragment
import com.parthenios.skillmatch.ui.chat.ChatListFragment
import com.parthenios.skillmatch.utils.UserPreferences

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Sadece yan ve alt padding uygula, üst padding uygulama
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        
        authRepository = AuthRepository()
        userPreferences = UserPreferences(this)
        
        // Kullanıcı giriş yapmış mı kontrol et
        if (authRepository.getCurrentUser() == null || !userPreferences.isUserLoggedIn()) {
            // Kullanıcı giriş yapmamış, LoginActivity'ye yönlendir
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setupToolbar()
        setupDrawerNavigation()
        setupBottomNavigation()
        
        // Varsayılan olarak Keşfet fragment'ini göster
        if (savedInstanceState == null) {
            showFragment(ExploreFragment())
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
        
        // Profil butonuna click listener ekle
        binding.ivProfile.setOnClickListener {
            showFragment(ProfileFragment())
        }
    }

    private fun setupDrawerNavigation() {
        binding.navigationDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_explore -> {
                    showFragment(ExploreFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_matching -> {
                    showFragment(MatchingFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_profile -> {
                    showFragment(ProfileFragment())
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Ayarlar yakında gelecek!", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_help -> {
                    Toast.makeText(this, "Yardım yakında gelecek!", Toast.LENGTH_SHORT).show()
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_logout -> {
                    authRepository.signOut()
                    userPreferences.clearUser()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> {
                    showFragment(ExploreFragment())
                    true
                }
                R.id.nav_matching -> {
                    showFragment(MatchingFragment())
                    true
                }
                R.id.nav_chat -> {
                    // Chat listesi göster (aktif eşleşmeler)
                    showChatList()
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun showChatList() {
        showFragment(ChatListFragment())
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(binding.navigationDrawer)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.navigationDrawer)) {
            binding.drawerLayout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }
}
