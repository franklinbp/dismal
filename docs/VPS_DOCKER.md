# VPS Deployment with Docker and Easypanel

This production setup runs PostgreSQL on the private Compose network, while
Spring Boot and Next.js join the Easypanel proxy network. Traefik is the only
public listener on ports `80` and `443`; ports `9001` and `6001` remain bound
to host loopback for diagnostics.

## 1) Clone repo on VPS
```
git clone https://github.com/franklinbp/Dismal.git
cd Dismal
```

## 2) Configure environment
```
cp .env.example .env
nano .env
```
Fill at least:
- DB_PASSWORD
- JWT_SECRET
- AES_KEY
- ADMIN_IMPORT_DEFAULT_PASSWORD

## 3) Deploy
```
chmod +x scripts/deploy_easypanel.sh
./scripts/deploy_easypanel.sh
```

Do not run `docker compose down -v`; that command deletes database storage.

## 4) Public endpoints

- `https://dismal.vip/login`
- `https://www.dismal.vip/login`
- `https://api.dismal.vip/actuator/health`

## 5) Normal Git update

```
cd ~/Dismal
git pull --ff-only origin main
./scripts/deploy_easypanel.sh
```

The deploy script waits for both application services, preserves the previous
Traefik route as `dismal.yml.backup`, and never connects PostgreSQL to the
proxy network.
