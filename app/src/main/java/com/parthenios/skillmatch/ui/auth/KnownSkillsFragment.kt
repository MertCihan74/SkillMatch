package com.parthenios.skillmatch.ui.auth

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.parthenios.skillmatch.databinding.FragmentKnownSkillsBinding
import com.parthenios.skillmatch.utils.TextUtils

class KnownSkillsFragment : Fragment() {
    private var _binding: FragmentKnownSkillsBinding? = null
    private val binding get() = _binding!!
    private val skillsList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKnownSkillsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSkillInput()

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

    private fun setupSkillInput() {
        binding.etKnownSkills.setOnEditorActionListener { _, actionId, event ->
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
        val skill = binding.etKnownSkills.text.toString().trim()
        if (skill.isNotEmpty()) {
            val formattedSkill = TextUtils.correctCommonSkills(skill)
            if (!skillsList.contains(formattedSkill)) {
                skillsList.add(formattedSkill)
                createSkillChip(formattedSkill)
                binding.etKnownSkills.text?.clear()
                binding.tilKnownSkills.error = null
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
            binding.tilKnownSkills.error = "En az bir yetenek eklemelisin"
            return false
        }
        return true
    }

    private fun saveData() {
        val registerActivity = activity as RegisterActivity
        registerActivity.knownSkills = skillsList.toList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}