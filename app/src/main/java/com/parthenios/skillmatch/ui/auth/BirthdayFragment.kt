package com.parthenios.skillmatch.ui.auth

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.parthenios.skillmatch.databinding.FragmentBirthdayBinding
import java.util.*

class BirthdayFragment : Fragment() {
    private var _binding: FragmentBirthdayBinding? = null
    private val binding get() = _binding!!
    private var selectedDate: Date? = null
    private var selectedAge: Int = 18

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBirthdayBinding.inflate(inflater, container, false)
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

        binding.etBirthday.setOnClickListener {
            showAgePicker()
        }
    }

    private fun showAgePicker() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(com.parthenios.skillmatch.R.layout.dialog_age_picker, null)
        bottomSheetDialog.setContentView(view)

        val dayPicker = view.findViewById<NumberPicker>(com.parthenios.skillmatch.R.id.dayPicker)
        val monthPicker = view.findViewById<NumberPicker>(com.parthenios.skillmatch.R.id.monthPicker)
        val yearPicker = view.findViewById<NumberPicker>(com.parthenios.skillmatch.R.id.yearPicker)
        val btnConfirm = view.findViewById<com.google.android.material.button.MaterialButton>(com.parthenios.skillmatch.R.id.btnConfirm)

        // Gün picker (1-31)
        dayPicker.minValue = 1
        dayPicker.maxValue = 31
        dayPicker.value = 1

        // Ay picker (1-12) - Ay isimleri ile
        val monthNames = arrayOf(
            "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
        )
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = monthNames
        monthPicker.value = 0

        // Yıl picker (1924-2011)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.minValue = currentYear - 100
        yearPicker.maxValue = currentYear - 13
        yearPicker.value = currentYear - 18

        btnConfirm.setOnClickListener {
            val day = dayPicker.value
            val month = monthPicker.value + 1 // 0-11'den 1-12'ye çevir
            val year = yearPicker.value

            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, day)
            selectedDate = calendar.time
            selectedAge = calculateAge(selectedDate!!)

            val monthNames = arrayOf(
                "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
                "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
            )
            val formattedDate = String.format("%02d %s %04d", day, monthNames[month - 1], year)
            binding.etBirthday.setText(formattedDate)
            binding.tilBirthday.error = null

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun validateInputs(): Boolean {
        if (selectedDate == null) {
            binding.tilBirthday.error = "Doğum günü seçmelisiniz"
            return false
        }

        val age = calculateAge(selectedDate!!)
        if (age < 13) {
            binding.tilBirthday.error = "En az 13 yaşında olmalısınız"
            return false
        }

        if (age > 100) {
            binding.tilBirthday.error = "Geçerli bir doğum tarihi seçin"
            return false
        }

        return true
    }

    private fun calculateAge(birthDate: Date): Int {
        val today = Calendar.getInstance()
        val birth = Calendar.getInstance()
        birth.time = birthDate

        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }

    private fun saveData() {
        val registerActivity = activity as RegisterActivity
        selectedDate?.let { date ->
            registerActivity.birthDate = date
            registerActivity.age = selectedAge
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
