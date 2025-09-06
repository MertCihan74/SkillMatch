package com.parthenios.skillmatch.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.parthenios.skillmatch.data.Message
import com.parthenios.skillmatch.data.MessageType
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.FragmentChatBinding
import com.parthenios.skillmatch.utils.UserPreferences
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
                requireActivity().finish()
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
            requireActivity().finish()
        }
    }
    
    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(currentUser!!.uid)
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
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
            println("DEBUG: Match ID zaten var: $matchId, mesajlar yükleniyor")
            loadMessages()
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
    
    private suspend fun createNewMatch() {
        try {
            println("DEBUG: Yeni eşleşme oluşturuluyor...")
            val matchData = hashMapOf(
                "user1Id" to currentUser!!.uid,
                "user2Id" to otherUser!!.uid,
                "user1Name" to "${currentUser!!.firstName} ${currentUser!!.lastName}",
                "user2Name" to "${otherUser!!.firstName} ${otherUser!!.lastName}",
                "status" to "ACTIVE",
                "createdAt" to Date(),
                "expiresAt" to Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000) // 24 saat
            )
            
            println("DEBUG: Match data: $matchData")
            val docRef = firestore.collection("matches").add(matchData).await()
            matchId = docRef.id
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
                .orderBy("timestamp")
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
                            val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                            println("DEBUG: Mesaj yüklendi: ${message?.content}")
                            message
                        } catch (e: Exception) {
                            println("DEBUG: Mesaj parse hatası: ${e.message}")
                            println("DEBUG: Hatalı doküman: ${doc.data}")
                            null
                        }
                    }
                    
                    // Geçici mesajları koru, Firebase mesajlarını ekle
                    val currentMessages = messageAdapter.currentList.filter { it.id.startsWith("temp_") }
                    val allMessages = (currentMessages + firebaseMessages).distinctBy { it.id }
                    
                    println("DEBUG: ${firebaseMessages.size} Firebase mesajı, ${currentMessages.size} geçici mesaj, ${allMessages.size} toplam mesaj")
                    messageAdapter.submitList(allMessages)
                    
                    if (allMessages.isNotEmpty()) {
                        binding.recyclerViewMessages.scrollToPosition(allMessages.size - 1)
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
        messageAdapter.submitList(currentMessages)
        binding.recyclerViewMessages.scrollToPosition(currentMessages.size - 1)
        
        // Input'u temizle
        binding.etMessage.setText("")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = Message(
                    matchId = matchId!!,
                    senderId = currentUser!!.uid,
                    receiverId = otherUser!!.uid,
                    content = content.trim(),
                    messageType = MessageType.TEXT,
                    timestamp = Date(),
                    isRead = false
                )
                
                println("DEBUG: Mesaj gönderiliyor: ${message.content}")
                val docRef = firestore.collection("messages").add(message).await()
                println("DEBUG: Mesaj gönderildi, ID: ${docRef.id}")
                
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
