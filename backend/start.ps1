$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

if (Test-Path '.\mvnw.cmd') {
    .\mvnw.cmd spring-boot:run
} else {
    mvn spring-boot:run
}
