package com.parthenios.skillmatch.utils

object TextUtils {
    
    /**
     * Metni Türkçe sözlüğe uygun şekilde düzenler
     * - İlk harfi büyük yapar
     * - Diğer harfleri küçük yapar
     * - Türkçe karakterleri düzeltir
     */
    fun capitalizeTurkish(text: String): String {
        if (text.isEmpty()) return text
        
        val trimmed = text.trim()
        val words = trimmed.split("\\s+".toRegex())
        
        return words.joinToString(" ") { word ->
            if (word.isEmpty()) return@joinToString ""
            
            val firstChar = word[0].uppercaseChar()
            val restChars = word.substring(1).lowercase()
            
            // Türkçe karakter düzeltmeleri
            val correctedWord = (firstChar + restChars)
                .replace("i", "İ", ignoreCase = false)
                .replace("ı", "I", ignoreCase = false)
                .replace("ğ", "Ğ", ignoreCase = false)
                .replace("ü", "Ü", ignoreCase = false)
                .replace("ş", "Ş", ignoreCase = false)
                .replace("ö", "Ö", ignoreCase = false)
                .replace("ç", "Ç", ignoreCase = false)
            
            correctedWord
        }
    }
    
    /**
     * Yetenek adını düzenler
     */
    fun formatSkillName(skill: String): String {
        return capitalizeTurkish(skill)
    }
    
    /**
     * Yaygın yetenek isimlerini düzeltir
     */
    fun correctCommonSkills(skill: String): String {
        val skillMap = mapOf(
            "yazilim" to "Yazılım",
            "programlama" to "Programlama",
            "kodlama" to "Kodlama",
            "gitar" to "Gitar",
            "piyano" to "Piyano",
            "resim" to "Resim",
            "fotografcilik" to "Fotoğrafçılık",
            "fotograf" to "Fotoğraf",
            "yuzme" to "Yüzme",
            "futbol" to "Futbol",
            "basketbol" to "Basketbol",
            "tenis" to "Tenis",
            "yoga" to "Yoga",
            "pilates" to "Pilates",
            "dans" to "Dans",
            "muzik" to "Müzik",
            "sarkı" to "Şarkı",
            "sarki" to "Şarkı",
            "kitap" to "Kitap",
            "okuma" to "Okuma",
            "yazma" to "Yazma",
            "cizim" to "Çizim",
            "cizim" to "Çizim",
            "el sanatlari" to "El Sanatları",
            "el sanatları" to "El Sanatları",
            "dikiş" to "Dikiş",
            "dikis" to "Dikiş",
            "yemek" to "Yemek",
            "mutfak" to "Mutfak",
            "bahcecilik" to "Bahçıvanlık",
            "bahçıvanlık" to "Bahçıvanlık",
            "dil" to "Dil",
            "ingilizce" to "İngilizce",
            "almanca" to "Almanca",
            "fransizca" to "Fransızca",
            "fransızca" to "Fransızca",
            "ispanyolca" to "İspanyolca",
            "arapca" to "Arapça",
            "arapça" to "Arapça",
            // Yeni eklenenler
            "kopek bakiciligi" to "Köpek Bakıcılığı",
            "köpek bakıcılığı" to "Köpek Bakıcılığı",
            "kopek bakiciligi" to "Köpek Bakıcılığı",
            "kedi bakiciligi" to "Kedi Bakıcılığı",
            "kedi bakıcılığı" to "Kedi Bakıcılığı",
            "hayvan bakiciligi" to "Hayvan Bakıcılığı",
            "hayvan bakıcılığı" to "Hayvan Bakıcılığı",
            "cocuk bakiciligi" to "Çocuk Bakıcılığı",
            "çocuk bakıcılığı" to "Çocuk Bakıcılığı",
            "yaşlı bakiciligi" to "Yaşlı Bakıcılığı",
            "yaşlı bakıcılığı" to "Yaşlı Bakıcılığı",
            "ev temizligi" to "Ev Temizliği",
            "ev temizliği" to "Ev Temizliği",
            "bahcecilik" to "Bahçıvanlık",
            "bahçıvanlık" to "Bahçıvanlık",
            "bitki bakiciligi" to "Bitki Bakıcılığı",
            "bitki bakıcılığı" to "Bitki Bakıcılığı",
            "araba kullanma" to "Araba Kullanma",
            "motor kullanma" to "Motor Kullanma",
            "bisiklet" to "Bisiklet",
            "yuzme" to "Yüzme",
            "kosu" to "Koşu",
            "koşu" to "Koşu",
            "fitness" to "Fitness",
            "spor" to "Spor",
            "egzersiz" to "Egzersiz",
            "meditasyon" to "Meditasyon",
            "nefes egzersizi" to "Nefes Egzersizi",
            "nefes egzersizi" to "Nefes Egzersizi",
            "masaj" to "Masaj",
            "terapi" to "Terapi",
            "psikoloji" to "Psikoloji",
            "sosyoloji" to "Sosyoloji",
            "felsefe" to "Felsefe",
            "tarih" to "Tarih",
            "cografya" to "Coğrafya",
            "coğrafya" to "Coğrafya",
            "matematik" to "Matematik",
            "fizik" to "Fizik",
            "kimya" to "Kimya",
            "biyoloji" to "Biyoloji",
            "edebiyat" to "Edebiyat",
            "sanat" to "Sanat",
            "heykel" to "Heykel",
            "seramik" to "Seramik",
            "ahşap işçiliği" to "Ahşap İşçiliği",
            "ahşap işçiliği" to "Ahşap İşçiliği",
            "metal işçiliği" to "Metal İşçiliği",
            "metal işçiliği" to "Metal İşçiliği",
            "deri işçiliği" to "Deri İşçiliği",
            "deri işçiliği" to "Deri İşçiliği",
            "takı yapımı" to "Takı Yapımı",
            "takı yapımı" to "Takı Yapımı",
            "kuyumculuk" to "Kuyumculuk",
            "saat tamiri" to "Saat Tamiri",
            "saat tamiri" to "Saat Tamiri",
            "elektronik tamiri" to "Elektronik Tamiri",
            "elektronik tamiri" to "Elektronik Tamiri",
            "bilgisayar tamiri" to "Bilgisayar Tamiri",
            "bilgisayar tamiri" to "Bilgisayar Tamiri",
            "telefon tamiri" to "Telefon Tamiri",
            "telefon tamiri" to "Telefon Tamiri",
            "araba tamiri" to "Araba Tamiri",
            "araba tamiri" to "Araba Tamiri",
            "motor tamiri" to "Motor Tamiri",
            "motor tamiri" to "Motor Tamiri"
        )
        
        val lowerSkill = skill.lowercase().trim()
        return skillMap[lowerSkill] ?: formatSkillName(skill)
    }
}
