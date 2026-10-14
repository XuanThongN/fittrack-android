package com.fittrack.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.CyanRun
import com.fittrack.app.ui.theme.DarkBackground

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RunningMapView(
    pathPoints: List<Pair<Double, Double>>,
    currentLocation: Pair<Double, Double>?,
    modifier: Modifier = Modifier
) {
    val webView = remember { mutableMapOf<String, WebView?>() }

    val htmlContent = remember {
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
                }
                .leaflet-control-attribution { display: none !important; }
                .leaflet-bar { border: none !important; box-shadow: 0 4px 12px rgba(0,0,0,0.5) !important; }
                .leaflet-bar a { background-color: #1E293B !important; color: #F8FAFC !important; border-bottom: 1px solid #334155 !important; }
                .pulse-marker {
                    width: 18px;
                    height: 18px;
                    background: #06B6D4;
                    border: 3px solid #FFFFFF;
                    border-radius: 50%;
                    box-shadow: 0 0 12px #06B6D4;
                }
                .start-marker {
                    width: 16px;
                    height: 16px;
                    background: #10B981;
                    border: 3px solid #FFFFFF;
                    border-radius: 50%;
                    box-shadow: 0 0 8px #10B981;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false }).setView([16.0544, 108.2022], 15);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19
                }).addTo(map);

                var polyline = L.polyline([], {
                    color: '#06B6D4',
                    weight: 6,
                    opacity: 0.9,
                    lineJoin: 'round',
                    lineCap: 'round'
                }).addTo(map);

                var currentMarker = null;
                var startMarker = null;
                var hasCentered = false;

                function updateMap(pointsJson, curLat, curLng) {
                    try {
                        var pts = JSON.parse(pointsJson);
                        if (pts && pts.length > 0) {
                            polyline.setLatLngs(pts);

                            if (!startMarker && pts.length >= 1) {
                                var sIcon = L.divIcon({ className: 'start-marker', iconSize: [16, 16], iconAnchor: [8, 8] });
                                startMarker = L.marker(pts[0], { icon: sIcon }).addTo(map);
                            }
                        }

                        if (curLat && curLng) {
                            var curPos = [curLat, curLng];
                            if (!currentMarker) {
                                var cIcon = L.divIcon({ className: 'pulse-marker', iconSize: [18, 18], iconAnchor: [9, 9] });
                                currentMarker = L.marker(curPos, { icon: cIcon }).addTo(map);
                            } else {
                                currentMarker.setLatLng(curPos);
                            }

                            if (!hasCentered) {
                                map.setView(curPos, 16);
                                hasCentered = true;
                            }
                        }
                    } catch(e) {}
                }

                function centerCurrent(lat, lng) {
                    if (lat && lng) {
                        map.setView([lat, lng], 17);
                    } else if (polyline.getLatLngs().length > 0) {
                        map.fitBounds(polyline.getBounds(), { padding: [50, 50] });
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    // Pass data updates to WebView
    LaunchedEffect(pathPoints, currentLocation) {
        val wv = webView["wv"] ?: return@LaunchedEffect
        val pointsArray = pathPoints.joinToString(prefix = "[", postfix = "]") { "[${it.first}, ${it.second}]" }
        val curLat = currentLocation?.first ?: "null"
        val curLng = currentLocation?.second ?: "null"
        wv.evaluateJavascript("updateMap('$pointsArray', $curLat, $curLng);", null)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://unpkg.com", htmlContent, "text/html", "UTF-8", null)
                    webView["wv"] = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center on Me button
        FloatingActionButton(
            onClick = {
                val wv = webView["wv"]
                val lat = currentLocation?.first ?: "null"
                val lng = currentLocation?.second ?: "null"
                wv?.evaluateJavascript("centerCurrent($lat, $lng);", null)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            containerColor = CardSurface,
            contentColor = CyanRun
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Vị trí của tôi"
            )
        }
    }
}
