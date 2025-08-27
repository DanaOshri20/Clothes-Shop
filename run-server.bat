@echo off
chcp 65001 >nul
title StoreServer
echo === Starting StoreServer on port 5050 ===
java -cp out server.app.StoreServer 5050
echo.
echo (Server stopped)
pause
