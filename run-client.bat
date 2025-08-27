@echo off
chcp 65001 >nul
title client.app.ClientConsole
echo === Starting Console Client ===
java -cp out client.app.ClientConsole
echo.
echo (Client stopped)
pause
