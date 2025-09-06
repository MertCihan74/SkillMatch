package com.parthenios.skillmatch.ui.explore

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.google.android.material.navigation.NavigationView
import com.parthenios.skillmatch.R
import com.parthenios.skillmatch.auth.AuthRepository
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.ActivityExploreBinding
import com.parthenios.skillmatch.ui.auth.LoginActivity
import com.parthenios.skillmatch.ui.profile.ProfileActivity
import com.parthenios.skillmatch.ui.matching.MatchingActivity

class ExploreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExploreBinding
    private lateinit var userAdapter: UserCardAdapter
    private lateinit var authRepository: AuthRepository
    private val users = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Sadece yan ve alt padding uygula, üst padding uygulama
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        
        authRepository = AuthRepository()
        
        setupToolbar()
        setupRecyclerView()
        setupBottomNavigation()
        setupDrawerNavigation()
        loadUsers()
    }
    
    private fun onUserSwipedLeft(user: User) {
        // Kullanıcıyı beğenmedi - listeden çıkar
        users.remove(user)
        userAdapter.notifyDataSetChanged()
        
        // Eğer kart kalmadıysa yeni kartlar yükle
        if (users.isEmpty()) {
            loadUsers()
        }
    }
    
    private fun onUserSwipedRight(user: User) {
        // Kullanıcıyı beğendi - eşleşme isteği gönder
        Toast.makeText(this, "Eşleşme isteği ${user.firstName} ${user.lastName}'a gönderildi! 💕", Toast.LENGTH_SHORT).show()
        
        // Kullanıcıyı listeden çıkar
        users.remove(user)
        userAdapter.notifyDataSetChanged()
        
        // Eğer kart kalmadıysa yeni kartlar yükle
        if (users.isEmpty()) {
            loadUsers()
        }
    }
    
    private fun onUserSwipedDown(user: User) {
        // Aşağı kaydırma - kullanıcı bilgilerini göster
        Toast.makeText(this, "${user.firstName} ${user.lastName} - ${user.city}, ${user.age} yaşında", Toast.LENGTH_LONG).show()
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
                    // Zaten keşfet sayfasındayız
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_matching -> {
                    startActivity(Intent(this, MatchingActivity::class.java))
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

    private fun setupRecyclerView() {
        userAdapter = UserCardAdapter(
            onUserClick = { user ->
                // Kullanıcı kartına tıklandığında profil sayfasına git
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("user", user)
                startActivity(intent)
            },
            onSwipeLeft = { user ->
                // Sola kaydırma - kullanıcıyı reddet
                onUserSwipedLeft(user)
            },
            onSwipeRight = { user ->
                // Sağa kaydırma - eşleşme isteği gönder
                onUserSwipedRight(user)
            },
            onSwipeDown = { user ->
                // Aşağı kaydırma - kullanıcı bilgilerini göster
                onUserSwipedDown(user)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExploreActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = userAdapter
            
            // Kaydırma için PagerSnapHelper ekle
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_explore -> {
                    // Zaten keşfet sayfasındayız
                    true
                }
                R.id.nav_matching -> {
                    // Eşleşme sayfasına git
                    startActivity(Intent(this, MatchingActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // Profil sayfasına git
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        // Keşfet sayfasını seçili göster
        binding.bottomNavigation.selectedItemId = R.id.nav_explore
    }

    private fun loadUsers() {
        // Tek kullanıcı göster
        val sampleUsers = listOf(
            User(
                uid = "1",
                email = "ahmet@example.com",
                firstName = "Ahmet",
                lastName = "Yılmaz",
                username = "ahmetyilmaz",
                city = "İstanbul",
                birthday = "1995-05-15",
                age = 29,
                knownSkills = listOf("Gitar", "Piyano", "Müzik Teorisi"),
                wantedSkills = listOf("Programlama", "Web Tasarım")
            )
        )
        
        users.clear()
        users.addAll(sampleUsers)
        userAdapter.submitList(users)
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
            // Uygulamadan çık
            finishAffinity()
        }
    }
}
