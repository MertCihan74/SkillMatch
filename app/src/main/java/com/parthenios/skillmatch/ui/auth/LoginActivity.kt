package com.parthenios.skillmatch.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.parthenios.skillmatch.R
import com.parthenios.skillmatch.auth.AuthRepository
import com.parthenios.skillmatch.databinding.ActivityLoginBinding
import com.parthenios.skillmatch.MainActivity
import com.parthenios.skillmatch.utils.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository()
        userPreferences = UserPreferences(this)

        // Google Sign-In konfigürasyonu
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Her zaman hesap seçme ekranı açılması için mevcut oturumu kapat
        googleSignInClient.signOut()

        setupClickListeners()
        checkCurrentUser()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            loginWithEmail()
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkCurrentUser() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            // Kullanıcı Firebase'de var, Firestore'da profil kontrolü yap
            checkUserProfileCompleteness(currentUser.uid)
        }
    }
    
    private fun checkUserProfileCompleteness(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = authRepository.getUserFromFirestore(uid)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        if (user != null && user.firstName.isNotEmpty()) {
                            // Profil tamamlanmış, kullanıcı bilgilerini lokalde sakla
                            userPreferences.saveUser(user)
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            // Profil eksik, kayıt sayfasına yönlendir
                            Toast.makeText(this@LoginActivity, "Profil bilgilerinizi tamamlayın", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                            finish()
                        }
                    } else {
                        // Firestore'da kullanıcı yok, kullanıcıyı Firebase'den sil ve giriş ekranında bırak
                        deleteIncompleteUser(uid)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hata durumunda kullanıcıyı Firebase'den sil ve giriş ekranında bırak
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
                    // Giriş ekranında kal, kullanıcı tekrar giriş yapabilir
                    Toast.makeText(this@LoginActivity, "Eksik profil temizlendi, tekrar giriş yapabilirsiniz", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Silme başarısız olursa sadece çıkış yap
                    authRepository.signOut()
                    Toast.makeText(this@LoginActivity, "Profil temizlendi, tekrar giriş yapabilirsiniz", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loginWithEmail() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val result = authRepository.signInWithEmail(email, password)
            withContext(Dispatchers.Main) {
                showLoading(false)
                if (result.isSuccess) {
                    // Kullanıcı bilgilerini Firestore'dan al ve lokalde sakla
                    val currentUser = authRepository.getCurrentUser()
                    if (currentUser != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val userResult = authRepository.getUserFromFirestore(currentUser.uid)
                                withContext(Dispatchers.Main) {
                                    if (userResult.isSuccess) {
                                        val user = userResult.getOrNull()
                                        user?.let { userPreferences.saveUser(it) }
                                    }
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                }
                            }
                        }
                    } else {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Giriş başarısız: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun signInWithGoogle() {
        // Her zaman hesap seçme ekranı açılması için
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.let { signInWithGoogleAccount(it) }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google girişi başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogleAccount(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val result = authRepository.signInWithGoogle(account)
            withContext(Dispatchers.Main) {
                showLoading(false)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null) {
                        // Kullanıcı veritabanında var mı kontrol et
                        checkGoogleUserProfile(user.uid)
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Google girişi başarısız: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkGoogleUserProfile(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = authRepository.getUserFromFirestore(uid)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        if (user != null && user.firstName.isNotEmpty()) {
                            // Kullanıcı tam kayıtlı, kullanıcı bilgilerini lokalde sakla
                            userPreferences.saveUser(user)
                            Toast.makeText(this@LoginActivity, "Giriş başarılı!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            // Kullanıcı eksik bilgileri var, kayıt sayfasına git
                            Toast.makeText(this@LoginActivity, "Lütfen profil bilgilerinizi tamamlayın", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                            finish()
                        }
                    } else {
                        // Hata durumunda kayıt sayfasına git
                        Toast.makeText(this@LoginActivity, "Lütfen profil bilgilerinizi tamamlayın", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Bir hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnLogin.isEnabled = !show
        binding.btnGoogleSignIn.isEnabled = !show
    }
}
