@echo off
setlocal
cd /d "%~dp0"
set BROWSER=none
set PORT=3000
cd frontend
npm run dev
