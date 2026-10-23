#!/usr/bin/env bash
# 停掉本机 Metal 精排服务。先 TERM 让 llama-server 自己收摊，等不到再 KILL。
set -eu
HERE="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$HERE/llama.pid"

[ -f "$PID_FILE" ] || { echo "没有 pid 文件，服务未由本脚本启动"; exit 0; }
pid="$(cat "$PID_FILE")"
if ! kill -0 "$pid" 2>/dev/null; then
  echo "pid=$pid 已不存在，清掉 pid 文件"; rm -f "$PID_FILE"; exit 0
fi

kill "$pid"
for i in $(seq 1 15); do
  kill -0 "$pid" 2>/dev/null || { echo "已停止（pid=$pid）"; rm -f "$PID_FILE"; exit 0; }
  sleep 1
done
echo "TERM 后 15s 仍未退出，发 KILL" >&2
kill -9 "$pid" 2>/dev/null || true
rm -f "$PID_FILE"
echo "已强制停止（pid=$pid）"
