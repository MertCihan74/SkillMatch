package com.parthenios.skillmatch.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.parthenios.skillmatch.data.Message
import com.parthenios.skillmatch.data.MessageType
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.FragmentChatBinding
import com.parthenios.skillmatch.utils.UserPreferences
import com.parthenios.skillmatch.utils.AesGcm
import com.parthenios.skillmatch.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Date

class ChatFragment : Fragment() {
    
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var firestore: FirebaseFirestore
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageListener: ListenerRegistration
    
    private var currentUser: User? = null
    private var otherUser: User? = null
    private var matchId: String? = null
    private var chatKey: String? = null
    private var lastMessageCount: Int = 0
    
    companion object {
        private const val ARG_OTHER_USER = "otherUser"
        private const val ARG_MATCH_ID = "matchId"
        
        fun newInstance(otherUser: User, matchId: String? = null): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putSerializable(ARG_OTHER_USER, otherUser)
            args.putString(ARG_MATCH_ID, matchId)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            userPreferences = UserPreferences(requireContext())
            firestore = FirebaseFirestore.getInstance()
            currentUser = userPreferences.getUser()
            otherUser = arguments?.getSerializable(ARG_OTHER_USER) as? User
            matchId = arguments?.getString(ARG_MATCH_ID)
            
            if (currentUser == null || otherUser == null) {
                Toast.makeText(requireContext(), "Kullanıcı bilgileri bulunamadı", Toast.LENGTH_SHORT).show()
                return
            }
            
            setupRecyclerView()
            setupSendButton()
            loadOrCreateMatch()
            
            // Geçici olarak güvenlik kontrolünü devre dışı bırak
            // verifyMatchSecurity()
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Chat yüklenirken hata: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(currentUser!!.uid)
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
            // Animasyon kaynaklı "yenilenme" hissini azaltmak için değişim animasyonlarını kapat
            itemAnimator = null
        }

        // Yeni mesajlar eklendiğinde otomatik olarak en alta kaydır
        messageAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.recyclerViewMessages.post {
                    val total = messageAdapter.itemCount
                    if (total > 0) {
                        binding.recyclerViewMessages.smoothScrollToPosition(total - 1)
                    }
                }
            }
        })
    }
    
    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty() && matchId != null) {
                sendMessage(messageText)
                binding.etMessage.text?.clear()
            }
        }
    }
    
    private fun loadOrCreateMatch() {
        if (matchId != null) {
            println("DEBUG: Match ID zaten var: $matchId, chatKey yükleniyor ve mesajlar hazırlanıyor")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Katılımcıları doğrula; diğer kullanıcı silinmişse eşleşme ve mesajları temizle
                    val participantsOk = validateParticipantsOrCleanup()
                    if (!participantsOk) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Karşı kullanıcı silinmiş. Sohbet kaldırıldı.", Toast.LENGTH_LONG).show()
                            requireActivity().onBackPressed()
                        }
                        return@launch
                    }
                    val doc = firestore.collection("matches").document(matchId!!).get().await()
                    var existingKey = doc.getString("chatKey")
                    if (existingKey.isNullOrEmpty()) {
                        println("DEBUG: chatKey eksik (mevcut eşleşme), oluşturuluyor...")
                        existingKey = AesGcm.generateKeyB64()
                        firestore.collection("matches").document(matchId!!)
                            .update("chatKey", existingKey)
                            .await()
                        println("DEBUG: chatKey oluşturuldu ve kaydedildi (mevcut eşleşme)")
                    }
                    chatKey = existingKey
                    withContext(Dispatchers.Main) { loadMessages() }
                } catch (e: Exception) {
                    println("DEBUG: Mevcut eşleşme için chatKey yükleme hatası: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Sohbet anahtarı yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            return
        }
        
        println("DEBUG: Match ID yok, eşleşme aranıyor...")
        println("DEBUG: Current user: ${currentUser?.uid}")
        println("DEBUG: Other user: ${otherUser?.uid}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Mevcut eşleşmeyi kontrol et
                println("DEBUG: Eşleşme sorgusu başlatılıyor...")
                val matchQuery = firestore.collection("matches")
                    .whereEqualTo("user1Id", currentUser!!.uid)
                    .whereEqualTo("user2Id", otherUser!!.uid)
                    .whereEqualTo("status", "ACTIVE")
                    .get().await()
                
                val reverseMatchQuery = firestore.collection("matches")
                    .whereEqualTo("user1Id", otherUser!!.uid)
                    .whereEqualTo("user2Id", currentUser!!.uid)
                    .whereEqualTo("status", "ACTIVE")
                    .get().await()
                
                val allMatches = matchQuery.documents + reverseMatchQuery.documents
                println("DEBUG: ${allMatches.size} eşleşme bulundu")
                
                if (allMatches.isEmpty()) {
                    println("DEBUG: Eşleşme bulunamadı, yeni eşleşme oluşturuluyor...")
                    // Yeni eşleşme oluştur
                    createNewMatch()
                } else {
                    println("DEBUG: Mevcut eşleşme bulundu: ${allMatches.first().id}")
                    // Mevcut eşleşmeyi kullan
                    val matchDoc = allMatches.first()
                    matchId = matchDoc.id
                    // Sohbet anahtarını al/yoksa oluştur
                    var existingKey = matchDoc.getString("chatKey")
                    if (existingKey.isNullOrEmpty()) {
                        println("DEBUG: chatKey eksik, oluşturuluyor...")
                        existingKey = AesGcm.generateKeyB64()
                        firestore.collection("matches").document(matchId!!)
                            .update("chatKey", existingKey)
                            .await()
                        println("DEBUG: chatKey oluşturuldu ve kaydedildi")
                    }
                    chatKey = existingKey
                    withContext(Dispatchers.Main) {
                        loadMessages()
                    }
                }
                
            } catch (e: Exception) {
                println("DEBUG: Eşleşme yükleme hatası: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Eşleşme yüklenirken hata: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun validateParticipantsOrCleanup(): Boolean {
        return try {
            val otherId = otherUser?.uid ?: return false
            val otherDoc = firestore.collection("users").document(otherId).get().await()
            if (otherDoc.exists()) {
                true
            } else {
                // Diğer kullanıcı silinmiş: bu eşleşmeye ait mesajları ve eşleşmeyi temizle
                val localMatchId = matchId
                if (!localMatchId.isNullOrEmpty()) {
                    // Mesajları sil
                    val messages = firestore.collection("messages").whereEqualTo("matchId", localMatchId).get().await()
                    for (msg in messages.documents) {
                        try { firestore.collection("messages").document(msg.id).delete().await() } catch (_: Exception) {}
                    }
                    // Eşleşmeyi sil
                    try { firestore.collection("matches").document(localMatchId).delete().await() } catch (_: Exception) {}
                }
                // Bu eşleşmeye bağlı istek varsa (opsiyonel) durumunu REJECTED yap
                try {
                    val reqs = firestore.collection("matchRequests")
                        .whereEqualTo("matchId", matchId)
                        .get().await()
                    for (req in reqs.documents) {
                        try { firestore.collection("matchRequests").document(req.id).delete().await() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
                false
            }
        } catch (e: Exception) {
            println("DEBUG: validateParticipantsOrCleanup hata: ${e.message}")
            true
        }
    }
    
    private suspend fun createNewMatch() {
        try {
            println("DEBUG: Yeni eşleşme oluşturuluyor...")
            val generatedChatKey = AesGcm.generateKeyB64()
            val matchData = hashMapOf(
                "user1Id" to currentUser!!.uid,
                "user2Id" to otherUser!!.uid,
                "user1Name" to "${currentUser!!.firstName} ${currentUser!!.lastName}",
                "user2Name" to "${otherUser!!.firstName} ${otherUser!!.lastName}",
                "status" to "ACTIVE",
                "createdAt" to Date(),
                "expiresAt" to Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000), // 24 saat
                "chatKey" to generatedChatKey
            )
            
            println("DEBUG: Match data: $matchData")
            val docRef = firestore.collection("matches").add(matchData).await()
            matchId = docRef.id
            chatKey = generatedChatKey
            println("DEBUG: Yeni eşleşme oluşturuldu, ID: $matchId")
            
            withContext(Dispatchers.Main) {
                loadMessages()
            }
            
        } catch (e: Exception) {
            println("DEBUG: Eşleşme oluşturma hatası: ${e.message}")
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Eşleşme oluşturulurken hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadMessages() {
        if (matchId == null) {
            println("DEBUG: Match ID null, mesajlar yüklenemiyor")
            Toast.makeText(requireContext(), "Match ID bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        println("DEBUG: Mesajlar yükleniyor, Match ID: $matchId")
        
        try {
            messageListener = firestore.collection("messages")
                .whereEqualTo("matchId", matchId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("DEBUG: Firestore hatası: ${error.code} - ${error.message}")
                        println("DEBUG: Hata detayı: ${error.localizedMessage}")
                        Toast.makeText(requireContext(), "Mesajlar yüklenirken hata: ${error.message}", Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }
                    
                    if (snapshot == null) {
                        println("DEBUG: Snapshot null")
                        return@addSnapshotListener
                    }
                    
                    println("DEBUG: ${snapshot.documents.size} mesaj dokümanı bulundu")
                    
                    val firebaseMessages = snapshot.documents.mapNotNull { doc ->
                        try {
                            val raw = doc.toObject(Message::class.java)?.copy(id = doc.id)
                            if (raw == null) return@mapNotNull null
                            val resolved = if (raw.encVersion == 1 && raw.ciphertext != null && raw.nonce != null) {
                                val key = chatKey
                                if (!key.isNullOrEmpty()) {
                                    try {
                                        // Önce AAD olmadan dene, başarısızsa matchId ile dene (geri uyumluluk)
                                        val decrypted = try {
                                            AesGcm.decryptFromB64(key, raw.ciphertext, raw.nonce, null)
                                        } catch (e: Exception) {
                                            AesGcm.decryptFromB64(key, raw.ciphertext, raw.nonce, raw.matchId)
                                        }
                                        raw.copy(content = decrypted)
                                    } catch (e: Exception) {
                                        println("DEBUG: Mesaj çözme hatası: ${e.message}")
                                        raw.copy(content = "[Şifreli mesaj]")
                                    }
                                } else {
                                    raw.copy(content = "[Şifreli mesaj]")
                                }
                            } else {
                                raw
                            }
                            println("DEBUG: Mesaj yüklendi: ${resolved.content}")
                            resolved
                        } catch (e: Exception) {
                            println("DEBUG: Mesaj parse hatası: ${e.message}")
                            println("DEBUG: Hatalı doküman: ${doc.data}")
                            null
                        }
                    }
                    // İstemci tarafında zamana göre sırala (null timestamp en sona)
                    .sortedWith(compareBy<Message> { it.timestamp == null }.thenBy { it.timestamp })
                    
                    // Geçici mesajları koruma: snapshot geldiğinde sadece sunucu mesajlarını göster
                    val allMessages = firebaseMessages

                    println("DEBUG: ${firebaseMessages.size} Firebase mesajı, ${allMessages.size} toplam mesaj")
                    val shouldScroll = allMessages.size > lastMessageCount
                    messageAdapter.submitList(allMessages) {
                        binding.recyclerViewMessages.post {
                            if (shouldScroll && allMessages.isNotEmpty()) {
                                binding.recyclerViewMessages.smoothScrollToPosition(allMessages.size - 1)
                            }
                        }
                        lastMessageCount = allMessages.size
                    }

                    // Yeni bir mesaj geldiyse bildirim göster (karşı taraftan ve app açıkken arka plandayken anlamında basit kontrol)
                    if (shouldScroll) {
                        val last = allMessages.last()
                        if (last.senderId != currentUser?.uid && !last.content.isNullOrEmpty()) {
                            NotificationHelper.showMessageNotification(
                                requireContext(),
                                title = "Yeni mesaj",
                                body = last.content
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            println("DEBUG: Listener oluşturma hatası: ${e.message}")
            e.printStackTrace()
            Toast.makeText(requireContext(), "Mesaj listener oluşturulurken hata: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun sendMessage(content: String) {
        if (matchId == null) {
            Toast.makeText(requireContext(), "Match ID bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (content.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Mesaj boş olamaz", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Mesajı anlık olarak UI'ye ekle (optimistic update)
        val tempMessage = Message(
            id = "temp_${System.currentTimeMillis()}",
            matchId = matchId!!,
            senderId = currentUser!!.uid,
            receiverId = otherUser!!.uid,
            content = content.trim(),
            messageType = MessageType.TEXT,
            timestamp = Date(),
            isRead = false
        )
        
        // Geçici mesajı listeye ekle
        val currentMessages = messageAdapter.currentList.toMutableList()
        currentMessages.add(tempMessage)
        messageAdapter.submitList(currentMessages) {
            binding.recyclerViewMessages.post {
                binding.recyclerViewMessages.smoothScrollToPosition(currentMessages.size - 1)
            }
        }
        
        // Input'u temizle
        binding.etMessage.setText("")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // chatKey hazır değilse Firestore'dan al/oluştur
                val key = ensureChatKey()
                val (cipherB64, ivB64) = if (!key.isNullOrEmpty()) {
                    AesGcm.encryptToB64(key, content.trim(), null)
                } else {
                    null to null
                }

                val message = if (cipherB64 != null && ivB64 != null) {
                    Message(
                        matchId = matchId!!,
                        senderId = currentUser!!.uid,
                        receiverId = otherUser!!.uid,
                        content = "",
                        messageType = MessageType.TEXT,
                        timestamp = Date(),
                        isRead = false,
                        encVersion = 1,
                        ciphertext = cipherB64,
                        nonce = ivB64
                    )
                } else {
                    Message(
                        matchId = matchId!!,
                        senderId = currentUser!!.uid,
                        receiverId = otherUser!!.uid,
                        content = content.trim(),
                        messageType = MessageType.TEXT,
                        timestamp = Date(),
                        isRead = false
                    )
                }
                
                println("DEBUG: Mesaj gönderiliyor: ${message.content}")
                val docRef = firestore.collection("messages").add(message).await()
                println("DEBUG: Mesaj gönderildi, ID: ${docRef.id}")
                // Artık snapshot yalnızca sunucu mesajlarını gösterdiği için burada listeyi yeniden göndermeye gerek yok
                
                // Eşleşmenin son mesaj zamanını güncelle
                firestore.collection("matches").document(matchId!!)
                    .update("lastMessageAt", Date()).await()
                
                println("DEBUG: Match güncellendi")
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hata durumunda geçici mesajı kaldır
                    val updatedMessages = messageAdapter.currentList.filter { it.id != tempMessage.id }
                    messageAdapter.submitList(updatedMessages)
                    
                    Toast.makeText(requireContext(), "Mesaj gönderilirken hata: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun ensureChatKey(): String? {
        if (!chatKey.isNullOrEmpty()) return chatKey
        val localMatchId = matchId ?: return null
        return try {
            val result = firestore.runTransaction { tx ->
                val ref = firestore.collection("matches").document(localMatchId)
                val snapshot = tx.get(ref)
                var key = snapshot.getString("chatKey")
                if (key.isNullOrEmpty()) {
                    key = AesGcm.generateKeyB64()
                    tx.update(ref, mapOf("chatKey" to key))
                }
                key
            }.await()
            chatKey = result
            if (result.isNullOrEmpty()) {
                println("DEBUG: ensureChatKey -> chatKey boş döndü")
            } else {
                println("DEBUG: ensureChatKey -> chatKey hazır")
            }
            result
        } catch (e: Exception) {
            println("DEBUG: ensureChatKey hatası: ${e.message}")
            null
        }
    }
    
    private fun verifyMatchSecurity() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Kullanıcılar arasında aktif eşleşme var mı kontrol et
                val matchQuery = firestore.collection("matches")
                    .whereEqualTo("user1Id", currentUser!!.uid)
                    .whereEqualTo("user2Id", otherUser!!.uid)
                    .whereEqualTo("status", "ACTIVE")
                    .get().await()
                
                val reverseMatchQuery = firestore.collection("matches")
                    .whereEqualTo("user1Id", otherUser!!.uid)
                    .whereEqualTo("user2Id", currentUser!!.uid)
                    .whereEqualTo("status", "ACTIVE")
                    .get().await()
                
                val allMatches = matchQuery.documents + reverseMatchQuery.documents
                
                if (allMatches.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Bu kullanıcı ile aktif eşleşmeniz bulunmuyor", Toast.LENGTH_LONG).show()
                        requireActivity().finish()
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Güvenlik kontrolü hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun isAuthorizedToChat(): Boolean {
        // Basit güvenlik kontrolü - gerçek uygulamada daha kapsamlı olmalı
        return currentUser != null && otherUser != null && matchId != null
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        if (::messageListener.isInitialized) {
            messageListener.remove()
        }
        _binding = null
    }
}
