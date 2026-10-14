#!/usr/bin/env python3
"""
FitTrack Termux Companion & Local Web Dashboard
Cho phép quản lý cân nặng, bài tập và xem trực tiếp trên trình duyệt điện thoại.
"""

import sys
import os
import json
import sqlite3
import http.server
import socketserver
import time
from datetime import datetime

DB_PATH = os.path.expanduser("~/.fittrack_termux.db")

def init_db():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("""
        CREATE TABLE IF NOT EXISTS profile (
            id INTEGER PRIMARY KEY,
            name TEXT,
            height_cm REAL,
            initial_weight_kg REAL,
            target_weight_kg REAL,
            age INTEGER,
            gender TEXT
        )
    """)
    c.execute("""
        CREATE TABLE IF NOT EXISTS weights (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            weight_kg REAL,
            timestamp INTEGER,
            note TEXT
        )
    """)
    c.execute("""
        CREATE TABLE IF NOT EXISTS workouts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT,
            duration_sec INTEGER,
            calories REAL,
            timestamp INTEGER
        )
    """)
    c.execute("""
        CREATE TABLE IF NOT EXISTS runs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            distance_km REAL,
            duration_sec INTEGER,
            calories REAL,
            pace_str TEXT,
            timestamp INTEGER
        )
    """)
    # Default profile
    c.execute("SELECT COUNT(*) FROM profile")
    if c.fetchone()[0] == 0:
        c.execute("""
            INSERT INTO profile (id, name, height_cm, initial_weight_kg, target_weight_kg, age, gender)
            VALUES (1, 'Bạn', 170.0, 75.0, 65.0, 26, 'male')
        """)
        c.execute("""
            INSERT INTO weights (weight_kg, timestamp, note)
            VALUES (75.0, ?, 'Cân ban đầu')
        """, (int(time.time() * 1000) - 7 * 86400 * 1000,))
        c.execute("""
            INSERT INTO weights (weight_kg, timestamp, note)
            VALUES (73.8, ?, 'Tiến độ tuần 1')
        """, (int(time.time() * 1000),))
        c.execute("""
            INSERT INTO workouts (title, duration_sec, calories, timestamp)
            VALUES ('HIIT Đốt Mỡ Thần Tốc', 900, 180.0, ?)
        """, (int(time.time() * 1000),))
        c.execute("""
            INSERT INTO runs (distance_km, duration_sec, calories, pace_str, timestamp)
            VALUES (3.5, 1260, 240.0, '6''00"', ?)
        """, (int(time.time() * 1000),))
    conn.commit()
    conn.close()

def get_data():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT name, height_cm, initial_weight_kg, target_weight_kg, age, gender FROM profile WHERE id = 1")
    prof = c.fetchone()
    profile = {
        "name": prof[0], "height_cm": prof[1], "initial_weight": prof[2],
        "target_weight": prof[3], "age": prof[4], "gender": prof[5]
    }
    c.execute("SELECT id, weight_kg, timestamp, note FROM weights ORDER BY timestamp DESC")
    weights = [{"id": r[0], "weight": r[1], "timestamp": r[2], "note": r[3],
                "date": datetime.fromtimestamp(r[2]/1000).strftime("%d/%m/%Y %H:%M")}
               for r in c.fetchall()]
    c.execute("SELECT id, title, duration_sec, calories, timestamp FROM workouts ORDER BY timestamp DESC")
    workouts = [{"id": r[0], "title": r[1], "duration_min": r[2]//60, "calories": r[3],
                 "date": datetime.fromtimestamp(r[4]/1000).strftime("%d/%m/%Y %H:%M")}
                for r in c.fetchall()]
    c.execute("SELECT id, distance_km, duration_sec, calories, pace_str, timestamp FROM runs ORDER BY timestamp DESC")
    runs = [{"id": r[0], "distance_km": r[1], "duration_min": r[2]//60, "calories": r[3], "pace": r[4],
             "date": datetime.fromtimestamp(r[5]/1000).strftime("%d/%m/%Y %H:%M")}
            for r in c.fetchall()]
    conn.close()
    return {"profile": profile, "weights": weights, "workouts": workouts, "runs": runs}

HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FitTrack - Theo Dõi Cân Nặng & Thể Thao</title>
    <style>
        :root {
            --bg: #0F172A;
            --surface: #1E293B;
            --primary: #10B981;
            --orange: #F97316;
            --cyan: #06B6D4;
            --text: #F8FAFC;
            --muted: #94A3B8;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: var(--bg); color: var(--text); padding: 16px; max-width: 600px; margin: 0 auto; }
        header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        h1 { font-size: 24px; color: var(--text); }
        .badge { background: rgba(16,185,129,0.15); color: var(--primary); padding: 4px 10px; border-radius: 20px; font-weight: bold; font-size: 13px; }
        .card { background: var(--surface); border-radius: 16px; padding: 18px; margin-bottom: 16px; }
        .weight-hero { font-size: 46px; font-weight: 800; color: var(--text); }
        .grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; margin-bottom: 16px; }
        .stat-card { background: var(--surface); border-radius: 14px; padding: 14px; text-align: center; }
        .stat-val { font-size: 20px; font-weight: bold; margin-top: 4px; }
        .stat-label { font-size: 12px; color: var(--muted); }
        .progress-bar-bg { background: #334155; height: 10px; border-radius: 5px; overflow: hidden; margin: 10px 0; }
        .progress-bar-fill { background: var(--primary); height: 100%; border-radius: 5px; }
        .btn { background: var(--primary); color: #0F172A; border: none; padding: 12px 18px; border-radius: 12px; font-weight: bold; width: 100%; cursor: pointer; font-size: 15px; }
        .btn-cyan { background: var(--cyan); }
        .btn-orange { background: var(--orange); color: white; }
        .flex-row { display: flex; gap: 10px; }
        input, select { background: #0F172A; border: 1px solid #334155; color: white; padding: 10px 14px; border-radius: 10px; width: 100%; margin-bottom: 10px; }
        .list-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #334155; }
        .list-item:last-child { border-bottom: none; }
    </style>
</head>
<body>
    <header>
        <div>
            <h1>FitTrack 🏃‍♂️</h1>
            <p style="color: var(--muted); font-size: 13px;">Theo dõi Cân nặng & Giảm mỡ</p>
        </div>
        <span class="badge">Termux Live</span>
    </header>

    <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: baseline;">
            <div>
                <span class="stat-label">CÂN NẶNG HIỆN TẠI</span>
                <div class="weight-hero" id="curWeight">-- kg</div>
            </div>
            <div id="bmiBadge" style="text-align: right;"></div>
        </div>
        
        <div class="progress-bar-bg">
            <div class="progress-bar-fill" id="progFill" style="width: 50%;"></div>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 12px; color: var(--muted);">
            <span id="initWeight">Bắt đầu: --</span>
            <span id="lostWeight" style="color: var(--primary); font-weight: bold;">--</span>
            <span id="targetWeight" style="color: var(--orange);">Mục tiêu: --</span>
        </div>
    </div>

    <div class="grid-3">
        <div class="stat-card">
            <div class="stat-label">Calo hôm nay</div>
            <div class="stat-val" style="color: var(--orange);" id="todayCal">0 kcal</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Chạy bộ</div>
            <div class="stat-val" style="color: var(--cyan);" id="totalKm">0.0 km</div>
        </div>
        <div class="stat-card">
            <div class="stat-label">Workout</div>
            <div class="stat-val" style="color: var(--primary);" id="totalMins">0 phút</div>
        </div>
    </div>

    <div class="card">
        <h3 style="margin-bottom: 12px;">⚡ Ghi Nhanh Cân Nặng</h3>
        <form id="weightForm">
            <div class="flex-row">
                <input type="number" step="0.1" id="wInput" placeholder="Số cân (kg), vd: 73.2" required>
                <input type="text" id="noteInput" placeholder="Ghi chú (sáng sớm...)">
            </div>
            <button type="submit" class="btn">Lưu Cân Nặng</button>
        </form>
    </div>

    <div class="card">
        <h3 style="margin-bottom: 12px;">📋 Lịch Sử Cân Nặng Gần Đây</h3>
        <div id="weightList"></div>
    </div>

    <div class="card">
        <h3 style="margin-bottom: 12px;">🏋️ Buổi Tập Workout Đốt Mỡ</h3>
        <div id="workoutList"></div>
    </div>

    <div class="card">
        <h3 style="margin-bottom: 12px;">🏃 Buổi Chạy Bộ</h3>
        <div id="runList"></div>
    </div>

    <script>
        async function load() {
            const res = await fetch('/api/data');
            const data = await res.json();
            const prof = data.profile;
            const latest = data.weights[0] ? data.weights[0].weight : prof.initial_weight;
            
            document.getElementById('curWeight').innerText = latest.toFixed(1) + ' kg';
            document.getElementById('initWeight').innerText = 'Bắt đầu: ' + prof.initial_weight + ' kg';
            document.getElementById('targetWeight').innerText = 'Mục tiêu: ' + prof.target_weight + ' kg';
            
            const lost = (prof.initial_weight - latest).toFixed(1);
            document.getElementById('lostWeight').innerText = (lost >= 0 ? '-' : '+') + Math.abs(lost) + ' kg';
            
            const totalToLose = prof.initial_weight - prof.target_weight;
            const pct = Math.max(0, Math.min(100, Math.round(((prof.initial_weight - latest) / totalToLose) * 100)));
            document.getElementById('progFill').style.width = pct + '%';

            // BMI
            const h = prof.height_cm / 100;
            const bmi = (latest / (h * h)).toFixed(1);
            document.getElementById('bmiBadge').innerHTML = '<div style="color: #10B981; font-weight: bold; font-size: 18px;">BMI ' + bmi + '</div><div style="font-size: 12px; color: #94A3B8;">Lý tưởng</div>';

            // Stats
            const totalWorkoutCal = data.workouts.reduce((s, w) => s + w.calories, 0);
            const totalRunCal = data.runs.reduce((s, r) => s + r.calories, 0);
            document.getElementById('todayCal').innerText = Math.round(totalWorkoutCal + totalRunCal) + ' kcal';
            const km = data.runs.reduce((s, r) => s + r.distance_km, 0);
            document.getElementById('totalKm').innerText = km.toFixed(1) + ' km';
            const mins = data.workouts.reduce((s, w) => s + w.duration_min, 0);
            document.getElementById('totalMins').innerText = mins + ' phút';

            // Lists
            document.getElementById('weightList').innerHTML = data.weights.map(w => `
                <div class="list-item">
                    <div>
                        <b style="font-size: 16px;">${w.weight} kg</b>
                        <div style="font-size: 11px; color: #94A3B8;">${w.date} ${w.note ? '• ' + w.note : ''}</div>
                    </div>
                </div>
            `).join('');

            document.getElementById('workoutList').innerHTML = data.workouts.map(w => `
                <div class="list-item">
                    <div>
                        <b>${w.title}</b>
                        <div style="font-size: 11px; color: #94A3B8;">${w.date} • ${w.duration_min} phút</div>
                    </div>
                    <span style="color: var(--orange); font-weight: bold;">${Math.round(w.calories)} kcal</span>
                </div>
            `).join('');

            document.getElementById('runList').innerHTML = data.runs.map(r => `
                <div class="list-item">
                    <div>
                        <b>${r.distance_km} km (${r.duration_min} phút)</b>
                        <div style="font-size: 11px; color: #94A3B8;">${r.date} • Pace ${r.pace}</div>
                    </div>
                    <span style="color: var(--cyan); font-weight: bold;">${Math.round(r.calories)} kcal</span>
                </div>
            `).join('');
        }

        document.getElementById('weightForm').onsubmit = async (e) => {
            e.preventDefault();
            const w = document.getElementById('wInput').value;
            const note = document.getElementById('noteInput').value;
            await fetch('/api/add-weight', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({weight: parseFloat(w), note: note})
            });
            document.getElementById('wInput').value = '';
            document.getElementById('noteInput').value = '';
            load();
        };

        load();
    </script>
</body>
</html>
"""

class RequestHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/" or self.path == "/index.html":
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(HTML_TEMPLATE.encode("utf-8"))
        elif self.path == "/api/data":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(get_data()).encode("utf-8"))
        else:
            self.send_error(404)

    def do_POST(self):
        if self.path == "/api/add-weight":
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length).decode("utf-8")
            data = json.loads(body)
            w = float(data.get("weight", 0))
            note = data.get("note", "")
            if w > 20:
                conn = sqlite3.connect(DB_PATH)
                c = conn.cursor()
                c.execute("INSERT INTO weights (weight_kg, timestamp, note) VALUES (?, ?, ?)",
                          (w, int(time.time() * 1000), note))
                conn.commit()
                conn.close()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status":"ok"}')
        else:
            self.send_error(404)

def run_server(port=8080):
    init_db()
    with socketserver.TCPServer(("0.0.0.0", port), RequestHandler) as httpd:
        print(f"🚀 FitTrack Companion đang chạy tại: http://localhost:{port}")
        print("💡 Trên Android, chạy: termux-open-url http://localhost:8080")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nĐã dừng server.")

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "add-weight":
        init_db()
        w = float(sys.argv[2])
        note = sys.argv[3] if len(sys.argv) > 3 else "Termux CLI"
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute("INSERT INTO weights (weight_kg, timestamp, note) VALUES (?, ?, ?)",
                  (w, int(time.time() * 1000), note))
        conn.commit()
        conn.close()
        print(f"✅ Đã ghi cân nặng {w} kg vào cơ sở dữ liệu.")
    elif len(sys.argv) > 1 and sys.argv[1] == "stats":
        init_db()
        data = get_data()
        latest = data["weights"][0]["weight"] if data["weights"] else data["profile"]["initial_weight"]
        print(f"📊 FitTrack Thống Kê:")
        print(f" • Cân nặng hiện tại: {latest} kg")
        print(f" • Mục tiêu: {data['profile']['target_weight']} kg")
        print(f" • Tổng chạy bộ: {sum(r['distance_km'] for r in data['runs']):.1f} km")
    else:
        run_server()
