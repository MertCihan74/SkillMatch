# SkillMatch

## 🎯 Purpose

SkillMatch is a skill exchange platform that connects people who want to learn from each other. Users can share their existing skills and express what they want to learn, creating a community where knowledge flows freely.

**How it works:**
- You register with skills you know (e.g., "Guitar") and skills you want to learn (e.g., "Programming")
- The app matches you with others who have complementary skills
- For example: You know guitar and want to learn programming, while someone else knows programming and wants to learn guitar
- You connect, exchange knowledge, and both benefit from free learning while building social connections

This creates a win-win ecosystem where everyone can learn new skills without cost while expanding their social network.

---

## 🚀 Sprint 1

### ✅ Completed Features

**Authentication System**
- Email/Password registration and login
- Google Sign-In integration
- Secure password validation
- Email validation with domain checking

**User Profile Creation**
- 6-step registration process:
  1. Email & Password
  2. Personal Info (name, username)
  3. Birthday (with age calculation)
  4. City selection (81 Turkish cities)
  5. Known Skills (chip-based input)
  6. Wanted Skills (chip-based input)

**Technical Implementation**
- Firebase Authentication + Firestore
- Material Design UI with Turkish language support
- Form validation and error handling
- Kotlin Coroutines for async operations

### 🔧 Tech Stack
- **Backend**: Firebase (Auth + Firestore)
- **Frontend**: Android (Kotlin)
- **Architecture**: Repository Pattern
- **UI**: Material Design Components

---

**Sprint 1 Complete** ✅  
*User authentication and profile creation system ready for skill matching.*

## 🚀 Sprint 2 - Planned Features

### 🏠 Core App Screens
- **Home Page**: Main dashboard with user overview
- **Profile Page**: User profile viewing and editing
- **Discovery/Matching Screen**: Browse and find skill matches

### 🤖 Matching System
- **Basic Matching Algorithm**: Connect users with complementary skills
- Smart skill-based pairing system

### 💬 Communication
- **Chat UI**: Real-time messaging between matched users
- **End-to-End Encryption**: Secure communication

### 🛡️ Content Moderation
- **Inappropriate Content Moderation**: AI-powered content filtering
- **Moderation Pipeline**: Automated and manual review system

---

*Sprint 2 will focus on core user interactions and matching functionality.*
