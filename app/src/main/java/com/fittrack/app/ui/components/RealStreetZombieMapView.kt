package com.fittrack.app.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * RealStreetZombieMapView: High-detail real-world street map (CartoDB Voyager + OSM & ESRI Satellite)
 * with animated Runner pin and real-world Zombie Horde pursuing along actual streets and roads.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RealStreetZombieMapView(
    pathPoints: List<Pair<Double, Double>>,
    currentLocation: Pair<Double, Double>?,
    zombieState: ZombieChaseState,
    modifier: Modifier = Modifier
) {
    val webViewRef = remember { mutableMapOf<String, WebView?>() }
    var isSatellite by remember { mutableStateOf(false) }

    // Default to landmark if GPS hasn't locked yet (e.g. 10.7769, 106.7009 Ho Chi Minh City center)
    val defaultLat = currentLocation?.first ?: 10.7769
    val defaultLng = currentLocation?.second ?: 106.7009

    val html = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    background: #0F172A;
                    overflow: hidden;
                }
                .leaflet-control-attribution { display: none !important; }
                
                @keyframes pulseGlow {
                    0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(6, 182, 212, 0.8); }
                    70% { transform: scale(1.08); box-shadow: 0 0 0 16px rgba(6, 182, 212, 0); }
                    100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(6, 182, 212, 0); }
                }
                
                @keyframes zombieHop {
                    0% { transform: translateY(0px) rotate(-5deg) scale(1); }
                    50% { transform: translateY(-7px) rotate(5deg) scale(1.05); }
                    100% { transform: translateY(0px) rotate(-5deg) scale(1); }
                }

                .runner-marker {
                    background: #06B6D4;
                    color: white;
                    border: 3px solid #FFFFFF;
                    border-radius: 50%;
                    width: 36px;
                    height: 36px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 20px;
                    animation: pulseGlow 1.8s infinite;
                    box-shadow: 0 4px 14px rgba(6, 182, 212, 0.9);
                }

                .runner-label {
                    background: rgba(15, 23, 42, 0.85);
                    color: #06B6D4;
                    font-weight: 800;
                    font-size: 10px;
                    padding: 2px 6px;
                    border-radius: 6px;
                    border: 1px solid #06B6D4;
                    white-space: nowrap;
                    margin-top: 2px;
                    text-align: center;
                }

                .zombie-marker {
                    background: #EF4444;
                    color: white;
                    border: 2px solid #FFFFFF;
                    border-radius: 50%;
                    width: 32px;
                    height: 32px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 18px;
                    animation: zombieHop 0.55s infinite alternate;
                    box-shadow: 0 4px 14px rgba(239, 68, 68, 0.9);
                }

                .zombie-label {
                    background: rgba(239, 68, 68, 0.9);
                    color: #FFFFFF;
                    font-weight: 800;
                    font-size: 9px;
                    padding: 1px 4px;
                    border-radius: 4px;
                    white-space: nowrap;
                    margin-top: 1px;
                    text-align: center;
                }

                .start-marker {
                    background: #10B981;
                    color: white;
                    border: 2px solid #FFFFFF;
                    border-radius: 50%;
                    width: 28px;
                    height: 28px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 14px;
                    box-shadow: 0 2px 8px rgba(16, 185, 129, 0.6);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = null;
                var streetLayer = null;
                var satLayer = null;
                var runnerMarker = null;
                var startMarker = null;
                var zombieMarkers = [];
                var routePolyline = null;
                var hasAutoCentered = false;

                function initMap(lat, lng) {
                    if (map) return;
                    map = L.map('map', { 
                        zoomControl: false,
                        touchZoom: true,
                        dragging: true
                    }).setView([lat, lng], 17);

                    // High-resolution real-world street tiles with Vietnamese road & street names
                    streetLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                        maxZoom: 20,
                        subdomains: 'abcd'
                    }).addTo(map);

                    // ESRI Satellite Imagery layer
                    satLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                        maxZoom: 19
                    });

                    // Running Polyline route with high-visibility cyan neon glow
                    routePolyline = L.polyline([], {
                        color: '#06B6D4',
                        weight: 6,
                        opacity: 0.95,
                        lineJoin: 'round',
                        lineCap: 'round'
                    }).addTo(map);

                    // Draw runner at start pos
                    setRunnerPosition(lat, lng);
                }

                function setRunnerPosition(lat, lng) {
                    if (!map) return;
                    var pos = [lat, lng];

                    if (!runnerMarker) {
                        var icon = L.divIcon({
                            className: '',
                            html: '<div style="display:flex;flex-direction:column;align-items:center;"><div class="runner-marker">🏃</div><div class="runner-label">BẠN</div></div>',
                            iconSize: [44, 52],
                            iconAnchor: [22, 26]
                        });
                        runnerMarker = L.marker(pos, { icon: icon, zIndexOffset: 1000 }).addTo(map);
                    } else {
                        runnerMarker.setLatLng(pos);
                    }
                }

                function updateRunnerAndZombies(pathJson, curLat, curLng, zombieDistMeters, isZombieActive) {
                    var lat = (curLat !== null && curLat !== undefined) ? curLat : $defaultLat;
                    var lng = (curLng !== null && curLng !== undefined) ? curLng : $defaultLng;

                    if (!map) {
                        initMap(lat, lng);
                    }

                    try {
                        var pts = JSON.parse(pathJson);
                        if (pts && pts.length > 0) {
                            routePolyline.setLatLngs(pts);

                            if (!startMarker && pts.length >= 1) {
                                var sIcon = L.divIcon({
                                    className: '',
                                    html: '<div class="start-marker">🚩</div>',
                                    iconSize: [28, 28],
                                    iconAnchor: [14, 14]
                                });
                                startMarker = L.marker(pts[0], { icon: sIcon }).addTo(map);
                            }
                        }

                        setRunnerPosition(lat, lng);

                        if (!hasAutoCentered) {
                            map.setView([lat, lng], 17);
                            hasAutoCentered = true;
                        }

                        // UPDATE ZOMBIES POSITION ON THE REAL STREET!
                        updateZombies(lat, lng, pts, zombieDistMeters, isZombieActive);
                    } catch(e) {
                        console.error("Map update error:", e);
                    }
                }

                function updateZombies(curLat, curLng, pts, distMeters, isZombieActive) {
                    // Remove old zombie markers
                    for (var i = 0; i < zombieMarkers.length; i++) {
                        map.removeLayer(zombieMarkers[i]);
                    }
                    zombieMarkers = [];

                    if (!isZombieActive) return;

                    // Calculate zombie position along real road or offset behind runner
                    // 1 meter lat is ~ 0.000009 degrees
                    var latOffset = - (distMeters * 0.000009);
                    var zLat = curLat + latOffset;
                    var zLng = curLng;

                    // If we have runner GPS track history, place zombie on real street path behind runner
                    if (pts && pts.length >= 2) {
                        var idx = Math.max(0, pts.length - 1 - Math.round(distMeters / 15));
                        zLat = pts[idx][0];
                        zLng = pts[idx][1];
                    }

                    // Tactical 3-zombie chasing formation along the street
                    var zombieCoords = [
                        { pos: [zLat, zLng], label: 'Đầu Đàn' },
                        { pos: [zLat - 0.00012, zLng + 0.00012], label: 'Hộ Vệ' },
                        { pos: [zLat - 0.00012, zLng - 0.00012], label: 'Đột Biến' }
                    ];

                    var zIcon = L.divIcon({
                        className: '',
                        html: '<div style="display:flex;flex-direction:column;align-items:center;"><div class="zombie-marker">🧟</div><div class="zombie-label">' + Math.round(distMeters) + 'm</div></div>',
                        iconSize: [36, 46],
                        iconAnchor: [18, 23]
                    });

                    for (var j = 0; j < zombieCoords.length; j++) {
                        var zm = L.marker(zombieCoords[j].pos, { icon: zIcon, zIndexOffset: 500 }).addTo(map);
                        zombieMarkers.push(zm);
                    }
                }

                function toggleLayer(isSat) {
                    if (!map) return;
                    if (isSat) {
                        if (map.hasLayer(streetLayer)) map.removeLayer(streetLayer);
                        if (!map.hasLayer(satLayer)) satLayer.addTo(map);
                    } else {
                        if (map.hasLayer(satLayer)) map.removeLayer(satLayer);
                        if (!map.hasLayer(streetLayer)) streetLayer.addTo(map);
                    }
                }

                function centerOnMe(lat, lng) {
                    if (!map) return;
                    if (lat && lng) {
                        map.setView([lat, lng], 18);
                    } else if (runnerMarker) {
                        map.setView(runnerMarker.getLatLng(), 18);
                    }
                }

                function zoomIn() {
                    if (map) map.zoomIn();
                }

                function zoomOut() {
                    if (map) map.zoomOut();
                }

                window.onload = function() {
                    initMap($defaultLat, $defaultLng);
                    updateRunnerAndZombies('[]', $defaultLat, $defaultLng, ${zombieState.distanceMeters}, ${zombieState.isEnabled});
                };
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    // Live update coordinates and zombie chase
    LaunchedEffect(pathPoints, currentLocation, zombieState.distanceMeters, zombieState.isEnabled) {
        val wv = webViewRef["wv"] ?: return@LaunchedEffect
        val pointsJson = pathPoints.joinToString(prefix = "[", postfix = "]") { "[${it.first}, ${it.second}]" }
        val curLat = currentLocation?.first ?: defaultLat
        val curLng = currentLocation?.second ?: defaultLng
        val distM = zombieState.distanceMeters
        val zActive = zombieState.isEnabled
        wv.evaluateJavascript("updateRunnerAndZombies('$pointsJson', $curLat, $curLng, $distM, $zActive);", null)
    }

    // Live update layer toggle
    LaunchedEffect(isSatellite) {
        val wv = webViewRef["wv"] ?: return@LaunchedEffect
        wv.evaluateJavascript("toggleLayer($isSatellite);", null)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowContentAccess = true
                        allowFileAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://basemaps.cartocdn.com", html, "text/html", "UTF-8", null)
                    webViewRef["wv"] = this
                }
            },
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
                // Layer switch pill: Street vs Satellite
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground.copy(alpha = 0.92f))
                        .clickable { isSatellite = !isSatellite }
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
                        text = if (isSatellite) "Vệ Tinh" else "Đường Phố Thật",
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

        // Map Control Buttons: Zoom In/Out & My Location
        androidx.compose.foundation.layout.Column(
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
                    .clickable {
                        val wv = webViewRef["wv"]
                        wv?.evaluateJavascript("zoomIn();", null)
                    },
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
                    .clickable {
                        val wv = webViewRef["wv"]
                        wv?.evaluateJavascript("zoomOut();", null)
                    },
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
                    val wv = webViewRef["wv"]
                    val lat = currentLocation?.first ?: defaultLat
                    val lng = currentLocation?.second ?: defaultLng
                    wv?.evaluateJavascript("centerOnMe($lat, $lng);", null)
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
