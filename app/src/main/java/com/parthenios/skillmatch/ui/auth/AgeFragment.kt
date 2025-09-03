package com.parthenios.skillmatch.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.parthenios.skillmatch.databinding.FragmentAgeBinding

class AgeFragment : Fragment() {
    private var _binding: FragmentAgeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    private fun validateInputs(): Boolean {
        val ageText = binding.etAge.text.toString().trim()

        if (ageText.isEmpty()) {
            binding.tilAge.error = "Yaş gerekli"
            return false
        }

        val age = ageText.toIntOrNull()
        if (age == null || age < 13 || age > 100) {
            binding.tilAge.error = "Geçerli bir yaş girin (13-100)"
            return false
        }

        return true
    }

    private fun saveData() {
        val registerActivity = activity as RegisterActivity
        registerActivity.age = binding.etAge.text.toString().trim().toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}