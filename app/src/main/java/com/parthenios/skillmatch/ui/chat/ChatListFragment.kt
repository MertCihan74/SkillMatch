package com.parthenios.skillmatch.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.parthenios.skillmatch.data.Match
import com.parthenios.skillmatch.data.MatchStatus
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.FragmentChatListBinding
import com.parthenios.skillmatch.utils.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.Date

class ChatListFragment : Fragment() {
    
    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var firestore: FirebaseFirestore
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var matchListener: ListenerRegistration
    
    private var currentUser: User? = null
    private val activeMatches = mutableListOf<Match>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userPreferences = UserPreferences(requireContext())
        firestore = FirebaseFirestore.getInstance()
        currentUser = userPreferences.getUser()
        
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Kullanıcı bilgileri bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        setupRecyclerView()
        loadActiveMatches()
    }
    
    private fun setupRecyclerView() {
        chatListAdapter = ChatListAdapter(
            onChatClick = { match ->
                // Chat'e git
                openChat(match)
            },
            currentUserId = currentUser!!.uid
        )
        
        binding.recyclerViewChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatListAdapter
        }
    }
    
    private fun loadActiveMatches() {
        if (currentUser == null) return
        
        // Her iki yönlü eşleşmeleri kontrol et
        val query1 = firestore.collection("matches")
            .whereEqualTo("user1Id", currentUser!!.uid)
            .whereEqualTo("status", MatchStatus.ACTIVE.name)
            
        val query2 = firestore.collection("matches")
            .whereEqualTo("user2Id", currentUser!!.uid)
            .whereEqualTo("status", MatchStatus.ACTIVE.name)
        
        // İlk query'yi dinle
        query1.addSnapshotListener { snapshot1, error1 ->
            if (error1 != null) {
                Toast.makeText(requireContext(), "Eşleşmeler yüklenirken hata: ${error1.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            
            // İkinci query'yi de çalıştır
            query2.get().addOnSuccessListener { snapshot2 ->
                val matches1 = snapshot1?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Match::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                val matches2 = snapshot2?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Match::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                val allMatches = matches1 + matches2

                // Karşı kullanıcı silinmişse bu eşleşmeleri temizle ve listeden çıkar
                CoroutineScope(Dispatchers.IO).launch {
                    val firestore = FirebaseFirestore.getInstance()
                    val currentId = currentUser!!.uid
                    val validMatches = mutableListOf<Match>()
                    for (m in allMatches) {
                        val otherId = if (m.user1Id == currentId) m.user2Id else m.user1Id
                        val otherDoc = try { firestore.collection("users").document(otherId).get().await() } catch (e: Exception) { null }
                        if (otherDoc != null && otherDoc.exists()) {
                            validMatches.add(m)
                        } else {
                            // Cleanup: bu eşleşmeye bağlı mesajları ve eşleşmeyi sil
                            try {
                                val msgs = firestore.collection("messages").whereEqualTo("matchId", m.id).get().await()
                                for (d in msgs.documents) {
                                    try { firestore.collection("messages").document(d.id).delete().await() } catch (_: Exception) {}
                                }
                                try { firestore.collection("matches").document(m.id).delete().await() } catch (_: Exception) {}
                            } catch (_: Exception) {}
                        }
                    }

                    withContext(Dispatchers.Main) {
                        activeMatches.clear()
                        activeMatches.addAll(validMatches)
                        chatListAdapter.submitList(validMatches)
                        updateUI()
                    }
                }
            }.addOnFailureListener { error2 ->
                Toast.makeText(requireContext(), "Eşleşmeler yüklenirken hata: ${error2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateUI() {
        if (activeMatches.isEmpty()) {
            binding.tvNoChats.visibility = View.VISIBLE
            binding.recyclerViewChats.visibility = View.GONE
        } else {
            binding.tvNoChats.visibility = View.GONE
            binding.recyclerViewChats.visibility = View.VISIBLE
        }
    }
    
    private fun openChat(match: Match) {
        // Diğer kullanıcının bilgilerini al
        val otherUserId = if (match.user1Id == currentUser!!.uid) {
            match.user2Id
        } else {
            match.user1Id
        }
        
        val otherUserName = if (match.user1Id == currentUser!!.uid) {
            match.user2Name
        } else {
            match.user1Name
        }
        
        // Geçici User objesi oluştur (sadece chat için)
        val otherUser = User(
            uid = otherUserId,
            firstName = otherUserName.split(" ").firstOrNull() ?: "",
            lastName = otherUserName.split(" ").drop(1).joinToString(" ") ?: "",
            email = "",
            age = 0,
            city = "",
            knownSkills = emptyList(),
            wantedSkills = emptyList()
        )
        
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("otherUser", otherUser)
        intent.putExtra("matchId", match.id)
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        if (::matchListener.isInitialized) {
            matchListener.remove()
        }
        _binding = null
    }
}
