@echo off
rem Servidor local de la PWA (http://localhost:8090). Cierra esta ventana para detenerlo.
cd /d "%~dp0pwa"
start "" http://localhost:8090/
python -m http.server 8090
