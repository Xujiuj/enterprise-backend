param(
    [string] $EnvFile = (Join-Path $PSScriptRoot ".env"),
    [string] $SqlServer = $env:FX_SQLSERVER_HOST,
    [string] $SqlUser = $(if ($env:FX_SQLSERVER_USER) { $env:FX_SQLSERVER_USER } else { "sa" }),
    [string] $SqlPassword = $env:FX_SQLSERVER_PASSWORD,
    [string] $Database = $(if ($env:FX_ENTERPRISE_DATABASE) { $env:FX_ENTERPRISE_DATABASE } else { "enterprise" })
)

$ErrorActionPreference = "Stop"
$WorkspaceRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$SqlDir = Join-Path $WorkspaceRoot "deploy\sqlserver"

function Get-EnvValue {
    param([string] $Name, [string] $Default = "")
    if (Test-Path -LiteralPath $EnvFile) {
        foreach ($line in Get-Content -LiteralPath $EnvFile -Encoding UTF8) {
            $trimmed = $line.Trim()
            if (-not $trimmed -or $trimmed.StartsWith("#")) { continue }
            $idx = $trimmed.IndexOf("=")
            if ($idx -lt 1) { continue }
            if ($trimmed.Substring(0, $idx).Trim() -eq $Name) {
                return $trimmed.Substring($idx + 1).Trim()
            }
        }
    }
    return $Default
}

if (-not (Get-Command sqlcmd -ErrorAction SilentlyContinue)) {
    throw "Missing required command: sqlcmd"
}
if (-not $SqlServer) { $SqlServer = Get-EnvValue "FX_SQLSERVER_HOST" "" }
if (-not $SqlPassword) { $SqlPassword = Get-EnvValue "FX_SQLSERVER_PASSWORD" "" }
if (-not $Database) { $Database = Get-EnvValue "FX_ENTERPRISE_DATABASE" "enterprise" }
if (-not $SqlServer -or -not $SqlPassword) {
    throw "FX_SQLSERVER_HOST and FX_SQLSERVER_PASSWORD are required."
}

Write-Host "==> Ensuring database $Database exists" -ForegroundColor Cyan
$ensureDbQuery = "IF DB_ID(N'$Database') IS NULL EXEC(N'CREATE DATABASE [$Database] COLLATE Chinese_PRC_CI_AS');"
sqlcmd -S $SqlServer -U $SqlUser -P $SqlPassword -C -b -d master -Q $ensureDbQuery
if ($LASTEXITCODE -ne 0) { throw "Failed to ensure database exists" }

Write-Host "==> Running enterprise initialization data" -ForegroundColor Cyan
Push-Location $SqlDir
sqlcmd -S $SqlServer -U $SqlUser -P $SqlPassword -C -b -d $Database -i "enterprise-init.sql"
if ($LASTEXITCODE -ne 0) { throw "enterprise initialization failed" }
sqlcmd -S $SqlServer -U $SqlUser -P $SqlPassword -C -b -d $Database -i "session-timeout-1h.sql"
if ($LASTEXITCODE -ne 0) { throw "session timeout initialization failed" }
Pop-Location

Write-Host "Enterprise SQL Server initialization complete."
