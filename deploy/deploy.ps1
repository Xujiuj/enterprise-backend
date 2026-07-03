param(
    [switch] $Build,
    [string] $ServerHost = $(if ($env:FX_DEPLOY_HOST) { $env:FX_DEPLOY_HOST } else { "124.221.155.102" }),
    [string] $SshUser = $(if ($env:FX_DEPLOY_USER) { $env:FX_DEPLOY_USER } else { "ubuntu" }),
    [string] $Password = $(if ($env:FX_DEPLOY_PASSWORD) { $env:FX_DEPLOY_PASSWORD } else { "Test0000" }),
    [string] $RemotePath = $(if ($env:FX_ENTERPRISE_BACKEND_JAR) { $env:FX_ENTERPRISE_BACKEND_JAR } else { "/opt/fx/apps/enterprise-backend/app.jar" })
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$jarPath = Join-Path $repoRoot "ruoyi-admin\target\ruoyi-admin.jar"

if ($Build) {
    Push-Location $repoRoot
    try {
        & mvn -DskipTests package
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath $jarPath)) {
    throw "backend jar not found. Run mvn -DskipTests package first: $jarPath"
}

$askpass = "$env:TEMP\ssh_askpass_enterprise_backend.bat"
Set-Content -Path $askpass -Value "@echo off`necho $Password" -Encoding ASCII
$env:SSH_ASKPASS = $askpass
$env:SSH_ASKPASS_REQUIRE = "force"
$env:DISPLAY = "localhost:0"

$sshTarget = "${SshUser}@${ServerHost}"
Write-Host "Uploading enterprise backend JAR..."
& scp -o StrictHostKeyChecking=no -o ConnectTimeout=30 $jarPath "${sshTarget}:${RemotePath}" 2>&1
if ($LASTEXITCODE -ne 0) {
    Remove-Item -Path $askpass -ErrorAction SilentlyContinue
    throw "Upload failed with exit code $LASTEXITCODE"
}

Write-Host "Restarting enterprise-backend service..."
& ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 $sshTarget "echo '$Password' | sudo -S systemctl restart enterprise-backend && systemctl is-active enterprise-backend" 2>&1
Remove-Item -Path $askpass -ErrorAction SilentlyContinue
Write-Host "Enterprise backend deployed."
