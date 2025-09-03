package com.parthenios.skillmatch.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.parthenios.skillmatch.databinding.FragmentEmailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmailFragment : Fragment() {
    private var _binding: FragmentEmailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNext.setOnClickListener {
            if (validateInputs()) {
                createAccount()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Email validasyonu
        if (email.isEmpty()) {
            binding.tilEmail.error = "E-posta adresi gerekli"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Geçerli bir e-posta adresi girin"
            return false
        }

        // Email domain kontrolü
        if (!isValidEmailDomain(email)) {
            binding.tilEmail.error = "Geçerli bir e-posta domain'i girin (.com, .org, .net vb.)"
            return false
        }

        // Şifre validasyonu
        if (password.isEmpty()) {
            binding.tilPassword.error = "Şifre gerekli"
            return false
        }

        val passwordValidation = validatePassword(password)
        if (!passwordValidation.isValid) {
            binding.tilPassword.error = passwordValidation.errorMessage
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Şifre tekrarı gerekli"
            return false
        }

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Şifreler eşleşmiyor"
            return false
        }

        return true
    }

    private fun isValidEmailDomain(email: String): Boolean {
        val validDomains = listOf(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "icloud.com",
            "protonmail.com", "yandex.com", "mail.ru", "live.com", "msn.com",
            "aol.com", "zoho.com", "fastmail.com", "tutanota.com", "gmx.com",
            "web.de", "freenet.de", "t-online.de", "arcor.de", "gmx.de",
            "orange.fr", "wanadoo.fr", "free.fr", "laposte.net", "sfr.fr",
            "libero.it", "virgilio.it", "tiscali.it", "alice.it", "tin.it",
            "terra.es", "telefonica.net", "ya.com", "ono.com", "movistar.es",
            "telenet.be", "skynet.be", "scarlet.be", "pandora.be", "base.be",
            "ziggo.nl", "kpn.nl", "online.nl", "planet.nl", "chello.nl",
            "bluewin.ch", "swissonline.ch", "hispeed.ch", "sunrise.ch", "cablecom.ch",
            "chello.at", "aon.at", "tele2.at", "inode.at", "netway.at",
            "telia.se", "comhem.se", "bredband.net", "bahnhof.se", "tele2.se",
            "telenor.no", "online.no", "start.no", "chello.no", "broadpark.no",
            "tdc.dk", "get2net.dk", "cybercity.dk", "worldonline.dk", "post.tele.dk",
            "sonera.fi", "elisa.fi", "saunalahti.fi", "welho.fi", "kolumbus.fi",
            "wp.pl", "onet.pl", "interia.pl", "gazeta.pl", "o2.pl",
            "seznam.cz", "centrum.cz", "volny.cz", "tiscali.cz", "quick.cz",
            "freemail.hu", "citromail.hu", "vipmail.hu", "t-email.hu", "invitel.hu",
            "yahoo.ro", "gmail.ro", "zappmobile.ro", "rdsnet.ro", "clicknet.ro",
            "abv.bg", "mail.bg", "dir.bg", "inet.bg", "gbg.bg",
            "net.hr", "vip.hr", "t-com.hr", "inet.hr", "optinet.hr",
            "siol.net", "t-2.net", "amis.net", "volja.net", "telemach.net",
            "azet.sk", "centrum.sk", "post.sk", "stonline.sk", "slovanet.sk",
            "omnitel.net", "delfi.lt", "takas.lt", "lrt.lt", "lrytas.lt",
            "apollo.lv", "inbox.lv", "mail.lv", "tvnet.lv", "delfi.lv",
            "hot.ee", "mail.ee", "zone.ee", "online.ee", "internet.ee",
            "otenet.gr", "forthnet.gr", "hol.gr", "otenet.gr", "in.gr",
            "cytanet.com.cy", "cablenet.com.cy", "prime-tel.com.cy", "cyta.com.cy", "cablenet.com.cy",
            "go.com.mt", "maltanet.net", "onvol.net", "maltacom.net", "melita.com",
            "pt.lu", "vo.lu", "tango.lu", "orange.lu", "post.lu",
            "eircom.net", "oceanfree.net", "indigo.ie", "eircom.ie", "iol.ie",
            "sapo.pt", "clix.pt", "netcabo.pt", "mail.pt", "iol.pt",
            "simnet.is", "visir.is", "mbl.is", "visir.is", "simnet.is"
        )
        
        val domain = email.substringAfter("@").lowercase()
        
        // Sahte domain'leri kontrol et
        val fakeDomains = listOf(
            "hmail.com", "gmail.co", "yahoo.co", "hotmail.co", "outlook.co",
            "gmail.cm", "yahoo.cm", "hotmail.cm", "outlook.cm", "gmail.con",
            "yahoo.con", "hotmail.con", "outlook.con", "gmail.om", "yahoo.om"
        )
        
        if (fakeDomains.contains(domain)) {
            return false
        }
        
        return validDomains.contains(domain)
    }

    private fun validatePassword(password: String): PasswordValidation {
        if (password.length < 8) {
            return PasswordValidation(false, "Şifre en az 8 karakter olmalı")
        }

        if (!password.any { it.isUpperCase() }) {
            return PasswordValidation(false, "Şifre en az 1 büyük harf içermeli")
        }

        if (!password.any { it.isLowerCase() }) {
            return PasswordValidation(false, "Şifre en az 1 küçük harf içermeli")
        }

        if (!password.any { it.isDigit() }) {
            return PasswordValidation(false, "Şifre en az 1 rakam içermeli")
        }

        if (!password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) {
            return PasswordValidation(false, "Şifre en az 1 özel karakter içermeli (!@#$%^&* vb.)")
        }

        return PasswordValidation(true, "")
    }

    data class PasswordValidation(val isValid: Boolean, val errorMessage: String)

    private fun createAccount() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.btnNext.isEnabled = false
        binding.btnNext.text = "Hesap oluşturuluyor..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authRepository = com.parthenios.skillmatch.auth.AuthRepository()
                val result = authRepository.createUserWithEmail(email, password)
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        // Email ve şifre bilgilerini RegisterActivity'ye kaydet
                        (activity as? RegisterActivity)?.let { registerActivity ->
                            registerActivity.email = email
                            registerActivity.password = password
                        }
                        (activity as RegisterActivity).goToNextStep()
                    } else {
                        binding.btnNext.isEnabled = true
                        binding.btnNext.text = "Devam Et"
                        Toast.makeText(
                            requireContext(),
                            "Hesap oluşturulamadı: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnNext.isEnabled = true
                    binding.btnNext.text = "Devam Et"
                    Toast.makeText(
                        requireContext(),
                        "Bir hata oluştu: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}