package com.parthenios.skillmatch.ui.explore

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parthenios.skillmatch.data.User
import com.parthenios.skillmatch.databinding.ItemUserCardBinding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class UserCardAdapter(
    private val onUserClick: (User) -> Unit,
    private val onSwipeLeft: (User) -> Unit,
    private val onSwipeRight: (User) -> Unit,
    private val onSwipeDown: (User) -> Unit
) : ListAdapter<User, UserCardAdapter.UserCardViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserCardViewHolder {
        val binding = ItemUserCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserCardViewHolder(
        private val binding: ItemUserCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var startX = 0f
        private var startY = 0f
        private var isSwipeHandled = false
        private var currentX = 0f
        private var currentY = 0f
        private var isDragging = false
        private var swipeThreshold = 0f
        private var screenWidth = 0f

        fun bind(user: User) {
            binding.apply {
                tvName.text = "${user.firstName} ${user.lastName}"
                tvUsername.text = "@${user.username}"
                tvCity.text = user.city
                
                // Bilinen yetenekleri göster
                val knownSkillsText = user.knownSkills.joinToString(", ")
                tvKnownSkills.text = knownSkillsText
                
                // Öğrenmek istediği yetenekleri göster
                val wantedSkillsText = user.wantedSkills.joinToString(", ")
                tvWantedSkills.text = wantedSkillsText
                
                // Yaş bilgisini direkt göster
                val age = if (user.age > 0) user.age else calculateAge(user.birthday)
                tvAge.text = "$age yaşında"
                
                // Screen width'i al
                screenWidth = root.context.resources.displayMetrics.widthPixels.toFloat()
                swipeThreshold = screenWidth * 0.25f // %25 threshold
                
                // Reset card position
                resetCardPosition()
                
                // Swipe gesture'ları için touch listener
                root.setOnTouchListener { view, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            currentX = root.translationX
                            currentY = root.translationY
                            isSwipeHandled = false
                            isDragging = false
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!isSwipeHandled) {
                                val deltaX = event.rawX - startX
                                val deltaY = event.rawY - startY
                                
                                // Minimum hareket miktarı kontrolü
                                if (abs(deltaX) > 15 || abs(deltaY) > 15) {
                                    isDragging = true
                                }
                                
                                if (isDragging) {
                                    // Kartı hareket ettir
                                    root.translationX = deltaX
                                    root.translationY = deltaY * 0.3f // Dikey hareketi sınırla
                                    
                                    // Bumble tarzı rotasyon efekti - daha az eğilme
                                    val rotation = deltaX * 0.03f
                                    root.rotation = rotation
                                    
                                    // Alpha efekti
                                    val alpha = 1f - (abs(deltaX) / (screenWidth * 0.6f))
                                    root.alpha = alpha.coerceIn(0.3f, 1f)
                                    
                                    // Swipe direction indicator
                                    updateSwipeIndicator(deltaX)
                                }
                            }
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                            
                            if (!isSwipeHandled && isDragging) {
                                val deltaX = event.rawX - startX
                                val deltaY = event.rawY - startY
                                
                                // Aşağı kaydırma kontrolü (öncelikli)
                                if (deltaY > 150 && abs(deltaY) > abs(deltaX)) {
                                    // Aşağı kaydırma - kullanıcı bilgilerini göster
                                    onSwipeDown(user)
                                    isSwipeHandled = true
                                }
                                // Yatay swipe kontrolü
                                else if (abs(deltaX) > swipeThreshold && abs(deltaX) > abs(deltaY)) {
                                    if (deltaX > 0) {
                                        // Sağa kaydırma - eşleşme isteği gönder
                                        animateSwipeOut(1f) {
                                            onSwipeRight(user)
                                        }
                                    } else {
                                        // Sola kaydırma - kullanıcıyı reddet
                                        animateSwipeOut(-1f) {
                                            onSwipeLeft(user)
                                        }
                                    }
                                    isSwipeHandled = true
                                } else {
                                    // Geri döndür
                                    animateBackToCenter()
                                }
                            } else if (!isDragging) {
                                // Tıklama - profil sayfasına git
                                onUserClick(user)
                            }
                            
                            // Reset swipe indicator
                            hideSwipeIndicator()
                            true
                        }
                        else -> false
                    }
                }
            }
        }
        
        private fun updateSwipeIndicator(deltaX: Float) {
            val progress = abs(deltaX) / swipeThreshold
            val alpha = min(progress, 1f)
            
            if (deltaX > 0) {
                // Sağa kaydırma - yeşil tick indicator
                showSwipeIndicator("TICK", 0xFF4CAF50.toInt(), alpha)
            } else if (deltaX < 0) {
                // Sola kaydırma - kırmızı çarpı indicator
                showSwipeIndicator("CROSS", 0xFFF44336.toInt(), alpha)
            }
        }
        
        private fun showSwipeIndicator(text: String, color: Int, alpha: Float) {
            if (text == "CROSS") {
                binding.tvSwipeLeft.alpha = alpha
            } else if (text == "TICK") {
                binding.tvSwipeRight.alpha = alpha
            }
        }
        
        private fun hideSwipeIndicator() {
            binding.tvSwipeLeft.alpha = 0f
            binding.tvSwipeRight.alpha = 0f
        }
        
        private fun resetCardPosition() {
            binding.root.translationX = 0f
            binding.root.translationY = 0f
            binding.root.rotation = 0f
            binding.root.alpha = 1f
            binding.root.scaleX = 1f
            binding.root.scaleY = 1f
        }
        
        private fun animateSwipeOut(direction: Float, onComplete: () -> Unit = {}) {
            val targetX = direction * screenWidth * 1.2f
            val targetRotation = direction * 8f
            
            // X translation animation
            ObjectAnimator.ofFloat(binding.root, "translationX", targetX).apply {
                duration = 250
                interpolator = DecelerateInterpolator(1.5f)
                start()
            }
            
            // Rotation animation
            ObjectAnimator.ofFloat(binding.root, "rotation", targetRotation).apply {
                duration = 250
                interpolator = DecelerateInterpolator(1.5f)
                start()
            }
            
            // Alpha animation
            ObjectAnimator.ofFloat(binding.root, "alpha", 0f).apply {
                duration = 200
                start()
            }
            
            // Scale animation for Bumble effect
            ObjectAnimator.ofFloat(binding.root, "scaleX", 0.8f).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
                start()
            }
            
            ObjectAnimator.ofFloat(binding.root, "scaleY", 0.8f).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
                start()
            }
            
            // Animation tamamlandığında callback'i çağır
            binding.root.postDelayed({
                onComplete()
            }, 250)
        }
        
        private fun animateBackToCenter() {
            // Bumble tarzı geri dönüş animasyonu
            val animatorSet = android.animation.AnimatorSet()
            
            val translationXAnim = ObjectAnimator.ofFloat(binding.root, "translationX", 0f)
            val translationYAnim = ObjectAnimator.ofFloat(binding.root, "translationY", 0f)
            val rotationAnim = ObjectAnimator.ofFloat(binding.root, "rotation", 0f)
            val alphaAnim = ObjectAnimator.ofFloat(binding.root, "alpha", 1f)
            val scaleXAnim = ObjectAnimator.ofFloat(binding.root, "scaleX", 1f)
            val scaleYAnim = ObjectAnimator.ofFloat(binding.root, "scaleY", 1f)
            
            // Overshoot interpolator for bouncy effect
            val overshootInterpolator = OvershootInterpolator(1.2f)
            
            translationXAnim.interpolator = overshootInterpolator
            translationYAnim.interpolator = overshootInterpolator
            rotationAnim.interpolator = overshootInterpolator
            scaleXAnim.interpolator = overshootInterpolator
            scaleYAnim.interpolator = overshootInterpolator
            
            alphaAnim.interpolator = DecelerateInterpolator()
            
            translationXAnim.duration = 300
            translationYAnim.duration = 300
            rotationAnim.duration = 300
            alphaAnim.duration = 200
            scaleXAnim.duration = 300
            scaleYAnim.duration = 300
            
            animatorSet.playTogether(
                translationXAnim,
                translationYAnim,
                rotationAnim,
                alphaAnim,
                scaleXAnim,
                scaleYAnim
            )
            
            animatorSet.start()
        }
        
        private fun calculateAge(birthday: String): Int {
            return try {
                val birthYear = birthday.split("-")[0].toInt()
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                currentYear - birthYear
            } catch (e: Exception) {
                0
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
