package com.example.progetto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.progetto.R
import com.example.progetto.data.dao.TripDao
import com.example.progetto.data.entity.Trip
import com.example.progetto.data.entity.TripType
import com.example.progetto.utils.NotificationHelper
import com.example.progetto.utils.StatisticsHelper
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.progetto.workers.TripReminderWorker
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.livedata.observeAsState
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.util.UUID
import androidx.work.workDataOf
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    tripDao: TripDao,
    onNavigateBack: () -> Unit
) {
    val trips by tripDao.getAllTrips().collectAsState(initial = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    var lastWorkId by remember { mutableStateOf<UUID?>(null) }
    val workInfo = lastWorkId?.let { id ->
        WorkManager.getInstance(context).getWorkInfoByIdLiveData(id).observeAsState().value
    }
    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.travel_statistics)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(
                    onClick = {
                        // 测试发送通知
                        NotificationHelper.sendTripReminderNotification(
                            context = context,
                            title = "Prova di notifica",
                            message = "Norifica funziona!！"
                        )
                    }
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.test_notification)
                    )
                }

                FloatingActionButton(
                    onClick = {
                        val req = OneTimeWorkRequestBuilder<TripReminderWorker>()
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .setInputData(workDataOf("forceNotify" to true))
                            .build()
                        lastWorkId = req.id
                        WorkManager.getInstance(context).enqueueUniqueWork(
                            "trip_reminder_now", ExistingWorkPolicy.REPLACE, req
                        )
                    }
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.run_worker)
                    )
                }
            }
        }

    ) { padding ->
        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📊",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_statistics_data),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.start_recording_statistics),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            StatisticsContent(
                trips = trips,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun StatisticsContent(
    trips: List<Trip>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val stats = remember(trips) { StatisticsHelper.getTotalStats(trips) }
    val monthlyCount = remember(trips) { StatisticsHelper.getMonthlyTripCount(trips) }
    val monthlyDistance = remember(trips) { StatisticsHelper.getMonthlyDistance(trips) }
    val typeDistribution = remember(trips) { StatisticsHelper.getTripTypeDistribution(trips) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        TotalStatsCard(stats = stats)

        Spacer(modifier = Modifier.height(24.dp))

        ChartCard(
            title = stringResource(R.string.monthly_trip_count),
            subtitle = stringResource(R.string.last_6_months)
        ) {
            MonthlyTripCountChart(monthlyCount = monthlyCount)
        }

        Spacer(modifier = Modifier.height(24.dp))

        ChartCard(
            title = stringResource(R.string.monthly_distance),
            subtitle = stringResource(R.string.unit_km)
        ) {
            MonthlyDistanceChart(monthlyDistance = monthlyDistance)
        }

        Spacer(modifier = Modifier.height(24.dp))

        ChartCard(
            title = stringResource(R.string.trip_type_distribution),
            subtitle = stringResource(R.string.total_trips_count, trips.size)
        ) {
            TripTypeDistributionChart(distribution = typeDistribution)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TotalStatsCard(stats: StatisticsHelper.TotalStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.overview),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = stringResource(R.string.total_trips), value = stringResource(R.string.count_unit, stats.totalTrips))
                StatItem(label = stringResource(R.string.total_distance), value = String.format("%.1f km", stats.totalDistance))
                StatItem(label = stringResource(R.string.average_distance), value = String.format("%.1f km", stats.averageDistance))
            }

            stats.longestTrip?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.longest_journey, it.destination, it.distance),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}


@Composable
private fun MonthlyTripCountChart(monthlyCount: Map<String, Int>) {
    val recentMonths = StatisticsHelper.getRecentMonths(6)
    val data = recentMonths.map { month -> (monthlyCount[month] ?: 0).toDouble() }

    if (data.all { it == 0.0 }) {
        Text(
            text = stringResource(R.string.no_data_last_6_months),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp)
        )
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                series(data)
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer()
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}


@Composable
private fun MonthlyDistanceChart(monthlyDistance: Map<String, Double>) {
    val recentMonths = StatisticsHelper.getRecentMonths(6)
    val data = recentMonths.map { month -> (monthlyDistance[month] ?: 0.0).toDouble() }

    if (data.all { it == 0.0 }) {
        Text(
            text = stringResource(R.string.no_data_last_6_months),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp)
        )
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                series(data)
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer()
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
private fun TripTypeDistributionChart(distribution: Map<TripType, Int>) {
    val total = distribution.values.sum().toFloat()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TripType.values().forEach { type ->
            val count = distribution[type] ?: 0
            val percentage = if (total > 0) (count / total * 100) else 0f
            TripTypeBar(type = type, count = count, percentage = percentage)
        }
    }
}

@Composable
private fun TripTypeBar(type: TripType, count: Int, percentage: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = type.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.count_unit, count) + " " + stringResource(R.string.percentage, percentage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.shapes.small
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .fillMaxHeight()
                    .background(
                        getColorForTripType(type),
                        MaterialTheme.shapes.small
                    )
            )
        }
    }
}

@Composable
private fun getColorForTripType(type: TripType): Color {
    return when (type) {
        TripType.LOCAL -> Color(0xFF4CAF50)
        TripType.DAY_TRIP -> Color(0xFF2196F3)
        TripType.MULTI_DAY -> Color(0xFFFF9800)
    }
}
