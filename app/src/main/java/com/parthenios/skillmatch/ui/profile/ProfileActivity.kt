package com.parthenios.skillmatch.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.parthenios.skillmatch.R
import com.parthenios.skillmatch.auth.AuthRepository
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.ActivityProfileBinding
import com.parthenios.skillmatch.ui.auth.LoginActivity
import com.parthenios.skillmatch.ui.explore.ExploreActivity
import com.parthenios.skillmatch.ui.matching.MatchingActivity

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var authRepository: AuthRepository
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        
        authRepository = AuthRepository()
        
        setupToolbar()
        setupDrawerNavigation()
        
        // ProfileFragment'i oluştur
        val profileFragment = ProfileFragment()
        
        // Intent'ten kullanıcı bilgilerini al
        user = intent.getSerializableExtra("user") as? User
        if (user != null) {
            // Başka kullanıcının profili - fragment'e user bilgisini gönder
            val bundle = Bundle()
            bundle.putSerializable("user", user)
            profileFragment.arguments = bundle
        }
        // Eğer user null ise kendi profilimizi göster (ProfileFragment'te otomatik olarak lokaldeki kullanıcıyı alacak)
        
        // Fragment'i göster
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, profileFragment)
            .commit()

        setupBottomNavigation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
    }

    private fun setupDrawerNavigation() {
        binding.navigationDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_explore -> {
                    startActivity(Intent(this, ExploreActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_matching -> {
                    startActivity(Intent(this, MatchingActivity::class.java))
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_profile -> {
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
                    startActivity(Intent(this, ExploreActivity::class.java))
                    true
                }
                R.id.nav_matching -> {
                    startActivity(Intent(this, MatchingActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // Kendi profilimizi göster (user bilgisi olmadan)
                    val profileFragment = ProfileFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, profileFragment)
                        .commit()
                    true
                }
                else -> false
            }
        }
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