$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot

Write-Host 'PostgreSQL must already be running on your computer.' -ForegroundColor Yellow
Write-Host 'Starting backend and frontend in separate windows...' -ForegroundColor Cyan

Start-Process powershell.exe -ArgumentList @(
    '-NoExit',
    '-ExecutionPolicy',
    'Bypass',
    '-File',
    (Join-Path $projectRoot 'backend\start.ps1')
)

Start-Process powershell.exe -ArgumentList @(
    '-NoExit',
    '-ExecutionPolicy',
    'Bypass',
    '-File',
    (Join-Path $projectRoot 'frontend\start.ps1')
)

Write-Host 'Frontend: http://localhost:4200' -ForegroundColor Green
Write-Host 'Backend:  http://localhost:8080' -ForegroundColor Green
