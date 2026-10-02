# Dismal Desktop (JavaFX)

## Run (dev)

```bash
cd desktop
mvn javafx:run
```

## API URL

Set the backend URL with:

```bash
export DISMAL_API_URL="http://localhost:8080"
```

## Packaging (jpackage)

Run from `desktop/` after building a jar.

```bash
mvn -q -DskipTests package
```

Scripts:

```bash
./package/macos.sh 0.1.0
./package/linux.sh 0.1.0
```

On Windows (PowerShell):

```powershell
./package/windows.ps1 -Version 0.1.0
```

### Windows

```bash
jpackage \
  --name DismalDesktop \
  --input target \
  --main-jar dismal-desktop-0.1.0.jar \
  --type exe
```

### macOS

```bash
jpackage \
  --name DismalDesktop \
  --input target \
  --main-jar dismal-desktop-0.1.0.jar \
  --type dmg
```

### Linux

```bash
jpackage \
  --name DismalDesktop \
  --input target \
  --main-jar dismal-desktop-0.1.0.jar \
  --type deb
```

Adjust `--type` to `rpm` if needed.
