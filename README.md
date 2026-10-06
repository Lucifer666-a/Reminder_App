# RemindMe - Smart Attendance & Reminder System

RemindMe is a smart Android-based attendance and reminder application designed for institutional employees. The application combines online attendance, digital signatures, scheduled reminders, and backend integration to provide a more structured and secure attendance workflow.

---

## 🚀 Key Features

### 📱 Employee Side

* **Online Attendance System:** Employees can submit attendance directly to the institution's backend server through REST API integration.
* **Meeting/Event Attendance:** Retrieves active meeting or event topics created by administrators and allows employees to submit their attendance.
* **Digital Signature:** Employees can provide a handwritten digital signature directly from the application.
* **Apel Attendance:** Dedicated morning and afternoon attendance sessions with automatic session identification.
* **Date Validation:** Attendance forms are automatically locked when the selected date is not the current date.
* **Identity Protection:** Name and position information can only be modified once every 7 days.
* **Smart Attendance Detection:** The application detects existing local attendance records and automatically prevents unnecessary reminder alarms.
* **Dynamic Countdown:** Displays the remaining time until the next scheduled attendance or reminder.
* **Custom Alarm Sound:** Users can select between the default alarm or a custom `.mp3` audio file.
* **Weekend Mode:** Automatically displays a holiday message after the Friday afternoon attendance or during weekends.

### ⏰ Smart Reminder System

* **Dynamic Workday Scheduling:** Alarm times automatically adjust according to the day and attendance schedule.
* **Monday Morning Reminder:** Scheduled at 07:45 before the 08:00 morning assembly.
* **Tuesday–Friday Reminder:** Scheduled at 07:55.
* **Friday Afternoon Reminder:** Scheduled at 16:10.
* **Monday–Thursday Afternoon Reminder:** Scheduled at 16:30.
* **Silent Assembly Mode:** Automatically suppresses loud alarm sounds during designated assembly periods.
* **Alarm Popup:** Uses a dedicated alarm activity instead of relying on notification sounds.

### 🔐 Security & Data Protection

* **Admin PIN Verification:** Meeting attendance requires an administrator-provided PIN before the attendance request is submitted.
* **Date-Based Attendance Lock:** Prevents attendance submissions for previous or future dates.
* **7-Day Identity Lock:** Restricts frequent changes to employee identity information.
* **Base64 Digital Signature:** Converts the user's handwritten signature into a PNG bitmap and encodes it into Base64 before being sent to the backend.
* **Backend Validation:** Attendance data is submitted directly to the server through protected API endpoints.

---

## 🌐 Backend & API Integration

RemindMe communicates with the backend using REST APIs through Retrofit and OkHttp.

### Available Endpoints

* `GET /api/acara.php` — Retrieves the currently active meeting/event topic.
* `POST /api/absen.php` — Submits meeting attendance, digital signature, and Admin PIN verification.
* `POST /api/absen_apel.php` — Submits morning or afternoon assembly attendance.

The application also supports testing through Ngrok using the `ngrok-skip-browser-warning` request header.

---

## 🛠️ Tech Stack

* **Platform:** Android
* **Programming Language:** Kotlin
* **Networking:** Retrofit & OkHttp
* **Local Database:** SQLite
* **Backend:** REST API
* **Data Format:** JSON
* **Digital Signature:** Bitmap → PNG → Base64
* **Remote Testing:** Ngrok

---

## 📱 User Experience

RemindMe is designed around a simple attendance workflow:

1. Open the application.
2. View the next attendance/reminder schedule.
3. Receive an alarm when attendance time approaches.
4. Open the attendance form.
5. Complete the required attendance information.
6. Provide a digital signature when required.
7. Enter the Admin PIN for meeting attendance.
8. Submit attendance to the backend.
9. The application records the attendance locally and updates the home screen.

---

© 2026 RemindMe. Built for smarter and more reliable institutional attendance management.
