@echo off
chcp 65001 >nul
set PORT=%1
if "%PORT%"=="" set PORT=6060
title ChatServer:%PORT%
echo === Starting ChatServer on %PORT% ===
java -cp out server.app.ChatServer %PORT%
echo.
echo (Chat server stopped)
pause
