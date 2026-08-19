$ErrorActionPreference = 'Stop'
$env:CATALOG_IMPORT_ENABLED = 'true'
$env:CATALOG_IMPORT_EXIT_AFTER_RUN = 'true'
$env:SERVER_PORT = '0'
$env:DEBUG = 'false'
$env:SHOW_SQL = 'false'

Write-Host 'Importing the selected MusicBrainz catalogue...'
Write-Host 'You can stop with Ctrl+C and run this file again later. Existing data will not be duplicated.'
Push-Location $PSScriptRoot
try {
    & (Join-Path $PSScriptRoot 'mvnw.cmd') spring-boot:run

    if ($LASTEXITCODE -ne 0) {
        throw "Catalogue import failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

Write-Host 'Catalogue import command finished.'
