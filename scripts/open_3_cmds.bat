@echo off
title SmartFlash Dev Launcher

start "SmartFlash - 1. Git Commit" cmd /k "cd /d d:\Elearning-system && git status -sb"
start "SmartFlash - 2. Frontend Web (5173)" cmd /k "cd /d d:\Elearning-system\frontend-web && npm run dev"
start "SmartFlash - 3. AI-ML Service (8000)" cmd /k "cd /d d:\Elearning-system\ai-ml-service && .venv\Scripts\uvicorn.exe main:app --host 0.0.0.0 --port 8000 --reload"
