# Desktop Installers (Windows / macOS / Arch Linux)

This project uses JavaFX + Spring Boot. Installers must be built **per OS**.

## 1) Configure API URL (VPS)
The desktop app now reads the API URL in this order:
1) Java system property: `-Ddismal.api.url=...`
2) Environment variable: `DISMAL_API_URL`
3) Default: `http://localhost:8080`

For production installers, pass the VPS URL with `-Ddismal.api.url`.

## 2) Build the fat jar
From repo root:
```
./mvnw -f apps/desktop/pom.xml -DskipTests package
```
Jar output:
```
apps/desktop/target/dismal-desktop-0.1.0.jar
```

## 3) Windows (.exe)
Run on Windows host or Windows CI runner:
```
./scripts/packaging/windows/build-desktop.ps1 -ApiUrl http://VPS_IP:9001 -AppVersion 1.0.0
```
Output:
```
apps/desktop/target/installer/Dismal-Desktop-Setup.exe
```

## 4) macOS (.dmg)
Run on macOS host or macOS CI runner:
```
APP_VERSION=1.0.0 ./scripts/packaging/macos/build-desktop.sh http://VPS_IP:9001
```
Output:
```
apps/desktop/target/installer/Dismal-Desktop.dmg
```

## 5) Arch Linux (.pkg.tar.zst)
Run on Arch host:
```
APP_VERSION=1.0.0 ./scripts/packaging/arch/build-desktop.sh http://VPS_IP:9001
```
Output:
```
apps/desktop/target/installer/dismal-desktop-0.1.0-1-x86_64.pkg.tar.zst
```

## 6) Notes
- `jpackage` requires **JDK 21**.
- Build on each OS (Windows/macOS/Arch). Cross-build is not supported by jpackage.
- If you need to change the API URL later, set `DISMAL_API_URL` in the OS environment.
- The packaging scripts use Spring Boot's `org.springframework.boot.loader.launch.JarLauncher` with `-Dloader.main=com.dismal.desktop.DismalDesktopApp`.
