# MyFitness App

## Overview

MyFitness is an Android mobile application that supports users in their daily fitness journey. Based on an MVVM architecture and developed using Jetpack Compose, it leverages Firebase for authentication and data storage, and Room for offline support.

After registration and profile completion (age, height, weight, goals), the app uses Google’s Gemini API to generate a personalized weekly training plan. The plan is enhanced with tutorial videos hosted in Firestore. Users can track their exercises daily, log their progress, and view statistics in real-time. Weekly data is archived, enabling performance tracking over time. Offline mode and daily notifications ensure user engagement and accessibility.

---

## Features

* Secure User Authentication with Firebase
* Profile setup with personal metrics and fitness goals
* Automated weekly workout plan generation via Gemini AI
* Exercise tracking: sets, reps, calories
* Embedded video tutorials for guided execution
* Real-time statistics and weekly performance archive
* Offline mode with Room and sync to Firestore
* Daily notifications for workout reminders

---

## Technologies Used

* Kotlin
* Android Studio
* Jetpack Compose
* Firebase Authentication
* Firebase Firestore
* Google Gemini API
* Room (offline persistence)
* WorkManager (notifications)
* MVVM architecture

---

## Setup Instructions

1. Clone the repository https://github.com/slimbahae/MyFitnessAndroidApp.git
2. Add your `google-services.json` file in the `app/` directory
3. Configure your Gemini API key in `local.properties`:

   ```
   GEMINI_API_KEY=your-api-key
   ```
4. Open the project in Android Studio and run on an emulator or physical device (API 31+)

---

## Author

Developed by SLIMANI Bahaeddine, BOUAZZA Chaymae, and BENABBOU Imane — ENSIAS
