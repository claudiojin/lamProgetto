package com.example.progetto

import EditTripScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.progetto.data.entity.Trip
import com.example.progetto.data.dao.TripDao
import com.example.progetto.data.database.TripDatabase
import com.example.progetto.data.entity.TripType
import com.example.progetto.ui.components.TripCard
import com.example.progetto.ui.theme.ProgettoTheme
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.example.progetto.data.dao.LocationDao
import com.example.progetto.ui.screens.GpsTestScreen

class MainActivity : ComponentActivity() {

    //istanza di database
    private lateinit var database: TripDatabase
    private lateinit var tripDao: TripDao
    private lateinit var locationDao: LocationDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        database = TripDatabase.getDatabase(this)
        tripDao = database.tripDao()
        locationDao = database.locationDao()

//        insertTestData()


        setContent {
            enableEdgeToEdge()
            ProgettoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    TripListScreen(
//                        tripDao = tripDao,
//                        modifier = Modifier.padding(innerPadding)
//                    )
                    GpsTestScreen()
                }
            }
        }
    }


    @Composable
    fun TripListScreen(
        tripDao: TripDao,
        modifier: Modifier = Modifier
    ) {
        // 从数据库获取数据（Flow自动更新）
        val trips by tripDao.getAllTrips().collectAsState(initial = emptyList())
        val coroutineScope = rememberCoroutineScope()

        var tripToDelete by remember { mutableStateOf<Trip?>(null) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        var tripToEdit by remember { mutableStateOf<Trip?>(null) }
        var showEditScreen by remember { mutableStateOf(false) }


        if(showEditScreen){
            EditTripScreen(
                trip = tripToEdit,
                onSave = {
                    trip -> coroutineScope.launch {
                        if (tripToEdit == null){
                            tripDao.insert(trip)
                        }else{
                            tripDao.update(trip)

                        }
                        showEditScreen= false
                        tripToEdit = null
                }

                },
                onCancel = {
                    showEditScreen = false
                    tripToEdit = null
                }
            )
        } else {
            Scaffold(modifier = modifier.fillMaxSize(),
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            tripToEdit = null
                            showEditScreen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Trip"
                        )
                    }
                }
                ) {
                innerPadding ->
                Column(modifier = modifier.fillMaxSize()
                    .padding(innerPadding)) {
                    // 标题
                    Text(
                        text = "我的旅行 (共${trips.size}个)",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    // 旅行列表
                    if (trips.isEmpty()) {
                        // 空状态
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无旅行记录")
                        }
                    } else {
                        LazyColumn {
                            items(trips) { trip ->
                                TripCard(
                                    trip = trip,
                                    onDeleted = {
                                        tripToDelete = it
                                        showDeleteDialog = true
                                    },
                                    onEdit = {tripToEdit = it
                                        showEditScreen = true
                                    }
                                )
                            }
                        }
                    }
                }}
            }


        if (showDeleteDialog && tripToDelete != null){
            DeleteConfirmationDialog(
                trip = tripToDelete!!,
                onConfirm = {
                    coroutineScope.launch {
                        tripDao.delete(tripToDelete!!)
                        showDeleteDialog = false
                        tripToDelete = null

                    }
                },
                onDismiss = {
                    showDeleteDialog = false
                    tripToDelete = null
                }
            )
        }
    }




    @Composable
    fun DeleteConfirmationDialog(
        trip: Trip,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ){
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Conferma Eliminazione")
            },
            text = {
                Text("Sicuro di eliminare ${trip.destination}?")
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")

                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Annulla")
                }
            }
        )
    }


    private fun insertTestData() {
        lifecycleScope.launch {
            // 检查数据库是否为空

            // 只在数据库为空时插入测试数据
            // 为了测试，我们直接插入
            val trip1 = Trip(
                destination = "北京",
                startDate = "2025-11-01",
                endDate = "2025-11-03",
                type = TripType.MULTI_DAY,
                notes = "参观长城和故宫",
                distance = 234.5
            )

            val trip2 = Trip(
                destination = "上海",
                startDate = "2025-11-15",
                endDate = "2025-11-15",
                type = TripType.DAY_TRIP,
                notes = "外滩一日游"
            )

            val trip3 = Trip(
                destination = "市区公园",
                startDate = "2025-11-20",
                endDate = "2025-11-20",
                type = TripType.LOCAL,
                notes = "晨跑"
            )

            tripDao.insert(trip1)
            tripDao.insert(trip2)
            tripDao.insert(trip3)
        }
    }

}
