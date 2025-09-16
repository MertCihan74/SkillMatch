package com.parthenios.skillmatch.ui.explore

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.parthenios.skillmatch.R
import com.parthenios.skillmatch.data.MatchRequest
import com.parthenios.skillmatch.data.MatchRequestStatus
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.FragmentExploreBinding
import com.parthenios.skillmatch.ui.profile.ProfileActivity
import com.parthenios.skillmatch.utils.MatchAlgorithm
import com.parthenios.skillmatch.utils.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class ExploreFragment : Fragment() {
    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!
    private lateinit var userAdapter: UserCardAdapter
    private val users = mutableListOf<User>()
    private lateinit var userPreferences: UserPreferences
    private val allUsers = mutableListOf<User>() // Tüm kullanıcılar
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userPreferences = UserPreferences(requireContext())
        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        loadUsersFromFirebase()
    }

    private fun setupRecyclerView() {
        userAdapter = UserCardAdapter(
            onUserClick = { user ->
                // Kullanıcı kartına tıklandığında profil sayfasına git
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                intent.putExtra("user", user)
                startActivity(intent)
            },
            onSwipeLeft = { user ->
                // Sola kaydırma - kullanıcıyı reddet
                onUserSwipedLeft(user)
            },
            onSwipeRight = { user ->
                // Sağa kaydırma - eşleşme isteği gönder
                onUserSwipedRight(user)
            },
            onSwipeDown = { user ->
                // Aşağı kaydırma - kullanıcı bilgilerini göster
                onUserSwipedDown(user)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = userAdapter
            
            // PagerSnapHelper ekle - kartlar arasında kaydırma
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)
            
            // Touch handling için
            isNestedScrollingEnabled = false
        }
    }
    
    private fun onUserSwipedLeft(user: User) {
        // Kullanıcıyı reddetti - listeden çıkar
        users.remove(user)
        userAdapter.notifyDataSetChanged()
        
        // Eğer kart kalmadıysa yeni eşleşmeler yükle
        if (users.isEmpty()) {
            loadUsersFromFirebase()
        }
    }
    
    private fun onUserSwipedRight(user: User) {
        // Kullanıcıyı beğendi - eşleşme isteği gönder
        val currentUser = userPreferences.getUser()
        if (currentUser != null) {
            val matchScore = MatchAlgorithm.calculateMatchScore(currentUser, user)
            sendMatchRequest(currentUser, user)
            Toast.makeText(requireContext(), "Eşleşme isteği ${user.firstName} ${user.lastName}'a gönderildi! 💕 (Eşleşme: %$matchScore)", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "Eşleşme isteği ${user.firstName} ${user.lastName}'a gönderildi! 💕", Toast.LENGTH_SHORT).show()
        }
        
        // Kullanıcıyı listeden çıkar
        users.remove(user)
        userAdapter.notifyDataSetChanged()
        
        // Eğer kart kalmadıysa yeni eşleşmeler yükle
        if (users.isEmpty()) {
            loadUsersFromFirebase()
        }
    }
    
    private fun sendMatchRequest(fromUser: User, toUser: User) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Yalnızca PENDING durumdaki aynı hedefe gönderilmiş istek var mı? Varsa sessizce çık
                val existingPending = firestore.collection("matchRequests")
                    .whereEqualTo("fromUserId", fromUser.uid)
                    .whereEqualTo("toUserId", toUser.uid)
                    .whereEqualTo("status", MatchRequestStatus.PENDING.name)
                    .get().await()
                if (!existingPending.isEmpty) return@launch

                val matchRequest = MatchRequest(
                    fromUserId = fromUser.uid,
                    toUserId = toUser.uid,
                    fromUserName = "${fromUser.firstName} ${fromUser.lastName}",
                    toUserName = "${toUser.firstName} ${toUser.lastName}",
                    status = MatchRequestStatus.PENDING
                )
                
                firestore.collection("matchRequests").add(matchRequest).await()
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Eşleşme isteği gönderilirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun onUserSwipedDown(user: User) {
        // Aşağı kaydırma - kullanıcı bilgilerini göster
        Toast.makeText(requireContext(), "${user.firstName} ${user.lastName} - ${user.city}, ${user.age} yaşında", Toast.LENGTH_LONG).show()
    }

    private fun loadUsersFromFirebase() {
        val currentUser = userPreferences.getUser()
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Kullanıcı bilgileri bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Firestore'dan tüm kullanıcıları çek
                val result = firestore.collection("users").get().await()
                
                val firebaseUsers = result.documents.mapNotNull { document ->
                    try {
                        val user = document.toObject(User::class.java)
                        user?.copy(uid = document.id) // UID'yi document ID'den al
                    } catch (e: Exception) {
                        null
                    }
                }.filter { user ->
                    // Mevcut kullanıcıyı hariç tut
                    user.uid != currentUser.uid
                }
                
                // currentUser'ın PENDING gönderdiği isteklerin hedeflerini çek (sadece bekleyenleri gizle)
                val pendingSent = firestore.collection("matchRequests")
                    .whereEqualTo("fromUserId", currentUser.uid)
                    .whereEqualTo("status", MatchRequestStatus.PENDING.name)
                    .get().await()
                val hiddenToUserIds = pendingSent.documents.mapNotNull { it.getString("toUserId") }.toSet()

                // currentUser'ın daha önce eşleştiği kullanıcıları çıkar
                val matches1 = firestore.collection("matches")
                    .whereEqualTo("user1Id", currentUser.uid)
                    .get().await()
                val matches2 = firestore.collection("matches")
                    .whereEqualTo("user2Id", currentUser.uid)
                    .get().await()
                val previouslyMatchedIds = buildSet {
                    matches1.documents.forEach { doc ->
                        doc.getString("user2Id")?.let { add(it) }
                    }
                    matches2.documents.forEach { doc ->
                        doc.getString("user1Id")?.let { add(it) }
                    }
                }

                withContext(Dispatchers.Main) {
                    allUsers.clear()
                    allUsers.addAll(firebaseUsers)

                    // Eşleşme algoritması ile potansiyel eşleşmeleri bul ve PENDING gönderilmişleri hariç tut
                    val potentialMatches = MatchAlgorithm.findPotentialMatches(currentUser, allUsers)
                        .filter { candidate ->
                            candidate.uid !in hiddenToUserIds && candidate.uid !in previouslyMatchedIds
                        }

                    users.clear()
                    users.addAll(potentialMatches)
                    userAdapter.submitList(users)

                    if (users.isEmpty()) {
                        Toast.makeText(requireContext(), "Henüz eşleşme bulunamadı. Profilinizi güncelleyin!", Toast.LENGTH_LONG).show()
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Kullanıcılar yüklenirken hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
