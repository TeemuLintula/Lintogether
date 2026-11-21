package com.example.lintogether.sub_functions

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow

@Composable
fun MoneyScreen() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("money_prefs", Context.MODE_PRIVATE) }

    var cash by remember { mutableStateOf(prefs.getString("cash", "0") ?: "0") }
    var investments by remember { mutableStateOf(prefs.getString("investments", "0") ?: "0") }
    var loans by remember { mutableStateOf(prefs.getString("loans", "0") ?: "0") }
    var other by remember { mutableStateOf(prefs.getString("other", "0") ?: "0") }

    val cashVal = cash.toDoubleOrNull() ?: 0.0
    val invVal = investments.toDoubleOrNull() ?: 0.0
    val loansVal = loans.toDoubleOrNull() ?: 0.0
    val otherVal = other.toDoubleOrNull() ?: 0.0

    val total = cashVal + invVal + otherVal - loansVal

    Scaffold(
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "NET WORTH",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = String.format(Locale.getDefault(), "%,.0f €", total),
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(48.dp))

            MoneyInputField(
                label = "Cash",
                value = cash,
                onValueChange = { 
                    cash = it
                    prefs.edit { putString("cash", it) }
                }
            )

            MoneyInputField(
                label = "Investments",
                value = investments,
                onValueChange = { 
                    investments = it
                    prefs.edit { putString("investments", it) }
                }
            )

            MoneyInputField(
                label = "Loans",
                value = loans,
                onValueChange = { 
                    loans = it
                    prefs.edit { putString("loans", it) }
                },
                isDeduction = true
            )

            MoneyInputField(
                label = "Other",
                value = other,
                onValueChange = { 
                    other = it
                    prefs.edit { putString("other", it) }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            MoneyPieChart(
                cash = cashVal,
                investments = invVal,
                loans = loansVal,
                other = otherVal
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            InvestmentGrowthChart(
                initialInvestment = invVal,
                baseBalance = cashVal + otherVal - loansVal
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InvestmentGrowthChart(initialInvestment: Double, baseBalance: Double) {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    
    var selectedYear by remember { mutableIntStateOf(2035) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    val horizons = listOf(
        "Current" to currentYear,
        "5 Years" to currentYear + 5,
        "2035" to 2035,
        "2045" to 2045
    )

    val annualReturn = 0.07
    val isShortTerm = (selectedYear - currentYear) <= 5
    
    val points = remember(initialInvestment, baseBalance, selectedYear) {
        val pts = mutableListOf<Double>()
        if (initialInvestment > 0 || baseBalance != 0.0) {
            if (isShortTerm) {
                val totalMonths = (selectedYear - currentYear) * 12 + (11 - currentMonth)
                for (m in 0..totalMonths) {
                    pts.add(baseBalance + initialInvestment * (1 + annualReturn).pow(m / 12.0))
                }
            } else {
                val totalYears = selectedYear - currentYear
                for (y in 0..totalYears) {
                    pts.add(baseBalance + initialInvestment * (1 + annualReturn).pow(y.toDouble()))
                }
            }
        }
        pts
    }

    val yearMarkers = remember(points, currentYear, selectedYear) {
        val markers = mutableListOf<Pair<Int, Int>>() // index to year
        if (points.isNotEmpty()) {
            markers.add(0 to currentYear)
            if (points.size > 1) {
                markers.add((points.size - 1) to selectedYear)
            }
        }
        markers
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column {
            Text(
                text = "INVESTMENT PROJECTION",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                horizons.forEach { (label, year) ->
                    val isSelected = selectedYear == year
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) Color(0xFF4CAF50) else Color(0xFF1E1E1E),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { 
                                selectedYear = year
                                selectedIndex = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (points.isNotEmpty()) {
                val maxValue = points.maxOrNull() ?: 1.0
                val minValue = points.minOrNull() ?: 0.0

                val currentSelectedIndex = selectedIndex ?: (points.size - 1)
                val selectedValue = points[currentSelectedIndex]
                
                val displayLabelX = if (isShortTerm) {
                    val monthCount = currentMonth + currentSelectedIndex
                    val year = currentYear + monthCount / 12
                    val monthName = java.text.DateFormatSymbols().months[monthCount % 12].take(3)
                    "$monthName $year"
                } else {
                    (currentYear + currentSelectedIndex).toString()
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Y-Axis Labels
                    Box(modifier = Modifier.width(50.dp).height(200.dp)) {
                        // Max value label
                        Text(
                            text = formatAxisValue(maxValue),
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(y = 10.dp)
                        )
                        
                        // Min value label
                        Text(
                            text = formatAxisValue(minValue),
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(y = (-10).dp)
                        )
                        
                        if (selectedIndex != null) {
                            val progress = (selectedValue - minValue) / (maxValue - minValue).coerceAtLeast(1.0)
                            Text(
                                text = formatAxisValue(selectedValue),
                                color = Color(0xFF4CAF50),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(y = (-(progress * 160.0).dp) - 10.dp)
                                    .background(Color.Black.copy(alpha = 0.7f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                                .pointerInput(points) {
                                    detectTapGestures { offset ->
                                        selectedIndex = (offset.x / size.width * (points.size - 1)).toInt().coerceIn(points.indices)
                                    }
                                }
                                .pointerInput(points) {
                                    detectDragGestures { change, _ ->
                                        selectedIndex = (change.position.x / size.width * (points.size - 1)).toInt().coerceIn(points.indices)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 20.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height
                                
                                // Vertical Grid Lines for year markers
                                yearMarkers.forEach { (idx, _) ->
                                    val x = if (points.size > 1) idx.toFloat() / (points.size - 1) * width else width / 2
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.05f),
                                        start = Offset(x, 0f), end = Offset(x, height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                                
                                // Horizontal grid lines (Top and Bottom)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = Offset(0f, 0f), end = Offset(width, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = Offset(0f, height), end = Offset(width, height),
                                    strokeWidth = 1.dp.toPx()
                                )

                                // Projection Path
                                val path = Path()
                                points.forEachIndexed { index, value ->
                                    val x = if (points.size > 1) index.toFloat() / (points.size - 1) * width else width / 2
                                    val y = height - ((value - minValue) / (maxValue - minValue).coerceAtLeast(1.0)).toFloat() * height
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path = path, color = Color(0xFF4CAF50), style = Stroke(width = 3.dp.toPx()))

                                // Dots for year starts
                                yearMarkers.forEach { (idx, _) ->
                                    val x = if (points.size > 1) idx.toFloat() / (points.size - 1) * width else width / 2
                                    val valAtMarker = points[idx]
                                    val yMarker = height - ((valAtMarker - minValue) / (maxValue - minValue).coerceAtLeast(1.0)).toFloat() * height
                                    drawCircle(
                                        color = Color(0xFF4CAF50).copy(alpha = 0.6f),
                                        radius = 3.dp.toPx(),
                                        center = Offset(x, yMarker)
                                    )
                                }

                                // Active Selection Crosshair
                                val selX = if (points.size > 1) currentSelectedIndex.toFloat() / (points.size - 1) * width else width / 2
                                val selY = height - ((selectedValue - minValue) / (maxValue - minValue).coerceAtLeast(1.0)).toFloat() * height
                                
                                if (selectedIndex != null) {
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.3f),
                                        start = Offset(selX, 0f), end = Offset(selX, height),
                                        strokeWidth = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.3f),
                                        start = Offset(0f, selY), end = Offset(width, selY),
                                        strokeWidth = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                }

                                drawCircle(color = Color(0xFF4CAF50), radius = 6.dp.toPx(), center = Offset(selX, selY))
                                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(selX, selY))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // X-Axis Year Labels
                        Box(modifier = Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 10.dp)) {
                            val canvasWidth = this@BoxWithConstraints.maxWidth - 50.dp - 8.dp - 20.dp
                            yearMarkers.forEach { (idx, year) ->
                                val progress = if (points.size > 1) idx.toFloat() / (points.size - 1) else 0.5f
                                Text(
                                    text = year.toString(),
                                    color = Color.Gray,
                                    fontSize = 8.sp,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = (progress * canvasWidth.value).dp - 10.dp)
                                )
                            }
                            
                            if (selectedIndex != null) {
                                val progress = if (points.size > 1) currentSelectedIndex.toFloat() / (points.size - 1) else 0.5f
                                Text(
                                    text = displayLabelX,
                                    color = Color(0xFF4CAF50),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = (progress * canvasWidth.value).dp - 15.dp)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = String.format(Locale.getDefault(), "Value: %,.0f €", selectedValue),
                    color = Color(0xFF4CAF50),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (selectedIndex != null) "at $displayLabelX" else "at end of $selectedYear",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val message = if (initialInvestment <= 0 && baseBalance == 0.0) "Add money to see projection" else "Select a future date"
                    Text(
                        text = message,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

fun formatAxisValue(value: Double): String {
    return when {
        value >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.getDefault(), "%.0fK", value / 1_000)
        else -> String.format(Locale.getDefault(), "%.0f", value)
    }
}

@Composable
fun MoneyPieChart(
    cash: Double,
    investments: Double,
    loans: Double,
    other: Double
) {
    val entries = listOf(
        "Cash" to (cash to Color(0xFF4CAF50)),
        "Investments" to (investments to Color(0xFF2196F3)),
        "Loans" to (loans to Color(0xFFF44336)),
        "Other" to (other to Color(0xFFFFC107))
    ).filter { it.second.first > 0 }

    if (entries.isEmpty()) return

    val totalValue = entries.sumOf { it.second.first }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                entries.forEach { (_, data) ->
                    val (value, color) = data
                    val sweepAngle = (value / totalValue).toFloat() * 360f
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Legend
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEach { (label, data) ->
                val (value, color) = data
                val percentage = (value / totalValue) * 100
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f € (%.1f%%)", value, percentage),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MoneyInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDeduction: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isDeduction) Color(0xFFF44336) else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = value,
            onValueChange = { input ->
                if (input.isEmpty() || input.toDoubleOrNull() != null || input == ".") {
                    onValueChange(input)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            suffix = { Text("€", color = Color.Gray, modifier = Modifier.padding(start = 4.dp)) }
        )
    }
}
