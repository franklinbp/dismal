# VPS Deployment (Systemd + Nginx)

This is a minimal, production-leaning checklist and templates for a single VPS.

## Environment variables

Required:
- DB_URL
- DB_USER
- DB_PASSWORD
- JWT_SECRET (Base64 string)
- AES_KEY (Base64 string)
- ADMIN_IMPORT_DEFAULT_PASSWORD

Optional:
- PORT (default 8080)
- JWT_EXPIRATION_MS
- N8N_WEBHOOK_URL
- N8N_SECRET
- SALES_FIXED_COST_GLOBAL
- SALES_FIXED_COST_MODE
- OUTBOX_DISPATCH_ENABLED
- OUTBOX_DISPATCH_RATE_MS
- OUTBOX_DISPATCH_MAX_ATTEMPTS
- ADMIN_AR_BLOCK_DAYS
- ADMIN_RESET_PASSWORD_ENABLED
- INVOICE_PDF_BASE_URL
- AR_OVERDUE_CRON

## Systemd unit

Save as `/etc/systemd/system/dismal.service`:

```ini
[Unit]
Description=Dismal Backend
After=network.target

[Service]
Type=simple
User=dismal
Group=dismal
WorkingDirectory=/opt/dismal
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=PORT=8080
EnvironmentFile=/etc/dismal/dismal.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/dismal/Dismal-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=5
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectHome=true
ReadWritePaths=/opt/dismal

[Install]
WantedBy=multi-user.target
```

Then:
```
sudo systemctl daemon-reload
sudo systemctl enable --now dismal
sudo systemctl status dismal
```

## Environment file

Save as `/etc/dismal/dismal.env`:

```
DB_URL=jdbc:postgresql://127.0.0.1:5432/dismal_db
DB_USER=dismal
DB_PASSWORD=CHANGE_ME
JWT_SECRET=BASE64_SECRET
AES_KEY=BASE64_AES_KEY
ADMIN_IMPORT_DEFAULT_PASSWORD=CHANGE_ME
```

## Nginx reverse proxy

Save as `/etc/nginx/sites-available/dismal`:

```nginx
server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Then:
```
sudo ln -s /etc/nginx/sites-available/dismal /etc/nginx/sites-enabled/dismal
sudo nginx -t
sudo systemctl reload nginx
```

## Quick smoke checks

- `curl -i http://127.0.0.1:8080/actuator/health` (if Actuator is added)
- `curl -i http://api.example.com/health` (if you add a health endpoint)

## Clean deploy rules

Do not upload generated dependency or build folders from a workstation:

- `node_modules/`
- `.next/`
- `build/`
- `target/`
- `*.tsbuildinfo`
- local zip exports unless the deploy explicitly needs that artifact

For web builds, install dependencies and build on the VPS or in CI, not before copying the source tree:

```
cd /opt/dismal/apps/web
npm ci
npm run build
```

For backend deploys, build the jar in a clean environment and copy only the jar plus configuration managed by `/etc/dismal/dismal.env`. Flyway migrations in `src/main/resources/db/migration` must be included in the backend artifact before restarting the service.

