package com.parthenios.skillmatch.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.parthenios.skillmatch.R
import com.parthenios.skillmatch.auth.AuthRepository
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.FragmentProfileBinding
import com.parthenios.skillmatch.ui.auth.LoginActivity
import com.parthenios.skillmatch.utils.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var authRepository: AuthRepository
    private lateinit var userPreferences: UserPreferences
    private var user: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        authRepository = AuthRepository()
        userPreferences = UserPreferences(requireContext())
        
        // Intent'ten kullanıcı bilgilerini al veya lokaldeki kullanıcıyı al
        user = arguments?.getSerializable("user") as? User
        if (user == null) {
            // Önce lokaldeki kullanıcı bilgilerini kontrol et
            user = userPreferences.getUser()
            if (user != null) {
                setupUI(isCurrentUser = true)
            } else {
                loadCurrentUser()
            }
        } else {
            // Başka bir kullanıcının profili
            setupUI(isCurrentUser = false)
        }
    }
    
    private fun loadCurrentUser() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = authRepository.getUserFromFirestore(currentUser.uid)
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            user = result.getOrNull()
                            // Kullanıcı bilgilerini lokalde sakla
                            user?.let { userPreferences.saveUser(it) }
                            setupUI(isCurrentUser = true)
                        } else {
                            // Hata durumunda varsayılan bilgileri göster
                            setupUI(isCurrentUser = true)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        setupUI(isCurrentUser = true)
                    }
                }
            }
        } else {
            setupUI(isCurrentUser = true)
        }
    }

    private fun setupUI(isCurrentUser: Boolean = true) {
        user?.let { userData ->
            binding.apply {
                tvName.text = "${userData.firstName} ${userData.lastName}"
                tvUsername.text = "@${userData.username}"
                tvEmail.text = userData.email
                tvCity.text = userData.city

                // Yaş bilgisini direkt göster
                val age = if (userData.age > 0) userData.age else calculateAge(userData.birthday)
                tvAge.text = "$age yaşında"

                // Bilinen yetenekleri göster
                val knownSkillsText = userData.knownSkills.joinToString(", ")
                tvKnownSkills.text = knownSkillsText

                // Öğrenmek istediği yetenekleri göster
                val wantedSkillsText = userData.wantedSkills.joinToString(", ")
                tvWantedSkills.text = wantedSkillsText
            }
        } ?: run {
            // Eğer kullanıcı bilgisi yoksa mevcut kullanıcıdan bilgi al
            val currentUser = authRepository.getCurrentUser()
            binding.apply {
                tvName.text = currentUser?.displayName ?: "Kullanıcı"
                tvUsername.text = "@${currentUser?.email?.split("@")?.get(0) ?: "kullanici"}"
                tvEmail.text = currentUser?.email ?: ""
                tvCity.text = "Bilinmiyor"
                tvAge.text = "Bilinmiyor"
                tvKnownSkills.text = "Profil bilgileri yükleniyor..."
                tvWantedSkills.text = "Profil bilgileri yükleniyor..."
            }
        }

        // Çıkış yap butonunu sadece kendi profilinde göster
        if (isCurrentUser) {
            binding.btnLogout.visibility = View.VISIBLE
            binding.btnLogout.setOnClickListener {
                authRepository.signOut()
                userPreferences.clearUser()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
        } else {
            binding.btnLogout.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            if (show) {
                // Loading state - tüm bilgileri gizle
                tvName.text = "Yükleniyor..."
                tvUsername.text = ""
                tvEmail.text = ""
                tvCity.text = ""
                tvAge.text = ""
                tvKnownSkills.text = ""
                tvWantedSkills.text = ""
            }
        }
    }

    private fun calculateAge(birthday: String): Int {
        return try {
            val parts = birthday.split("-")
            if (parts.size != 3) {
                println("DEBUG: Birthday format error: $birthday")
                return 0
            }
            
            val birthYear = parts[0].toInt()
            val birthMonth = parts[1].toInt()
            val birthDay = parts[2].toInt()
            
            val calendar = java.util.Calendar.getInstance()
            val currentYear = calendar.get(java.util.Calendar.YEAR)
            val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1 // Calendar.MONTH 0-based
            val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            
            var age = currentYear - birthYear
            
            // Eğer henüz doğum günü gelmemişse yaşı 1 azalt
            if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                age--
            }
            
            println("DEBUG: Birthday: $birthday, BirthYear: $birthYear, CurrentYear: $currentYear, Age: $age")
            age
        } catch (e: Exception) {
            println("DEBUG: Age calculation error: ${e.message}")
            0
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
