#!/usr/bin/env bash
set -e

REPO_NAME=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || echo "")

echo "============================================="
echo "   FitTrack Android APK Downloader & Installer"
echo "============================================="

if [ -z "$REPO_NAME" ]; then
    echo "❌ Lỗi: Chưa phát hiện GitHub Repository. Vui lòng kiểm tra git remote."
    exit 1
fi

echo "🔍 Kiểm tra tiến trình build trên GitHub ($REPO_NAME)..."
RUN_ID=$(gh run list --workflow="build-apk.yml" --limit 1 --json databaseId -q '.[0].databaseId')

if [ -z "$RUN_ID" ]; then
    echo "⚠️ Chưa có lượt build nào được kích hoạt. Đang kích hoạt build mới..."
    gh workflow run build-apk.yml
    sleep 5
    RUN_ID=$(gh run list --workflow="build-apk.yml" --limit 1 --json databaseId -q '.[0].databaseId')
fi

echo "⏳ Đang theo dõi lượt build ID: $RUN_ID..."
gh run watch "$RUN_ID"

echo "📥 Đang tải file APK về điện thoại..."
rm -rf build_output
mkdir -p build_output
gh run download "$RUN_ID" --dir build_output

APK_FILE=$(find build_output -name "*.apk" | head -n 1)

if [ -n "$APK_FILE" ]; then
    cp "$APK_FILE" FitTrack-debug.apk
    echo ""
    echo "✅ Tải APK thành công: FitTrack-debug.apk"
    echo "🚀 Đang mở trình cài đặt Android..."
    if command -v termux-open &> /dev/null; then
        termux-open FitTrack-debug.apk
    else
        xdg-open FitTrack-debug.apk 2>/dev/null || echo "Mở file FitTrack-debug.apk trong bộ quản lý file để cài đặt."
    fi
else
    echo "❌ Không tìm thấy file APK trong artifacts tải về."
    exit 1
fi
