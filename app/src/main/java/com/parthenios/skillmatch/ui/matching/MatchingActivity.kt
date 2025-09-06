package com.parthenios.skillmatch.ui.matching

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
import com.parthenios.skillmatch.databinding.ActivityMatchingBinding
import com.parthenios.skillmatch.ui.auth.LoginActivity
import com.parthenios.skillmatch.ui.explore.ExploreActivity
import com.parthenios.skillmatch.ui.profile.ProfileActivity

class MatchingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMatchingBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            enableEdgeToEdge()
            
            binding = ActivityMatchingBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // Sadece yan ve alt padding uygula, üst padding uygulama
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                insets
            }
            
            authRepository = AuthRepository()
            
            setupToolbar()
            setupUI()
            setupBottomNavigation()
            setupDrawerNavigation()
        } catch (e: Exception) {
            // Hata durumunda basit bir mesaj göster
            android.widget.Toast.makeText(this, "Hata: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
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
                    // Zaten eşleşmeler sayfasındayız
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
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

    private fun setupUI() {
        try {
            binding.apply {
                tvTitle.text = "Eşleşmelerin"
                tvSubtitle.text = "Seninle eşleşen kullanıcıları burada görebilirsin"
                
                // Örnek eşleşme kartları
                setupMatchCards()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "UI Hatası: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun setupMatchCards() {
        // Örnek eşleşme verileri
        val matches = listOf(
            MatchData("Ahmet Yılmaz", "Gitar, Piyano", "Programlama, Web Tasarım", "İstanbul"),
            MatchData("Ayşe Demir", "Programlama, Android", "Gitar, Müzik", "Ankara"),
            MatchData("Mehmet Kaya", "Fotoğrafçılık, Tasarım", "Programlama, Mobil Uygulama", "İzmir")
        )
        
        // Eşleşme kartlarını göster
        binding.apply {
            if (matches.isNotEmpty()) {
                val firstMatch = matches[0]
                tvMatchName1.text = firstMatch.name
                tvMatchSkills1.text = "Bildiği: ${firstMatch.knownSkills}"
                tvMatchWanted1.text = "Öğrenmek İstediği: ${firstMatch.wantedSkills}"
                tvMatchCity1.text = firstMatch.city
                
                if (matches.size > 1) {
                    val secondMatch = matches[1]
                    tvMatchName2.text = secondMatch.name
                    tvMatchSkills2.text = "Bildiği: ${secondMatch.knownSkills}"
                    tvMatchWanted2.text = "Öğrenmek İstediği: ${secondMatch.wantedSkills}"
                    tvMatchCity2.text = secondMatch.city
                }
            } else {
                tvNoMatches.text = "Henüz eşleşmeniz bulunmuyor. Keşfet sayfasından kullanıcıları inceleyin!"
                tvNoMatches.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> {
                    startActivity(Intent(this, ExploreActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_matching -> {
                    // Zaten eşleşme sayfasındayız
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        // Eşleşme sayfasını seçili göster
        binding.bottomNavigation.selectedItemId = R.id.nav_matching
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

    data class MatchData(
        val name: String,
        val knownSkills: String,
        val wantedSkills: String,
        val city: String
    )
}
