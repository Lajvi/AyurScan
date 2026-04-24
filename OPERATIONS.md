# AyurScan - Operations Guide

This guide provides step-by-step instructions on how to set up, run, and operate the AyurScan project. The project consists of an Android App (frontend) and a FastAPI server (backend).

## 1. Project Architecture
*   **Android App**: Built with Kotlin and Jetpack Compose. Handles UI, user authentication via Firebase, and displaying data.
*   **Python Backend**: Built with FastAPI. Provides Ayurvedic recommendations, medicine data, and AI features. Located in the `ayur-food-recommender` directory.

---

## 2. Running the Python Backend (FastAPI)

The backend provides the API that the Android app communicates with. **It must be running for the app's recommendation features to function.**

### Prerequisites:
*   Python 3.8 or higher installed on your computer.

### Steps:
1.  Open a terminal or command prompt in Android Studio.
2.  Navigate to the backend directory:
    ```powershell
    cd ayur-food-recommender
    ```
3.  Start the server. The command you use depends on how you are testing the app:
    
    **Option A: If testing on an Android Emulator**
    ```powershell
    python -m uvicorn main:app --reload --host 127.0.0.1 --port 8000
    ```

    **Option B: If testing on a Physical Android Phone via Wi-Fi**
    ```powershell
    python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
    ```
    *(The `--host 0.0.0.0` flag exposes the server to your local Wi-Fi network, allowing your phone to connect to it).*

---

## 3. Configuring the Network Connection

The Android app needs to know where to find the Python backend. You must configure the IP address based on your testing environment.

1. Open the network configuration file: 
   `app/src/main/java/com/example/ayurscan/network/RetrofitClient.kt`
2. Update the `BASE_URL` constant depending on your setup:

*   **For Android Emulator:**
    ```kotlin
    private const val BASE_URL = "http://10.0.2.2:8000/"
    ```
    *(The emulator uses `10.0.2.2` as a special alias to reach your computer's localhost).*

*   **For Physical Phone (Wi-Fi):**
    Open your terminal, type `ipconfig`, and find your computer's IPv4 Address (e.g., `10.37.98.138`).
    ```kotlin
    private const val BASE_URL = "http://10.37.98.138:8000/"
    ```

---

## 4. Running the Android Application

### Steps:
1.  Open the **AyurScan** project in Android Studio.
2.  Wait for Gradle to finish syncing (watch the progress bar at the bottom).
3.  Connect your physical Android device via USB, or start an Android Emulator from the Device Manager.
4.  Click the green **Run 'app'** button (Shift + F10) in the top toolbar of Android Studio.

Alternatively, you can build and install using the terminal:
```powershell
# To build and install the debug app
.\gradlew installDebug

# To clean the project (useful if you encounter strange build errors)
.\gradlew clean
```

---

## 5. Firebase Configuration Notes

This app relies on Firebase for User Authentication and the Firestore Database.
*   **google-services.json**: This file must be present inside your `app/` folder. It links the Android app to your specific Firebase project.
*   **Database**: Ensure your Firestore database rules allow authenticated users to read and write their profiles and scan data.
