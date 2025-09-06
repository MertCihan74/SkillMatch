package com.parthenios.skillmatch.ui.auth

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.parthenios.skillmatch.R
import com.parthenios.skillmatch.auth.AuthRepository
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.FragmentWantedSkillsBinding
import com.parthenios.skillmatch.MainActivity
import com.parthenios.skillmatch.utils.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WantedSkillsFragment : Fragment() {
    private var _binding: FragmentWantedSkillsBinding? = null
    private val binding get() = _binding!!
    private val skillsList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWantedSkillsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSkillInput()

        binding.btnComplete.setOnClickListener {
            if (validateInputs()) {
                completeRegistration()
            }
        }

        binding.btnBack.setOnClickListener {
            (activity as RegisterActivity).goToPreviousStep()
        }
    }

    private fun setupSkillInput() {
        binding.etWantedSkills.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                addSkill()
                true
            } else {
                false
            }
        }

        binding.btnAddSkill.setOnClickListener {
            addSkill()
        }
    }

    private fun addSkill() {
        val skill = binding.etWantedSkills.text.toString().trim()
        if (skill.isNotEmpty()) {
            val formattedSkill = TextUtils.correctCommonSkills(skill)
            if (!skillsList.contains(formattedSkill)) {
                skillsList.add(formattedSkill)
                createSkillChip(formattedSkill)
                binding.etWantedSkills.text?.clear()
                binding.tilWantedSkills.error = null
            }
        }
    }

    private fun createSkillChip(skill: String) {
        val chip = Chip(requireContext())
        chip.text = skill
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            skillsList.remove(skill)
            binding.chipGroupSkills.removeView(chip)
        }
        binding.chipGroupSkills.addView(chip)
    }

    private fun validateInputs(): Boolean {
        if (skillsList.isEmpty()) {
            binding.tilWantedSkills.error = "En az bir yetenek eklemelisin"
            return false
        }
        return true
    }

    private fun completeRegistration() {
        val registerActivity = activity as RegisterActivity
        registerActivity.wantedSkills = skillsList.toList()

        binding.btnComplete.isEnabled = false
        binding.btnComplete.text = "Kayıt tamamlanıyor..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authRepository = AuthRepository()
                val currentUser = authRepository.getCurrentUser()
                
                if (currentUser != null) {
                    // Yaş hesapla
                    val calculatedAge = calculateAge(registerActivity.birthDate)
                    
                    val user = User(
                        uid = currentUser.uid,
                        email = registerActivity.email,
                        firstName = registerActivity.firstName,
                        lastName = registerActivity.lastName,
                        username = registerActivity.username,
                        birthDate = registerActivity.birthDate,
                        age = calculatedAge,
                        city = registerActivity.city,
                        knownSkills = registerActivity.knownSkills,
                        wantedSkills = registerActivity.wantedSkills,
                        birthday = formatBirthday(registerActivity.birthDate)
                    )

                    val result = authRepository.saveUserToFirestore(user)
                    
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            Toast.makeText(
                                requireContext(),
                                "Kayıt başarıyla tamamlandı!",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(android.content.Intent(requireContext(), MainActivity::class.java))
                            requireActivity().finish()
                        } else {
                            binding.btnComplete.isEnabled = true
                            binding.btnComplete.text = "Kayıt Ol"
                            Toast.makeText(
                                requireContext(),
                                "Kayıt sırasında hata oluştu: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.btnComplete.isEnabled = true
                        binding.btnComplete.text = "Kayıt Ol"
                        Toast.makeText(
                            requireContext(),
                            "Kullanıcı oturumu bulunamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnComplete.isEnabled = true
                    binding.btnComplete.text = "Kayıt Ol"
                    Toast.makeText(
                        requireContext(),
                        "Bir hata oluştu: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun calculateAge(birthDate: java.util.Date?): Int {
        if (birthDate == null) return 0
        
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        val birthCalendar = java.util.Calendar.getInstance()
        birthCalendar.time = birthDate
        val birthYear = birthCalendar.get(java.util.Calendar.YEAR)
        val birthMonth = birthCalendar.get(java.util.Calendar.MONTH) + 1
        val birthDay = birthCalendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        var age = currentYear - birthYear
        
        if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
            age--
        }
        
        return age
    }
    
    private fun formatBirthday(birthDate: java.util.Date?): String {
        if (birthDate == null) return ""
        
        val calendar = java.util.Calendar.getInstance()
        calendar.time = birthDate
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}