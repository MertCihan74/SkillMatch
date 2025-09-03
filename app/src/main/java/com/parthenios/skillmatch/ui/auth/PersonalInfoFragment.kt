package com.parthenios.skillmatch.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.parthenios.skillmatch.databinding.FragmentPersonalInfoBinding

class PersonalInfoFragment : Fragment() {
    private var _binding: FragmentPersonalInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Google kullanıcısı ise ad soyad otomatik doldur
        autoFillGoogleUserInfo()

        binding.btnNext.setOnClickListener {
            if (validateInputs()) {
                saveData()
                (activity as RegisterActivity).goToNextStep()
            }
        }

        binding.btnBack.setOnClickListener {
            (activity as RegisterActivity).goToPreviousStep()
        }
    }

    private fun autoFillGoogleUserInfo() {
        val registerActivity = activity as RegisterActivity
        val authRepository = com.parthenios.skillmatch.auth.AuthRepository()
        val currentUser = authRepository.getCurrentUser()

        // Google kullanıcısı ise email'i her zaman göster
        if (currentUser != null && currentUser.email != null) {
            binding.etEmail.setText(currentUser.email)
            registerActivity.email = currentUser.email!!
            
            if (currentUser.displayName != null) {
                val displayName = currentUser.displayName!!
                val nameParts = displayName.split(" ")
                
                if (nameParts.isNotEmpty()) {
                    // İlk isim ad kısmına
                    binding.etFirstName.setText(nameParts[0])
                    registerActivity.firstName = nameParts[0]
                }
                
                if (nameParts.size > 1) {
                    if (nameParts.size == 2) {
                        // 2 kelime: ilki isim, ikincisi soyad
                        binding.etFirstName.setText(nameParts[0])
                        binding.etLastName.setText(nameParts[1])
                        registerActivity.firstName = nameParts[0]
                        registerActivity.lastName = nameParts[1]
                    } else if (nameParts.size >= 3) {
                        // 3+ kelime: ilk 2'si isim, geri kalanı soyad
                        val firstName = "${nameParts[0]} ${nameParts[1]}"
                        val lastName = nameParts.subList(2, nameParts.size).joinToString(" ")
                        binding.etFirstName.setText(firstName)
                        binding.etLastName.setText(lastName)
                        registerActivity.firstName = firstName
                        registerActivity.lastName = lastName
                    }
                }
                
                // Username için öneri ver
                val emailUsername = currentUser.email!!.substringBefore("@")
                binding.etUsername.setText(emailUsername)
                registerActivity.username = emailUsername
            }
        } else if (registerActivity.email.isNotEmpty()) {
            // Email zaten dolu, firstName'e odaklan
            binding.etFirstName.requestFocus()
        }
    }

    private fun validateInputs(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()

        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "Ad gerekli"
            return false
        }

        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Soyad gerekli"
            return false
        }

        if (username.isEmpty()) {
            binding.tilUsername.error = "Kullanıcı adı gerekli"
            return false
        }

        if (username.length < 3) {
            binding.tilUsername.error = "Kullanıcı adı en az 3 karakter olmalı"
            return false
        }

        return true
    }

    private fun saveData() {
        val registerActivity = activity as RegisterActivity
        registerActivity.firstName = binding.etFirstName.text.toString().trim()
        registerActivity.lastName = binding.etLastName.text.toString().trim()
        registerActivity.username = binding.etUsername.text.toString().trim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}