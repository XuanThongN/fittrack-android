package com.fittrack.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fittrack.app.data.model.ZombieAlertLevel
import com.fittrack.app.data.model.ZombieChaseState
import com.fittrack.app.service.RunningSessionManager
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.CyanRun
import com.fittrack.app.ui.theme.DangerRed
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.util.StreetChaseEngine
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

// 100% Free, NO API KEY required OpenStreetMap tile providers (no watermark, unblocked):
// 1. OSM HOT: High-contrast Humanitarian OpenStreetMap with clear Vietnamese street names and buildings
private val OsmHotTileSource = XYTileSource(
    "OSM_HOT",
    1,
    19,
    256,
    ".png",
    arrayOf(
        "https://a.tile.openstreetmap.fr/hot/",
        "https://b.tile.openstreetmap.fr/hot/"
    )
)

// 2. CyclOSM: Dedicated running, walking and cycling map with trails, pedestrian ways, and parks
private val CyclOsmTileSource = XYTileSource(
    "CyclOSM",
    1,
    18,
    256,
    ".png",
    arrayOf(
        "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
        "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
        "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"
    )
)

/**
 * 100% Native OpenStreetMap View powered by osmdroid.
 * Features:
 * - Real Street Snapping & Routing: Uses OpenStreetMap OSRM street network so zombies always
 *   spawn and pursue along the actual real-world street, following every turn and intersection.
 * - Glowing Red Pursuit Trail: Shows the exact road polyline connecting the zombie horde to the runner.
 * - AAA Game-Quality Animated Sprites: 8 FPS multi-frame athletic sprint and monster claw attack sprites.
 */
@Composable
fun RealStreetZombieMapView(
    pathPoints: List<Pair<Double, Double>>,
    currentLocation: Pair<Double, Double>?,
    currentBearing: Float = 0f,
    zombieState: ZombieChaseState,
    isRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var layerMode by remember { mutableIntStateOf(0) } // 0: Carto Voyager, 1: Carto Dark, 2: OpenTopo
    var hasAutoCentered by remember { mutableStateOf(false) }

    // Pre-fetched real OpenStreetMap street prefix nodes from RunningSessionManager (Single source of truth)
    val streetPrefix by RunningSessionManager.streetPrefix.collectAsState()
    var isFollowBearing by remember { mutableStateOf(false) }

    // Fallback coordinate only if GPS is completely unavailable
    val defaultLat = currentLocation?.first ?: 10.7769
    val defaultLng = currentLocation?.second ?: 106.7009

    // Continuous 8 FPS Character Animation Loop (125ms per frame)
    var animFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(125L)
            animFrame = (animFrame + 1) % 4
        }
    }

    // Initialize osmdroid Configuration & cleanup any old watermarked Carto tiles
    remember {
        Configuration.getInstance().apply {
            userAgentValue = "FitTrackApp/1.6.2 (com.fittrack.app; Android)"
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        try {
            File(context.cacheDir, "osmdroid/tiles/CartoVoyager").deleteRecursively()
            File(context.cacheDir, "osmdroid/tiles/CartoDark").deleteRecursively()
        } catch (_: Exception) {}
    }

    // Retain native MapView and its overlays
    val mapView = remember {
        MapView(context).apply {
            clipToOutline = true
            setTileSource(OsmHotTileSource)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(17.5)
            controller.setCenter(GeoPoint(defaultLat, defaultLng))
        }
    }

    // Manage MapView Android lifecycle (vital for Lock Screen wake/sleep resume)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
        }
    }

    // Native Polyline for runner route (Cyan)
    val routePolyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.parseColor("#06B6D4")
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }
    }

    // Native Polyline for Zombie Pursuit Trail (Crimson Red along real street)
    val zombieTrailPolyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.parseColor("#EF4444")
            outlinePaint.strokeWidth = 9f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            outlinePaint.alpha = 190
        }
    }

    // Native Marker for Runner
    val runnerMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
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

    // 3 Native Zombie Markers (Alpha Leader, Flank Stalker, Mutant)
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

    // Auto-center camera whenever the user's real location is first discovered
    LaunchedEffect(currentLocation) {
        if (currentLocation != null) {
            val userGeo = GeoPoint(currentLocation.first, currentLocation.second)
            if (!hasAutoCentered) {
                mapView.controller.animateTo(userGeo)
                mapView.controller.setZoom(17.5)
                hasAutoCentered = true
            }
            if (streetPrefix.isEmpty()) {
                RunningSessionManager.fetchStreetPrefixIfAvailable()
            }
        }
    }

    // Switch Tile Source (0: OSM HOT Streets, 1: CyclOSM Running Trails, 2: OpenTopo)
    LaunchedEffect(layerMode) {
        val tileSource = when (layerMode) {
            0 -> OsmHotTileSource
            1 -> CyclOsmTileSource
            else -> TileSourceFactory.OpenTopo
        }
        mapView.setTileSource(tileSource)
        mapView.invalidate()
    }

    // Heading rotation (Compass mode)
    LaunchedEffect(currentBearing, isFollowBearing) {
        val targetOrientation = if (isFollowBearing && currentBearing != 0f) -currentBearing else 0f
        if (mapView.mapOrientation != targetOrientation) {
            mapView.setMapOrientation(targetOrientation, false)
            mapView.invalidate()
        }
    }

    // Live update coordinates, real street route navigation, and animated sprites
    LaunchedEffect(
        animFrame,
        pathPoints,
        streetPrefix,
        currentLocation,
        currentBearing,
        zombieState.distanceMeters,
        zombieState.isEnabled,
        isRunning
    ) {
        if (currentLocation != null) {
            val curLat = currentLocation.first
            val curLng = currentLocation.second
            val runnerGeo = GeoPoint(curLat, curLng)

            // 1. Update Animated Runner Marker
            runnerMarker.position = runnerGeo
            runnerMarker.icon = MarkerAnimationFactory.createRunnerFrameDrawable(
                context = context,
                frame = animFrame,
                isMoving = isRunning
            )
            if (!mapView.overlays.contains(runnerMarker)) {
                mapView.overlays.add(runnerMarker)
            }

            // 2. Update Route Polyline
            if (pathPoints.isNotEmpty()) {
                val geoPoints = pathPoints.map { GeoPoint(it.first, it.second) }
                routePolyline.setPoints(geoPoints)

                startMarker.position = GeoPoint(pathPoints.first().first, pathPoints.first().second)
                if (!mapView.overlays.contains(startMarker)) {
                    mapView.overlays.add(startMarker)
                }
                if (!mapView.overlays.contains(routePolyline)) {
                    mapView.overlays.add(routePolyline)
                }
            }

            // 3. Update Real Street Zombie Horde Navigation
            if (zombieState.isEnabled) {
                val distM = zombieState.distanceMeters.coerceAtLeast(1.0)
                val distRounded = (distM / 5).toInt() * 5

                // Zombie 1: Alpha Leader (chases strictly along the center of the real street polyline)
                val leadChase = StreetChaseEngine.getPointAlongPathBackwards(
                    streetPrefix = streetPrefix,
                    pathPoints = pathPoints,
                    currentLocation = currentLocation,
                    currentBearing = currentBearing,
                    distanceBackMeters = distM,
                    lateralOffsetMeters = 0.0
                )
                zombieLeadMarker.position = GeoPoint(leadChase.position.first, leadChase.position.second)
                zombieLeadMarker.icon = MarkerAnimationFactory.createZombieFrameDrawable(
                    context = context,
                    frame = animFrame,
                    distMeters = distRounded,
                    alertLevel = zombieState.alertLevel,
                    zombieType = 0
                )

                // Zombie 2: Flank Stalker (chases 5m behind along real street, on right side of road +1.8m)
                val flank1Dist = distM + 5.0
                val flank1Chase = StreetChaseEngine.getPointAlongPathBackwards(
                    streetPrefix = streetPrefix,
                    pathPoints = pathPoints,
                    currentLocation = currentLocation,
                    currentBearing = currentBearing,
                    distanceBackMeters = flank1Dist,
                    lateralOffsetMeters = 1.8
                )
                zombieFlank1Marker.position = GeoPoint(flank1Chase.position.first, flank1Chase.position.second)
                zombieFlank1Marker.icon = MarkerAnimationFactory.createZombieFrameDrawable(
                    context = context,
                    frame = (animFrame + 1) % 4,
                    distMeters = ((flank1Dist / 5).toInt() * 5),
                    alertLevel = zombieState.alertLevel,
                    zombieType = 1
                )

                // Zombie 3: Toxic Mutant (chases 10m behind along real street, on left side of road -1.8m)
                val flank2Dist = distM + 10.0
                val flank2Chase = StreetChaseEngine.getPointAlongPathBackwards(
                    streetPrefix = streetPrefix,
                    pathPoints = pathPoints,
                    currentLocation = currentLocation,
                    currentBearing = currentBearing,
                    distanceBackMeters = flank2Dist,
                    lateralOffsetMeters = -1.8
                )
                zombieFlank2Marker.position = GeoPoint(flank2Chase.position.first, flank2Chase.position.second)
                zombieFlank2Marker.icon = MarkerAnimationFactory.createZombieFrameDrawable(
                    context = context,
                    frame = (animFrame + 2) % 4,
                    distMeters = ((flank2Dist / 5).toInt() * 5),
                    alertLevel = zombieState.alertLevel,
                    zombieType = 2
                )

                // 4. Update Glowing Red Pursuit Trail along real street
                val pursuitPoints = StreetChaseEngine.getPursuitTrailPoints(
                    streetPrefix = streetPrefix,
                    pathPoints = pathPoints,
                    currentLocation = currentLocation,
                    zombiePos = leadChase.position
                )
                if (pursuitPoints.size >= 2) {
                    zombieTrailPolyline.setPoints(pursuitPoints.map { GeoPoint(it.first, it.second) })
                    if (!mapView.overlays.contains(zombieTrailPolyline)) {
                        mapView.overlays.add(0, zombieTrailPolyline)
                    }
                } else {
                    mapView.overlays.remove(zombieTrailPolyline)
                }

                if (!mapView.overlays.contains(zombieLeadMarker)) mapView.overlays.add(zombieLeadMarker)
                if (!mapView.overlays.contains(zombieFlank1Marker)) mapView.overlays.add(zombieFlank1Marker)
                if (!mapView.overlays.contains(zombieFlank2Marker)) mapView.overlays.add(zombieFlank2Marker)
            } else {
                mapView.overlays.remove(zombieTrailPolyline)
                mapView.overlays.remove(zombieLeadMarker)
                mapView.overlays.remove(zombieFlank1Marker)
                mapView.overlays.remove(zombieFlank2Marker)
            }
        } else {
            mapView.overlays.remove(runnerMarker)
            mapView.overlays.remove(zombieTrailPolyline)
            mapView.overlays.remove(zombieLeadMarker)
            mapView.overlays.remove(zombieFlank1Marker)
            mapView.overlays.remove(zombieFlank2Marker)
        }

        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
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
                // Layer switch pill: Voyager -> Dark -> Topo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground.copy(alpha = 0.92f))
                        .clickable { layerMode = (layerMode + 1) % 3 }
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
                        text = when (layerMode) {
                            0 -> "Đường Phố"
                            1 -> "Đường Chạy Bộ"
                            else -> "Địa Hình"
                        },
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

            // Toggle Heading Follow / Compass
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isFollowBearing) CyanRun else DarkBackground.copy(alpha = 0.92f))
                    .clickable { isFollowBearing = !isFollowBearing },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFollowBearing) "🧭" else "⬆️",
                    fontSize = 15.sp
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
