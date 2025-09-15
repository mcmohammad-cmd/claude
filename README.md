# Android OBD-II Reader

A minimal Android app that connects to an ELM327 Bluetooth OBD-II dongle to read vehicle diagnostic data and save it as JSON.

## Features

- Connect to ELM327 Bluetooth Classic dongles
- Read live vehicle data (RPM, Speed, Coolant Temperature, Battery Voltage)
- Read Diagnostic Trouble Codes (DTCs)
- Save session data to JSON files

## Setup Instructions

### 1. Hardware Requirements
- ELM327 Bluetooth Classic OBD-II dongle (not BLE)
- Android device with Bluetooth support
- Vehicle with OBD-II port (1996+ in US)

### 2. Installation
1. Build the project in Android Studio
2. Install the generated APK on your Android device
3. Grant Bluetooth and Location permissions when prompted

### 3. Connecting to ELM327

#### First Time Setup:
1. Plug ELM327 dongle into your vehicle's OBD-II port (usually under dashboard)
2. Turn on vehicle ignition (engine doesn't need to be running)
3. On your Android device, go to Settings > Bluetooth
4. Look for device named "ELM327", "OBD-II", or "Vgate" 
5. Pair with default PIN: usually 1234 or 0000
6. Open the OBD Reader app

#### Using the App:
1. **Connect Tab**: 
   - Tap "Scan Devices" to find paired OBD dongles
   - Select your ELM327 device from the list
   - Tap "Connect" - status should show "Connected to [device name]"

2. **Read Data Tab**:
   - Ensure you're connected first
   - Tap "Read OBD Data" to get current vehicle parameters
   - View live data: RPM, Speed, Coolant Temperature, Battery Voltage
   - Check for any Diagnostic Trouble Codes (DTCs)

3. **Save JSON Tab**:
   - Review the current session data
   - Tap "Save to JSON File" to store the data
   - File is saved as `obd_session.json`

### 4. JSON File Location

Files are saved to the app's private storage:
```
/data/data/com.example.obdreader/files/Documents/obd_session.json
```

**Note**: This location requires root access to view directly. To access the file:
- Use Android Debug Bridge (ADB): `adb pull /data/data/com.example.obdreader/files/Documents/obd_session.json`
- Or modify the app to save to external storage for easier access

### 5. JSON File Format

```json
{
  "timestamp": "2025-09-15T10:30:00Z",
  "dtcs": ["P0301", "P0420"],
  "live": {
    "rpm": 820,
    "speed_kmh": 0,
    "coolant_c": 92,
    "battery_v": 13.7
  }
}
```

### 6. Troubleshooting

**Connection Issues:**
- Ensure ELM327 is properly plugged into OBD-II port
- Vehicle ignition must be ON
- Check Bluetooth pairing - unpair and re-pair if needed
- Try different ELM327 device if connection fails

**Reading Issues:**
- Some vehicles may not support all PIDs
- Engine should be running for accurate RPM readings
- Older vehicles (pre-2001) may have limited OBD-II support

**Permission Issues:**
- Grant Bluetooth and Location permissions in Android settings
- For Android 12+, ensure "Nearby devices" permission is granted

### 7. Supported Vehicles

This app works with any vehicle that supports OBD-II protocol:
- US vehicles: 1996 and newer
- European vehicles: 2001 and newer (EOBD)
- Most Asian vehicles: 2000 and newer

### 8. Technical Details

**Supported PIDs:**
- 010C: Engine RPM
- 010D: Vehicle Speed  
- 0105: Engine Coolant Temperature
- ATRV: Control Module Voltage (Battery)
- 03: Request trouble codes

**ELM327 Initialization:**
- ATZ: Reset
- ATE0: Echo off
- ATL0: Linefeeds off
- ATH0: Headers off
- ATSP0: Set protocol to auto

### 9. Development Notes

**Project Structure:**
- `MainActivity.kt`: Entry point and Compose setup
- `ObdViewModel.kt`: Business logic, Bluetooth communication, data processing
- `ObdReaderScreen.kt`: UI components and screens
- Minimal dependencies: Android Jetpack Compose, Material3, Coroutines

**Key Classes:**
- `ObdViewModel`: Manages Bluetooth connection and OBD communication
- `LiveData`: Data class for vehicle parameters
- `ObdSession`: Data class for complete session with timestamp

**Bluetooth Implementation:**
- Uses RFCOMM/SPP profile (UUID: 00001101-0000-1000-8000-00805F9B34FB)
- Classic Bluetooth only (not BLE)
- Handles Android 12+ permission changes

### 10. Building from Source

**Requirements:**
- Android Studio Hedgehog or newer
- Kotlin 1.9+
- compileSdk 34, minSdk 24, targetSdk 34

**Build Steps:**
1. Download this repository (zip)
2. Open the folder in Android Studio as an Existing Project
3. Create a top-level `settings.gradle.kts` with: 
   ```kotlin
   include(":app")
   rootProject.name = "ObdReader"
   ```
4. Create a minimal top-level `build.gradle.kts` with Kotlin and Android Gradle plugin classpaths if needed (or let Android Studio generate)
5. Sync Gradle and build

### 11. License & Disclaimer

This is a minimal educational implementation. For production use:
- Add proper error handling and validation
- Implement more robust OBD-II protocol parsing
- Add support for manufacturer-specific codes
- Include proper logging and crash reporting
- Consider security implications of Bluetooth communication

**Disclaimer**: This app is for educational purposes. Always ensure vehicle safety and comply with local laws when using OBD-II diagnostic tools while driving.
