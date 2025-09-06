package com.parthenios.skillmatch.utils

import com.parthenios.skillmatch.data.User

object MatchAlgorithm {
    
    /**
     * İki kullanıcı arasında eşleşme olup olmadığını kontrol eder
     * Eşleşme kriteri:
     * - User1'in öğrenmek istediği yetenekler, User2'nin bildiği yeteneklerle eşleşmeli
     * - User2'nin öğrenmek istediği yetenekler, User1'in bildiği yeteneklerle eşleşmeli
     */
    fun isMatch(user1: User, user2: User): Boolean {
        // User1'in öğrenmek istediği yetenekler, User2'nin bildiği yeteneklerle eşleşiyor mu?
        val user1WantsMatch = user1.wantedSkills.any { wantedSkill ->
            user2.knownSkills.any { knownSkill ->
                isSkillMatch(wantedSkill, knownSkill)
            }
        }
        
        // User2'nin öğrenmek istediği yetenekler, User1'in bildiği yeteneklerle eşleşiyor mu?
        val user2WantsMatch = user2.wantedSkills.any { wantedSkill ->
            user1.knownSkills.any { knownSkill ->
                isSkillMatch(wantedSkill, knownSkill)
            }
        }
        
        // Her iki yön de eşleşmeli
        return user1WantsMatch && user2WantsMatch
    }
    
    /**
     * İki yetenek arasında eşleşme olup olmadığını kontrol eder
     * Büyük/küçük harf duyarsız ve kısmi eşleşme destekler
     */
    private fun isSkillMatch(skill1: String, skill2: String): Boolean {
        val normalizedSkill1 = skill1.lowercase().trim()
        val normalizedSkill2 = skill2.lowercase().trim()
        
        // Tam eşleşme
        if (normalizedSkill1 == normalizedSkill2) {
            return true
        }
        
        // Kısmi eşleşme (bir yetenek diğerini içeriyor)
        if (normalizedSkill1.contains(normalizedSkill2) || normalizedSkill2.contains(normalizedSkill1)) {
            return true
        }
        
        // Benzer yetenekler için özel eşleşmeler
        val similarSkills = getSimilarSkills(normalizedSkill1)
        return similarSkills.any { similarSkill ->
            similarSkill == normalizedSkill2
        }
    }
    
    /**
     * Benzer yetenekler listesi
     */
    private fun getSimilarSkills(skill: String): List<String> {
        val skillMap = mapOf(
            "programlama" to listOf("kodlama", "yazılım", "software", "development", "geliştirme"),
            "gitar" to listOf("müzik", "enstrüman", "çalgı"),
            "piyano" to listOf("müzik", "enstrüman", "çalgı", "klavye"),
            "fotoğrafçılık" to listOf("fotoğraf", "görsel", "tasarım"),
            "tasarım" to listOf("design", "görsel", "grafik", "ui", "ux"),
            "yoga" to listOf("meditasyon", "spor", "fitness", "sağlık"),
            "dans" to listOf("müzik", "hareket", "ritim"),
            "android" to listOf("mobil", "uygulama", "app", "kotlin", "java"),
            "web" to listOf("html", "css", "javascript", "frontend", "backend"),
            "python" to listOf("programlama", "kodlama", "yazılım"),
            "java" to listOf("programlama", "kodlama", "yazılım"),
            "kotlin" to listOf("android", "mobil", "programlama"),
            "javascript" to listOf("web", "frontend", "programlama"),
            "photoshop" to listOf("tasarım", "görsel", "editing", "düzenleme"),
            "müzik" to listOf("gitar", "piyano", "enstrüman", "çalgı", "dans"),
            "spor" to listOf("fitness", "yoga", "egzersiz", "sağlık"),
            "sağlık" to listOf("yoga", "spor", "fitness", "meditasyon"),
            "meditasyon" to listOf("yoga", "sağlık", "spiritual", "mindfulness")
        )
        
        return skillMap[skill] ?: emptyList()
    }
    
    /**
     * Kullanıcı için potansiyel eşleşmeleri bulur
     */
    fun findPotentialMatches(currentUser: User, allUsers: List<User>): List<User> {
        return allUsers.filter { otherUser ->
            // Kendisi değil
            otherUser.uid != currentUser.uid &&
            // Eşleşme var
            isMatch(currentUser, otherUser)
        }
    }
    
    /**
     * Eşleşme skorunu hesaplar (0-100 arası)
     */
    fun calculateMatchScore(user1: User, user2: User): Int {
        if (!isMatch(user1, user2)) return 0
        
        var score = 0
        
        // User1'in öğrenmek istediği yeteneklerin eşleşme oranı
        val user1WantedMatches = user1.wantedSkills.count { wantedSkill ->
            user2.knownSkills.any { knownSkill ->
                isSkillMatch(wantedSkill, knownSkill)
            }
        }
        score += (user1WantedMatches * 100) / user1.wantedSkills.size
        
        // User2'nin öğrenmek istediği yeteneklerin eşleşme oranı
        val user2WantedMatches = user2.wantedSkills.count { wantedSkill ->
            user1.knownSkills.any { knownSkill ->
                isSkillMatch(wantedSkill, knownSkill)
            }
        }
        score += (user2WantedMatches * 100) / user2.wantedSkills.size
        
        // Ortalama skor
        return score / 2
    }
}
