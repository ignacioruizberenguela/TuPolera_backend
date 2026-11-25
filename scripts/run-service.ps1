<#
 Ejecuta un servicio cargando las variables del archivo .env (Windows PowerShell).
 Uso (desde la carpeta backend):
     .\scripts\run-service.ps1 auth-service
     .\scripts\run-service.ps1 api-gateway
     .\scripts\run-service.ps1 auth-service test      # ejecuta las pruebas
#>
param(
    [Parameter(Mandatory = $true)][string]$Servicio,
    [string]$Accion = "run"
)

$raiz = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $raiz ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "No existe $envFile. Copia .env.example como .env y completa los valores."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $linea = $_.Trim()
    if ($linea -and -not $linea.StartsWith("#") -and $linea.Contains("=")) {
        $partes = $linea.Split("=", 2)
        [System.Environment]::SetEnvironmentVariable($partes[0].Trim(), $partes[1].Trim(), "Process")
    }
}

Set-Location (Join-Path $raiz $Servicio)

if ($Accion -eq "test") {
    & .\mvnw.cmd test
} else {
    & .\mvnw.cmd spring-boot:run
}
