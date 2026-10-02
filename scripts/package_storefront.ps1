param(
    [string]$StorefrontPath = (Join-Path $PSScriptRoot "..\apps\storefront")
)

$ErrorActionPreference = "Stop"
$storefrontRoot = (Resolve-Path $StorefrontPath).Path
$deployRoot = Join-Path $storefrontRoot "deploy"

New-Item -ItemType Directory -Path $deployRoot -Force | Out-Null

$packages = @(
    @{ Country = "ecuador"; File = "dismal-ecuador-frontend-v2.zip" },
    @{ Country = "peru"; File = "dismal-peru-frontend-v2.zip" }
)

foreach ($package in $packages) {
    $source = Join-Path $storefrontRoot ("dist\" + $package.Country)
    if (-not (Test-Path (Join-Path $source "index.html"))) {
        throw "No existe el build de $($package.Country). Ejecute npm.cmd run build:all primero."
    }

    $destination = Join-Path $deployRoot $package.File
    if (Test-Path $destination) {
        Remove-Item -LiteralPath $destination -Force
    }

    $files = Get-ChildItem -LiteralPath $source -Force
    Compress-Archive -Path $files.FullName -DestinationPath $destination -CompressionLevel Optimal

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($destination)
    try {
        $entryNames = $archive.Entries | ForEach-Object { $_.FullName.Replace("\", "/") }
        foreach ($required in @("index.html", ".htaccess", "robots.txt", "sitemap.xml")) {
            if ($entryNames -notcontains $required) {
                throw "El paquete $($package.File) no contiene $required."
            }
        }
    }
    finally {
        $archive.Dispose()
    }

    Write-Host "$($package.Country): $destination"
}
