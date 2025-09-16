package com.parthenios.skillmatch.ui.chat

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityChatBinding
    private var otherUser: User? = null
    private var matchId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityChatBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Diğer kullanıcı bilgisini al
            otherUser = intent.getSerializableExtra("otherUser") as? User
            matchId = intent.getStringExtra("matchId")
            
            if (otherUser != null) {
                setupToolbar()
                showChatFragment()
            } else {
                Toast.makeText(this, "Kullanıcı bilgileri bulunamadı", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Chat açılırken hata: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = otherUser?.firstName + " " + otherUser?.lastName
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun showChatFragment() {
        try {
            val chatFragment = ChatFragment.newInstance(otherUser!!, matchId)
            replaceFragment(chatFragment)
        } catch (e: Exception) {
            Toast.makeText(this, "Chat fragment oluşturulurken hata: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
