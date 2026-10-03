RemindMe - Smart Attendance & Agency Reminder System
RemindMe is a modern Android application that has evolved from a local reminder utility into a robust, online agency attendance system. Integrated with a backend server, it features real-time synchronization, advanced anti-manipulation security, and intelligent scheduling to streamline daily institutional operations.
---
📺 Application Demo Video
![Watch Demo Video](https://img.shields.io/badge/YouTube-Demo%20Video-red?style=for-the-badge&logo=youtube)
🚀 Key Features
🏛️ Online Attendance & Backend Integration
REST API Integration (Retrofit & OkHttp): Seamlessly connects to the backend server for fetching active meeting topics (`GET`) and submitting attendance records (`POST`).
Digital Signature & PIN Security: Captures user signatures in Base64 format, protected by mandatory Admin PIN verification.
Specialized Roll Call (Apel) Sessions: Supports dedicated morning and afternoon roll call submissions with custom session parameters and Ngrok compatibility headers.
🔐 Security & Anti-Manipulation
Strict Date Locking (`isToday`): Prevents users from checking in or signing for past or future dates; forms automatically lock when the date does not match today.
7-Day Identity Protection: Restricts users from changing their full name and job title more than once every 7 days to eliminate "proxy attendance" (joki absen) practices.
Isolated Digital Canvas: Enforces strict view boundary clipping (`.clipToBounds()`) for digital signatures, rendering clean PNG Bitmaps before encoding.
⏰ Smart Alarm & Scheduling System
Dynamic Office Alarms: Automatically adjusts alarm schedules based on institutional office hours (Monday morning roll call, Tuesday–Friday mornings, and afternoon sign-outs).
Roll Call Silent Mode: Built-in toggle to automatically mute loud alarm rings during official morning and afternoon roll calls to maintain decorum.
Smart Attendance Detection: Automatically cancels pending alarms if the user has already recorded their attendance in the local SQLite database.
🎨 User Experience (UI/UX)
Smart Home Screen & Countdown: Features a flexible countdown timer that automatically transforms into a greeting banner (`"Selamat menikmati hari liburmu! 🎉"`) during weekends.
Custom Alarm Audio: Allows users to select default alarm tones or custom `.mp3` files from device storage with a non-looping test sound feature (`playSoundOnce`).
Safe Back-Button Navigation: Implements double-tap confirmation logic on the home screen to prevent accidental app exits.
---
🛠️ Tech Stack
This application is built using a combination of modern Android development technologies:
Mobile Language: Kotlin / Java
Networking & API: Retrofit 2, OkHttp
Local Database: SQLite
UI & Graphics: ConstraintLayout, Custom Canvas (Digital Signature rendering)
Testing Tool: Ngrok (for local backend tunneling)
---
📦 Local Installation & Setup Guide
Clone the Repository:
```bash
   git clone https://github.com/YourUsername/RemindMe.git
   cd RemindMe
```
Open in Android Studio:
Open Android Studio, select Open an Existing Project, and choose the cloned `RemindMe` folder.
Configure Backend Base URL:
Navigate to your API client configuration file and set your server endpoint (or local Ngrok URL).
Sync Project with Gradle Files:
Let Android Studio download and sync all required dependencies (Retrofit, OkHttp, etc.).
Run the Application:
Connect an Android physical device (with USB debugging enabled) or start an emulator, then click the Run (`Shift + F10`) button in Android Studio.
---
© 2026 RemindMe. Built with precision and clean code.
