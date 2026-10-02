# Monorepo Setup Instructions (Codex)

## Objetivo
Crear un monorepo con:
- `apps/web` (Next.js)
- `apps/android` (Kotlin + Jetpack Compose, minSdk 24)
- `apps/desktop` (JavaFX)
- `packages/shared` (cliente API/modelos compartidos)

## Estructura
```
Dismal/
  apps/
    web/
    android/
    desktop/
  packages/
    shared/
```

## Paso 1: Crear carpetas base
```
mkdir -p apps/web apps/android apps/desktop packages/shared
```

## Paso 2: Web (Next.js)
Dentro de `apps/web`, usar el setup de Next.js:
```
cd apps/web
npx create-next-app@latest .
```
- Responder con TypeScript = Yes
- ESLint = Yes
- App Router = Yes
- Tailwind = optional (si se quiere rapido UI)

## Paso 3: Android (Android Studio)
1) Abrir Android Studio
2) New Project > Empty Compose Activity
3) Location: `apps/android`
4) Language: Kotlin
5) Minimum SDK: 24
6) Package name (applicationId): `com.dismal.app`
7) Finish

Nota: Android Studio generara `settings.gradle` y `build.gradle` dentro de `apps/android`.

## Paso 4: Desktop (JavaFX)
Recomendado Gradle (standalone):

En `apps/desktop`:
```
cd apps/desktop
mkdir -p src/main/java/com/dismal/desktop src/main/resources
```
Crear `apps/desktop/build.gradle` con JavaFX plugin (minimo ejemplo):
```
plugins {
    id 'application'
    id 'org.openjfx.javafxplugin' version '0.1.0'
}

group = 'com.dismal'
version = '1.0.0'

repositories {
    mavenCentral()
}

javafx {
    version = '21'
    modules = [ 'javafx.controls', 'javafx.fxml' ]
}

application {
    mainClass = 'com.dismal.desktop.MainApp'
}
```

Crear `apps/desktop/src/main/java/com/dismal/desktop/MainApp.java`:
```
package com.dismal.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("Dismal Desktop");
        Scene scene = new Scene(new StackPane(label), 800, 600);
        stage.setTitle("Dismal");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## Paso 5: Shared (cliente API)
Crear `packages/shared` para modelos y cliente API. Puede ser:
- JS/TS para compartir entre web y Android (via OpenAPI/SDK)
- O definir DTOs y un cliente HTTP en TS

Ejemplo base (TS):
```
cd packages/shared
npm init -y
```

## Paso 6: Documentacion
Actualizar README principal con como ejecutar:
- Web: `cd apps/web && npm run dev`
- Android: abrir en Android Studio
- Desktop: `cd apps/desktop && ./gradlew run`

## Paso 7: Configuracion de entorno
- Definir variable de entorno para base URL del backend:
  - Web: `NEXT_PUBLIC_API_BASE_URL`
  - Android/Desktop: archivo de configuracion local (TODO).
- No commitear secretos (JWT/AES).

## API clients (OpenAPI)
### Source of truth
- Mantener un spec OpenAPI como fuente de verdad del API.
- Versionar el spec junto al backend.

### Generacion por plataforma
- Web (TypeScript): generar en `packages/shared/src/generated`.
- Android (Kotlin): generar en `apps/android/app/src/main/java/.../generated`.
- Desktop (Java): generar en `apps/desktop/src/main/java/.../generated`.
- No modificar a mano el codigo generado.

### Versionado
- API versionada por ruta (`/api/v1`).
- Clientes versionados con semver y alineados con el backend.

## Production Readiness (ops)
- Secrets: usar variables de entorno o un vault; no commitear `application.properties` con credenciales reales.
- Logging: considerar JSON logs y correlacionar requests (request-id).
- Health checks: habilitar Spring Boot Actuator y exponer `/actuator/health`.
- Metrics/tracing: recomendar Prometheus + OpenTelemetry (opcional).

## Runbook (ops)
### Correr en local
1) Levantar PostgreSQL y crear la DB `dismal_db`.
2) Configurar `spring.datasource.*`, JWT/AES y `integrations.n8n.webhook-url`.
3) Ejecutar: `./mvnw spring-boot:run`.

### Validar compra end-to-end
1) Crear usuario y hacer login.
2) Crear software y licencia.
3) Ejecutar compra (endpoint publico TODO).
4) Verificar orden creada y activacion consumida.

### Validar webhook n8n
1) Apuntar `integrations.n8n.webhook-url` a un flujo n8n real.
2) Ejecutar compra.
3) Confirmar recepcion en n8n.

## Testing strategy
- Unit tests (servicios):
  - CheckoutService: seleccion de licencia, consumo de activacion, creacion de orden.
  - CustomerService: filtrado de `enabled`.
- Integration tests:
  - PostgreSQL con Testcontainers (recomendado).
  - Escenarios: compras concurrentes, sin stock, licencia agotada, cliente deshabilitado no listado.
- Comando base:
  - `./mvnw test`

## CI recommendations
- Ejecutar `./mvnw test` en cada PR.
- Lint/format: agregar solo si el repo lo define (recomendado).
- Build artifacts: `./mvnw -DskipTests package` para generar el JAR.

## Nota importante
- El backend ya esta en la raiz del repo.
- Estos frontends consumen el backend via HTTP (base URL configurable por entorno).
