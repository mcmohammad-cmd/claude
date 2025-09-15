package com.example.obdreader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObdReaderScreen(viewModel: ObdViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Connect", "Read Data", "Save JSON")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "OBD-II Reader",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> ConnectTab(viewModel)
            1 -> ReadDataTab(viewModel)
            2 -> SaveJsonTab(viewModel)
        }

        // Error display
        viewModel.lastError?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun ConnectTab(viewModel: ObdViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Connection Status: ${viewModel.connectionStatus}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Available OBD Devices:")
        
        LazyColumn(
            modifier = Modifier.height(200.dp)
        ) {
            items(viewModel.pairedDevices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { viewModel.selectDevice(device) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = device.name ?: "Unknown Device",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = device.address,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (viewModel.selectedDevice == device) {
                            Text(
                                text = "Selected",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.scanPairedDevices() }
            ) {
                Text("Scan Devices")
            }

            Button(
                onClick = { viewModel.connect() },
                enabled = viewModel.selectedDevice != null && 
                         viewModel.connectionStatus != "Connected to ${viewModel.selectedDevice?.name}"
            ) {
                Text("Connect")
            }

            Button(
                onClick = { viewModel.disconnect() },
                enabled = viewModel.connectionStatus.contains("Connected")
            ) {
                Text("Disconnect")
            }
        }
    }
}

@Composable
fun ReadDataTab(viewModel: ObdViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = { viewModel.readData() },
            enabled = viewModel.connectionStatus.contains("Connected") && !viewModel.isReading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (viewModel.isReading) "Reading..." else "Read OBD Data")
        }

        if (viewModel.isReading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Live Data",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                DataRow("RPM", "${viewModel.liveData.rpm}")
                DataRow("Speed", "${viewModel.liveData.speedKmh} km/h")
                DataRow("Coolant Temp", "${viewModel.liveData.coolantC}°C")
                DataRow("Battery Voltage", "${viewModel.liveData.batteryV}V")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Diagnostic Trouble Codes (DTCs)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (viewModel.dtcs.isEmpty()) {
                    Text(
                        text = "No trouble codes found",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    viewModel.dtcs.forEach { dtc ->
                        Text(
                            text = dtc,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaveJsonTab(viewModel: ObdViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Save Current Data to JSON",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Session Data:",
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("RPM: ${viewModel.liveData.rpm}")
                Text("Speed: ${viewModel.liveData.speedKmh} km/h")
                Text("Coolant: ${viewModel.liveData.coolantC}°C")
                Text("Battery: ${viewModel.liveData.batteryV}V")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("DTCs: ${if (viewModel.dtcs.isEmpty()) "None" else viewModel.dtcs.joinToString(", ")}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.saveToJson() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save to JSON File")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Files are saved to:\n/data/data/com.example.obdreader/files/Documents/obd_session.json",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Medium)
        Text(text = value)
    }
}
