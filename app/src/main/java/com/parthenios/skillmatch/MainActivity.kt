package com.parthenios.skillmatch.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.parthenios.skillmatch.R
import com.parthenios.skillmatch.auth.AuthRepository
import com.parthenios.skillmatch.databinding.ActivityMainBinding
import com.parthenios.skillmatch.ui.auth.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authRepository = AuthRepository()
        
        // Kullanıcı giriş yapmamışsa login sayfasına yönlendir
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Kullanıcı giriş yapmış, profil kontrolü yap
        checkUserProfileCompleteness(currentUser.uid)
    }

    private fun checkUserProfileCompleteness(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = authRepository.getUserFromFirestore(uid)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        if (user != null && user.firstName.isNotEmpty()) {
                            // Profil tamamlanmış, UI'yi kur
                            setupUI()
                        } else {
                            // Profil eksik, kullanıcıyı Firebase'den sil ve giriş ekranına yönlendir
                            deleteIncompleteUser(uid)
                        }
                    } else {
                        // Firestore'da kullanıcı yok, kullanıcıyı Firebase'den sil ve giriş ekranına yönlendir
                        deleteIncompleteUser(uid)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hata durumunda kullanıcıyı Firebase'den sil ve giriş ekranına yönlendir
                    deleteIncompleteUser(uid)
                }
            }
        }
    }

    private fun deleteIncompleteUser(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Kullanıcıyı Firebase Authentication'dan sil
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    currentUser.delete().await()
                }
                
                withContext(Dispatchers.Main) {
                    // Giriş ekranına yönlendir
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Silme başarısız olursa sadece çıkış yap
                    authRepository.signOut()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun setupUI() {
        // UI'yi sadece profil tamamlandığında yükle
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        val currentUser = authRepository.getCurrentUser()
        binding.tvWelcome.text = "Hoş geldin, ${currentUser?.email}!"
        
        binding.btnLogout.setOnClickListener {
            authRepository.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}