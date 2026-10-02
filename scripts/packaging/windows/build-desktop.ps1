param(
  [Parameter(Mandatory=$true)][string]$ApiUrl,
  [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"

# Build fat jar
./mvnw.cmd -f apps/desktop/pom.xml -DskipTests package

$JarPath = "apps/desktop/target/dismal-desktop-0.1.0.jar"
$OutDir = "apps/desktop/target/installer"

if (!(Test-Path $OutDir)) {
  New-Item -ItemType Directory -Path $OutDir | Out-Null
}

jpackage `
  --type exe `
  --name "Dismal Desktop" `
  --app-version "$AppVersion" `
  --input "apps/desktop/target" `
  --main-jar "dismal-desktop-0.1.0.jar" `
  --main-class "org.springframework.boot.loader.launch.JarLauncher" `
  --java-options "-Ddismal.api.url=$ApiUrl -Dloader.main=com.dismal.desktop.DismalDesktopApp" `
  --dest $OutDir `
  --win-menu `
  --win-dir-chooser `
  --win-shortcut

Write-Host "Installer ready at $OutDir"
