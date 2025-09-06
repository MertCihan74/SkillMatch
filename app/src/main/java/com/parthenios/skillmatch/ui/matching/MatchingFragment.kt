package com.parthenios.skillmatch.ui.matching

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.parthenios.skillmatch.R
import com.parthenios.skillmatch.data.MatchRequest
import com.parthenios.skillmatch.data.MatchRequestStatus
import com.parthenios.skillmatch.data.Match
import com.parthenios.skillmatch.data.MatchStatus
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.ui.chat.ChatActivity
import com.parthenios.skillmatch.databinding.FragmentMatchingBinding
import com.parthenios.skillmatch.utils.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class MatchingFragment : Fragment() {
    private var _binding: FragmentMatchingBinding? = null
    private val binding get() = _binding!!
    private lateinit var userPreferences: UserPreferences
    private lateinit var firestore: FirebaseFirestore
    private var currentUser: User? = null
    private val receivedRequests = mutableListOf<MatchRequest>()
    private lateinit var receivedRequestsAdapter: MatchRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userPreferences = UserPreferences(requireContext())
        firestore = FirebaseFirestore.getInstance()
        currentUser = userPreferences.getUser()
        setupUI()
        loadMatchRequests()
    }

    private fun setupUI() {
        binding.apply {
            tvTitle.text = "Eşleşme İstekleri"
            tvSubtitle.text = "Size gelen eşleşme isteklerini burada görebilirsiniz"
            
            // Sadece alınan istekler için RecyclerView ayarla
            setupReceivedRequestsRecyclerView()
        }
    }
    
    private fun setupReceivedRequestsRecyclerView() {
        // Sadece alınan istekler - butonlar aktif
        receivedRequestsAdapter = MatchRequestAdapter(
            onAcceptClick = { request -> 
                // Kabul etme işlemi
                acceptMatchRequest(request)
            },
            onRejectClick = { request -> 
                // Reddetme işlemi
                rejectMatchRequest(request)
            },
            isSentRequests = false // Alınan istekler
        )
        
        binding.recyclerViewReceivedRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = receivedRequestsAdapter
        }
    }

    private fun loadMatchRequests() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Kullanıcı bilgileri bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Sadece alınan istekleri yükle - sadece beklemede olanlar
                val receivedQuery = firestore.collection("matchRequests")
                    .whereEqualTo("toUserId", currentUser!!.uid)
                    .whereEqualTo("status", MatchRequestStatus.PENDING.name)
                    .get().await()
                
                val receivedRequestsList = receivedQuery.documents.mapNotNull { document ->
                    try {
                        document.toObject(MatchRequest::class.java)?.copy(id = document.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                withContext(Dispatchers.Main) {
                    receivedRequests.clear()
                    receivedRequests.addAll(receivedRequestsList)
                    receivedRequestsAdapter.submitList(receivedRequests)
                    
                    // UI güncelleme
                    updateUI()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Eşleşme istekleri yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateUI() {
        binding.apply {
            if (receivedRequests.isEmpty()) {
                tvNoMatches.text = "Henüz eşleşme isteğiniz bulunmuyor. Keşfet sayfasından kullanıcıları inceleyin!"
                tvNoMatches.visibility = View.VISIBLE
            } else {
                tvNoMatches.visibility = View.GONE
            }
        }
    }
    
    private fun acceptMatchRequest(request: MatchRequest) {
        println("DEBUG: Accepting request ${request.id}")
        // Anlık olarak UI'den kaldır
        removeRequestFromUI(request)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // İsteği kabul et
                firestore.collection("matchRequests").document(request.id)
                    .update("status", MatchRequestStatus.ACCEPTED.name)
                    .await()
                
                // Eşleşme oluştur
                createMatch(request)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Eşleşme isteği kabul edildi! 🎉", Toast.LENGTH_SHORT).show()
                    // Chat'e yönlendir
                    redirectToChat(request)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hata durumunda eski duruma geri döndür
                    receivedRequests.add(request)
                    receivedRequestsAdapter.submitList(receivedRequests)
                    updateUI()
                    Toast.makeText(requireContext(), "İstek kabul edilirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun rejectMatchRequest(request: MatchRequest) {
        // Anlık olarak UI'den kaldır
        removeRequestFromUI(request)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // İsteği reddet
                firestore.collection("matchRequests").document(request.id)
                    .update("status", MatchRequestStatus.REJECTED.name)
                    .await()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Eşleşme isteği reddedildi", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hata durumunda eski duruma geri döndür
                    receivedRequests.add(request)
                    receivedRequestsAdapter.submitList(receivedRequests)
                    Toast.makeText(requireContext(), "İstek reddedilirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateRequestStatusInUI(request: MatchRequest, newStatus: MatchRequestStatus) {
        // Kabul edilen istekler için UI'yi güncelle (chat'e yönlendirilecek)
        val updatedRequest = request.copy(status = newStatus)
        val index = receivedRequests.indexOfFirst { it.id == request.id }
        
        if (index != -1) {
            // Listeyi güncelle
            receivedRequests[index] = updatedRequest
            
            // Adapter'ı güncelle - notifyItemChanged kullan
            receivedRequestsAdapter.notifyItemChanged(index)
            
            // Debug için log
            println("DEBUG: Request ${request.id} status updated to ${newStatus.name}")
            println("DEBUG: Item at index $index updated")
            println("DEBUG: Updated request status: ${updatedRequest.status}")
        } else {
            println("DEBUG: Request ${request.id} not found in received requests")
            println("DEBUG: Available request IDs: ${receivedRequests.map { it.id }}")
        }
    }
    
    private fun removeRequestFromUI(request: MatchRequest) {
        val index = receivedRequests.indexOfFirst { it.id == request.id }
        
        if (index != -1) {
            // Listeden kaldır
            receivedRequests.removeAt(index)
            
            // Adapter'ı güncelle - notifyItemRemoved kullan
            receivedRequestsAdapter.notifyItemRemoved(index)
            
            // UI'yi güncelle
            updateUI()
            
            println("DEBUG: Request ${request.id} removed from UI")
        } else {
            println("DEBUG: Request ${request.id} not found in received requests")
        }
    }
    
    private suspend fun createMatch(request: MatchRequest) {
        try {
            // Diğer kullanıcının bilgilerini al
            val otherUserDoc = firestore.collection("users")
                .document(request.fromUserId)
                .get().await()
            
            val otherUser = otherUserDoc.toObject(User::class.java)
            if (otherUser == null) {
                throw Exception("Diğer kullanıcı bulunamadı")
            }
            
            // Eşleşme oluştur
            val matchData = hashMapOf(
                "user1Id" to currentUser!!.uid,
                "user2Id" to request.fromUserId,
                "user1Name" to "${currentUser!!.firstName} ${currentUser!!.lastName}",
                "user2Name" to "${otherUser.firstName} ${otherUser.lastName}",
                "status" to MatchStatus.ACTIVE.name,
                "createdAt" to java.util.Date(),
                "expiresAt" to java.util.Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000) // 24 saat
            )
            
            val matchRef = firestore.collection("matches").add(matchData).await()
            
            // Match ID'yi request'e kaydet (opsiyonel)
            firestore.collection("matchRequests").document(request.id)
                .update("matchId", matchRef.id)
                .await()
                
        } catch (e: Exception) {
            throw Exception("Eşleşme oluşturulurken hata: ${e.message}")
        }
    }
    
    private fun redirectToChat(request: MatchRequest) {
        try {
            // Diğer kullanıcının bilgilerini al (basit versiyon)
            val otherUser = User(
                uid = request.fromUserId,
                firstName = request.fromUserName.split(" ").firstOrNull() ?: "Kullanıcı",
                lastName = request.fromUserName.split(" ").drop(1).joinToString(" ") ?: "",
                email = "",
                age = 0,
                city = "",
                knownSkills = emptyList(),
                wantedSkills = emptyList()
            )
            
            val intent = android.content.Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("otherUser", otherUser)
            intent.putExtra("matchId", "") // Boş matchId - ChatFragment'te oluşturulacak
            startActivity(intent)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Chat'e yönlendirilirken hata: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
