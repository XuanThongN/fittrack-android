package com.fittrack.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fittrack.app.data.model.ZombieAlertLevel
import com.fittrack.app.data.model.ZombieChaseState
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.CyanRun
import com.fittrack.app.ui.theme.DangerRed
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

/**
 * 100% Native OpenStreetMap View powered by osmdroid.
 * Features automatic auto-centering on real user GPS/Network location,
 * live marker tracking, 3-zombie chase pursuit, and accurate positioning.
 */
@Composable
fun RealStreetZombieMapView(
    pathPoints: List<Pair<Double, Double>>,
    currentLocation: Pair<Double, Double>?,
    zombieState: ZombieChaseState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isTopoLayer by remember { mutableStateOf(false) }
    var hasAutoCentered by remember { mutableStateOf(false) }

    // Fallback coordinate only if GPS is completely unavailable
    val defaultLat = currentLocation?.first ?: 10.7769
    val defaultLng = currentLocation?.second ?: 106.7009

    // Initialize osmdroid Configuration
    remember {
        Configuration.getInstance().apply {
            userAgentValue = "FitTrackApp/1.3 (com.fittrack.app; Android)"
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
    }

    // Retain native MapView and its overlays
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(17.5)
            controller.setCenter(GeoPoint(defaultLat, defaultLng))
        }
    }

    // Manage MapView Android lifecycle
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Native Polyline for runner route
    val routePolyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.parseColor("#06B6D4")
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }
    }

    // Native Marker for Runner
    val runnerMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createRunnerMarkerDrawable(context)
            position = GeoPoint(defaultLat, defaultLng)
            title = "BẠN"
        }
    }

    // Start Pin Marker
    val startMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = createStartFlagDrawable(context)
            title = "Điểm Xuất Phát"
        }
    }

    // 3 Native Zombie Markers
    val zombieLeadMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Zombie Đầu Đàn"
        }
    }
    val zombieFlank1Marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Zombie Hộ Vệ"
        }
    }
    val zombieFlank2Marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Zombie Đột Biến"
        }
    }

    // Ensure persistent route polyline is attached
    LaunchedEffect(mapView) {
        if (!mapView.overlays.contains(routePolyline)) {
            mapView.overlays.add(routePolyline)
        }
    }

    // Auto-center camera whenever the user's real location is first discovered
    LaunchedEffect(currentLocation) {
        if (currentLocation != null) {
            val userGeo = GeoPoint(currentLocation.first, currentLocation.second)
            if (!hasAutoCentered) {
                mapView.controller.animateTo(userGeo)
                mapView.controller.setZoom(17.5)
                hasAutoCentered = true
            }
        }
    }

    // Switch Tile Source (Mapnik Standard Street vs OpenTopo)
    LaunchedEffect(isTopoLayer) {
        if (isTopoLayer) {
            mapView.setTileSource(TileSourceFactory.OpenTopo)
        } else {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
        }
        mapView.invalidate()
    }

    // Live update coordinates, route, and zombies
    LaunchedEffect(pathPoints, currentLocation, zombieState.distanceMeters, zombieState.isEnabled) {
        if (currentLocation != null) {
            val curLat = currentLocation.first
            val curLng = currentLocation.second
            val runnerGeo = GeoPoint(curLat, curLng)

            // Update runner marker
            runnerMarker.position = runnerGeo
            if (!mapView.overlays.contains(runnerMarker)) {
                mapView.overlays.add(runnerMarker)
            }

            // Update route polyline
            if (pathPoints.isNotEmpty()) {
                val geoPoints = pathPoints.map { GeoPoint(it.first, it.second) }
                routePolyline.setPoints(geoPoints)

                startMarker.position = GeoPoint(pathPoints.first().first, pathPoints.first().second)
                if (!mapView.overlays.contains(startMarker)) {
                    mapView.overlays.add(startMarker)
                }
            }

            // Update Zombie Horde
            if (zombieState.isEnabled) {
                val distM = zombieState.distanceMeters.coerceAtLeast(1.0)
                val distRounded = (distM / 5).toInt() * 5

                // Position zombies along real street path behind runner
                var zLat = curLat - (distM * 0.000009)
                var zLng = curLng

                if (pathPoints.size >= 2) {
                    val backStep = (distM / 15).toInt().coerceAtMost(pathPoints.size - 1)
                    val targetIdx = (pathPoints.size - 1 - backStep).coerceAtLeast(0)
                    zLat = pathPoints[targetIdx].first
                    zLng = pathPoints[targetIdx].second
                }

                zombieLeadMarker.position = GeoPoint(zLat, zLng)
                zombieLeadMarker.icon = createZombieMarkerDrawable(context, distRounded)

                zombieFlank1Marker.position = GeoPoint(zLat - 0.00012, zLng + 0.00012)
                zombieFlank1Marker.icon = createZombieMarkerDrawable(context, (distRounded + 4))

                zombieFlank2Marker.position = GeoPoint(zLat - 0.00012, zLng - 0.00012)
                zombieFlank2Marker.icon = createZombieMarkerDrawable(context, (distRounded + 8))

                if (!mapView.overlays.contains(zombieLeadMarker)) mapView.overlays.add(zombieLeadMarker)
                if (!mapView.overlays.contains(zombieFlank1Marker)) mapView.overlays.add(zombieFlank1Marker)
                if (!mapView.overlays.contains(zombieFlank2Marker)) mapView.overlays.add(zombieFlank2Marker)
            } else {
                mapView.overlays.remove(zombieLeadMarker)
                mapView.overlays.remove(zombieFlank1Marker)
                mapView.overlays.remove(zombieFlank2Marker)
            }
        } else {
            // While GPS is searching, remove runner and zombie overlays from default map
            mapView.overlays.remove(runnerMarker)
            mapView.overlays.remove(zombieLeadMarker)
            mapView.overlays.remove(zombieFlank1Marker)
            mapView.overlays.remove(zombieFlank2Marker)
        }

        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
    ) {
        // Native OpenStreetMap View
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Bar: Mode tag & Proximity Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Layer switch pill: OSM Street vs Topo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground.copy(alpha = 0.92f))
                        .clickable { isTopoLayer = !isTopoLayer }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = CyanRun,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTopoLayer) "Địa Hình" else "OpenStreetMap",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Zombie distance badge if active
                if (zombieState.isEnabled) {
                    val badgeColor = when (zombieState.alertLevel) {
                        ZombieAlertLevel.DANGER, ZombieAlertLevel.CAUGHT -> DangerRed
                        ZombieAlertLevel.WARNING -> OrangeBurn
                        ZombieAlertLevel.SAFE -> EmeraldPrimary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBackground.copy(alpha = 0.92f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🧟 Zombie: ${zombieState.distanceMeters.toInt()}m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                    }
                }
            }
        }

        // Bottom Left GPS Status Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkBackground.copy(alpha = 0.92f))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (currentLocation != null) EmeraldPrimary else OrangeBurn)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (currentLocation != null) "GPS Đã Khóa" else "Đang Dò GPS...",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentLocation != null) EmeraldPrimary else OrangeBurn
            )
        }

        // Map Control Buttons: Zoom In/Out & My Location
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom In button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkBackground.copy(alpha = 0.92f))
                    .clickable { mapView.controller.zoomIn() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Phóng to",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Zoom Out button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkBackground.copy(alpha = 0.92f))
                    .clickable { mapView.controller.zoomOut() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Thu nhỏ",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Recenter My Location FAB
            FloatingActionButton(
                onClick = {
                    if (currentLocation != null) {
                        mapView.controller.animateTo(GeoPoint(currentLocation.first, currentLocation.second))
                        mapView.controller.setZoom(18.0)
                    } else {
                        Toast.makeText(
                            context,
                            "Đang dò GPS... Vui lòng bật vị trí (Location) trên máy!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.size(42.dp),
                containerColor = CyanRun,
                contentColor = DarkBackground,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Vị trí của tôi",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Creates custom BitmapDrawable for Runner Pin (🏃 BẠN)
 */
private fun createRunnerMarkerDrawable(context: Context): BitmapDrawable {
    val sizePx = 100
    val bitmap = Bitmap.createBitmap(sizePx, sizePx + 24, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer cyan glow
    paint.color = android.graphics.Color.parseColor("#4D06B6D4")
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

    // Cyan solid circle
    paint.color = android.graphics.Color.parseColor("#06B6D4")
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 8f, paint)

    // White outline
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 5f
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 8f, paint)

    // Runner Emoji
    paint.style = Paint.Style.FILL
    paint.textSize = 36f
    paint.textAlign = Paint.Align.CENTER
    val fontMetrics = paint.fontMetrics
    val textY = (sizePx / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2
    canvas.drawText("🏃", sizePx / 2f, textY, paint)

    // Badge "BẠN"
    val pillRect = RectF(sizePx / 2f - 32f, sizePx.toFloat() - 4f, sizePx / 2f + 32f, (sizePx + 22).toFloat())
    paint.color = android.graphics.Color.parseColor("#E60F172A")
    canvas.drawRoundRect(pillRect, 6f, 6f, paint)

    paint.color = android.graphics.Color.parseColor("#06B6D4")
    paint.textSize = 17f
    paint.isFakeBoldText = true
    canvas.drawText("BẠN", sizePx / 2f, (sizePx + 15).toFloat(), paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates custom BitmapDrawable for Zombie Marker (🧟 XXm)
 */
private fun createZombieMarkerDrawable(context: Context, distM: Int): BitmapDrawable {
    val sizePx = 90
    val bitmap = Bitmap.createBitmap(sizePx, sizePx + 22, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer red glow
    paint.color = android.graphics.Color.parseColor("#4DEF4444")
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

    // Red solid circle
    paint.color = android.graphics.Color.parseColor("#EF4444")
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 6f, paint)

    // White outline
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 6f, paint)

    // Zombie Emoji
    paint.style = Paint.Style.FILL
    paint.textSize = 32f
    paint.textAlign = Paint.Align.CENTER
    val fontMetrics = paint.fontMetrics
    val textY = (sizePx / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2
    canvas.drawText("🧟", sizePx / 2f, textY, paint)

    // Badge distance (e.g. "80m")
    val pillRect = RectF(sizePx / 2f - 30f, sizePx.toFloat() - 4f, sizePx / 2f + 30f, (sizePx + 20).toFloat())
    paint.color = android.graphics.Color.parseColor("#EF4444")
    canvas.drawRoundRect(pillRect, 6f, 6f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 16f
    paint.isFakeBoldText = true
    canvas.drawText("${distM}m", sizePx / 2f, (sizePx + 14).toFloat(), paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates Start Flag Drawable (🚩)
 */
private fun createStartFlagDrawable(context: Context): BitmapDrawable {
    val sizePx = 64
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }
    val fontMetrics = paint.fontMetrics
    val textY = (sizePx / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2
    canvas.drawText("🚩", sizePx / 2f, textY, paint)

    return BitmapDrawable(context.resources, bitmap)
}
