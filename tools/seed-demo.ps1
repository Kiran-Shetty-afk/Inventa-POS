[CmdletBinding()]
param(
    [switch]$Reset
)

$ErrorActionPreference = 'Stop'

if ($env:INVENTA_SEED_ENVIRONMENT -ne 'development') {
    throw 'Refusing to seed. Set INVENTA_SEED_ENVIRONMENT=development explicitly.'
}

$composeFile = Join-Path $PSScriptRoot '..\docker-compose.yml'
$sqlFile = Join-Path $PSScriptRoot 'seed-demo.sql'

if (-not (Test-Path -LiteralPath $sqlFile)) {
    throw "Seed SQL file was not found: $sqlFile"
}

if ($Reset) {
    Write-Error 'Reset is intentionally not implemented. The demo seed is repeatable and updates only its stable records.'
    exit 1
}

Write-Host 'Applying development demo data to userdb, inventorydb, salesdb, and billingdb...'
Get-Content -LiteralPath $sqlFile -Raw |
    docker compose -f $composeFile exec -T mysql sh -c 'mysql --protocol=tcp -uinventa_app -p"$MYSQL_PASSWORD" --database=userdb --batch --show-warnings'

if ($LASTEXITCODE -ne 0) {
    throw "Demo seeding failed with exit code $LASTEXITCODE."
}

Write-Host 'Demo seeding completed.'
