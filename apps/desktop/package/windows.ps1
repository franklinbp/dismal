param(
  [string]$Version = "0.1.0"
)

$Jar = "dismal-desktop-$Version.jar"

mvn -q -DskipTests package

jpackage `
  --name "DismalDesktop" `
  --input "target" `
  --main-jar $Jar `
  --type exe
