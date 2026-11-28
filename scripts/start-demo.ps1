param([int]$DatabasePort = 15432, [switch]$Verify)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function New-Secret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
function Check-Exit($Message) { if ($LASTEXITCODE -ne 0) { throw $Message } }
$env:CSS_JWT_SECRET = New-Secret
$env:CSS_REQUESTER_PASSWORD = New-Secret
$env:CSS_APPROVER_PASSWORD = New-Secret
$env:SPRING_DATASOURCE_PASSWORD = New-Secret
$env:CSS_DB_PASSWORD = $env:SPRING_DATASOURCE_PASSWORD
$env:SPRING_DATASOURCE_USERNAME = 'css'
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:$DatabasePort/css"
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SERVER_ADDRESS = '127.0.0.1'
$container = 'purchase-tracker-demo-db'
$names = docker container ls -a --format '{{.Names}}'
Check-Exit 'Start Docker Desktop, then retry.'
if ($names -contains $container) {
    docker start $container | Out-Null
    Check-Exit 'Cannot start the demo database.'
    $mapping = docker port $container 5432/tcp
    Check-Exit 'Cannot read the database port.'
    if ($mapping -ne "127.0.0.1:$DatabasePort") {
        throw "Database must be bound only to 127.0.0.1:$DatabasePort. Current mapping: $mapping"
    }
} else {
    docker run -d --name $container -e POSTGRES_DB=css -e POSTGRES_USER=css -e "POSTGRES_PASSWORD=$env:SPRING_DATASOURCE_PASSWORD" -p "127.0.0.1:${DatabasePort}:5432" -v purchase-tracker-demo-data:/var/lib/postgresql/data postgres:16-alpine | Out-Null
    Check-Exit 'Database port is unavailable. Retry with -DatabasePort and a free port.'
}
$ready = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    docker exec $container pg_isready -U css -d css *> $null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw 'Database did not become ready.' }
# Send the generated Base64 password over stdin; never print it or add it to git.
"ALTER ROLE css PASSWORD '$env:SPRING_DATASOURCE_PASSWORD';" | docker exec -i $container psql -U css -d css -v ON_ERROR_STOP=1 | Out-Null
Check-Exit 'Cannot rotate the local database password.'
Write-Host 'Local demo: http://localhost:8080/'
Write-Host "requester password: $env:CSS_REQUESTER_PASSWORD"
Write-Host "approver password:  $env:CSS_APPROVER_PASSWORD"
Write-Host 'Passwords and signing key change on every launcher run. Keep this terminal private.'
if ($Verify) {
    mvn verify
    Check-Exit 'Verification failed.'
}
mvn spring-boot:run
Check-Exit 'Application stopped with an error.'
