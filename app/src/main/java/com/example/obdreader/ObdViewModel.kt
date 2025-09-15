package com.example.obdreader

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

data class LiveData(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val coolantC: Int = 0,
    val batteryV: Double = 0.0
)

data class ObdSession(
    val timestamp: String,
    val dtcs: List<String>,
    val liveData: LiveData
)

class ObdViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    var connectionStatus by mutableStateOf("Disconnected")
        private set
    
    var pairedDevices by mutableStateOf<List<BluetoothDevice>>(emptyList())
        private set
    
    var selectedDevice by mutableStateOf<BluetoothDevice?>(null)
        private set
    
    var liveData by mutableStateOf(LiveData())
        private set
    
    var dtcs by mutableStateOf<List<String>>(emptyList())
        private set
    
    var isReading by mutableStateOf(false)
        private set
    
    var lastError by mutableStateOf<String?>(null)
        private set

    init {
        scanPairedDevices()
    }

    fun scanPairedDevices() {
        if (bluetoothAdapter == null) {
            lastError = "Bluetooth not supported"
            return
        }

        if (ActivityCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            lastError = "Bluetooth permission required"
            return
        }

        val paired = bluetoothAdapter.bondedDevices.filter { device ->
            val name = device.name?.lowercase() ?: ""
            name.contains("elm") || name.contains("obd") || name.contains("vgate")
        }
        
        pairedDevices = paired
        if (paired.isNotEmpty() && selectedDevice == null) {
            selectedDevice = paired.first()
        }
    }

    fun selectDevice(device: BluetoothDevice) {
        selectedDevice = device
    }

    fun connect() {
        val device = selectedDevice ?: return
        
        viewModelScope.launch {
            try {
                connectionStatus = "Connecting..."
                
                withContext(Dispatchers.IO) {
                    if (ActivityCompat.checkSelfPermission(
                            getApplication(),
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        throw SecurityException("Bluetooth permission required")
                    }

                    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                    bluetoothSocket?.connect()
                    
                    inputStream = bluetoothSocket?.inputStream
                    outputStream = bluetoothSocket?.outputStream
                    
                    // Initialize ELM327
                    initializeElm327()
                }
                
                connectionStatus = "Connected to ${device.name}"
                lastError = null
                
            } catch (e: Exception) {
                connectionStatus = "Connection failed"
                lastError = e.message
                disconnect()
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    bluetoothSocket?.close()
                } catch (e: IOException) {
                    // Ignore
                }
                bluetoothSocket = null
                inputStream = null
                outputStream = null
            }
            connectionStatus = "Disconnected"
        }
    }

    private suspend fun initializeElm327() = withContext(Dispatchers.IO) {
        val commands = listOf("ATZ", "ATE0", "ATL0", "ATH0", "ATSP0")
        
        for (command in commands) {
            sendCommand(command)
            Thread.sleep(100)
        }
    }

    private suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        val output = outputStream ?: throw IOException("Not connected")
        val input = inputStream ?: throw IOException("Not connected")
        
        output.write("$command\r".toByteArray())
        output.flush()
        
        val buffer = ByteArray(1024)
        val bytesRead = input.read(buffer)
        val response = String(buffer, 0, bytesRead).trim()
        
        response
    }

    fun readData() {
        if (bluetoothSocket?.isConnected != true) {
            lastError = "Not connected to OBD device"
            return
        }

        viewModelScope.launch {
            isReading = true
            lastError = null
            
            try {
                withContext(Dispatchers.IO) {
                    // Read live data
                    val rpm = readPid("010C")
                    val speed = readPid("010D")
                    val coolant = readPid("0105")
                    val battery = readBatteryVoltage()
                    
                    liveData = LiveData(
                        rpm = rpm,
                        speedKmh = speed,
                        coolantC = coolant,
                        batteryV = battery
                    )
                    
                    // Read DTCs
                    dtcs = readDtcs()
                }
            } catch (e: Exception) {
                lastError = e.message
            } finally {
                isReading = false
            }
        }
    }

    private suspend fun readPid(pid: String): Int = withContext(Dispatchers.IO) {
        try {
            val response = sendCommand(pid)
            val hex = response.replace(" ", "").replace(">", "")
            
            when (pid) {
                "010C" -> { // RPM
                    if (hex.length >= 10) {
                        val a = hex.substring(6, 8).toInt(16)
                        val b = hex.substring(8, 10).toInt(16)
                        (256 * a + b) / 4
                    } else 0
                }
                "010D" -> { // Speed
                    if (hex.length >= 8) {
                        hex.substring(6, 8).toInt(16)
                    } else 0
                }
                "0105" -> { // Coolant temp
                    if (hex.length >= 8) {
                        hex.substring(6, 8).toInt(16) - 40
                    } else 0
                }
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun readBatteryVoltage(): Double = withContext(Dispatchers.IO) {
        try {
            val response = sendCommand("ATRV")
            val voltage = response.replace("V", "").trim().toDoubleOrNull() ?: 0.0
            voltage
        } catch (e: Exception) {
            0.0
        }
    }

    private suspend fun readDtcs(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = sendCommand("03")
            val dtcList = mutableListOf<String>()
            
            if (response.contains("NO DATA") || response.contains("43 00")) {
                return@withContext emptyList()
            }
            
            val hex = response.replace(" ", "").replace(">", "")
            
            // Parse DTC codes from response
            // This is a simplified parser - real implementation would be more robust
            var i = 4 // Skip "43" response header
            while (i + 3 < hex.length) {
                val byte1 = hex.substring(i, i + 2).toIntOrNull(16) ?: break
                val byte2 = hex.substring(i + 2, i + 4).toIntOrNull(16) ?: break
                
                if (byte1 == 0 && byte2 == 0) break
                
                val firstChar = when (byte1 shr 6) {
                    0 -> "P"
                    1 -> "C"
                    2 -> "B"
                    3 -> "U"
                    else -> "P"
                }
                
                val dtc = String.format("%s%04X", firstChar, ((byte1 and 0x3F) shl 8) or byte2)
                dtcList.add(dtc)
                
                i += 4
            }
            
            dtcList
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveToJson() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val documentsDir = File(context.filesDir, "Documents")
                    if (!documentsDir.exists()) {
                        documentsDir.mkdirs()
                    }
                    
                    val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        .format(Date())
                    
                    val jsonObject = JSONObject().apply {
                        put("timestamp", timestamp)
                        put("dtcs", JSONArray(dtcs))
                        put("live", JSONObject().apply {
                            put("rpm", liveData.rpm)
                            put("speed_kmh", liveData.speedKmh)
                            put("coolant_c", liveData.coolantC)
                            put("battery_v", liveData.batteryV)
                        })
                    }
                    
                    val file = File(documentsDir, "obd_session.json")
                    file.writeText(jsonObject.toString(2))
                    
                    lastError = "Saved to ${file.absolutePath}"
                }
            } catch (e: Exception) {
                lastError = "Save failed: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
