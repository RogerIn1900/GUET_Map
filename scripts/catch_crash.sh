#!/bin/bash
# 抓 com.example.guet_map 实时崩溃栈
# 用法： ./scripts/catch_crash.sh
# Ctrl+C 退出

PKG=com.example.guet_map
echo "=== 清空 logcat 缓存 ==="
adb logcat -c

echo "=== 启动 $PKG，等待闪退 ==="
adb shell am start -n "$PKG/.MainActivity" 2>/dev/null

echo "=== 实时抓取 AndroidRuntime + GUETMapApp 崩溃栈（30s 窗口）==="
timeout 30 adb logcat -v time AndroidRuntime:E GUETMapApp:E "*:S" | tee /tmp/guet_crash_$(date +%H%M%S).log

echo
echo "=== last_crash.log（如有）==="
adb shell run-as $PKG cat files/last_crash.log 2>/dev/null \
  || echo "（无 / 不可读）"
