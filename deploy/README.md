# Enterprise backend deployment

This directory contains the standalone delivery scripts for the enterprise backend only.

## Files

- `.env.example`: copy to `.env` and fill enterprise database, Redis, and vendor API endpoint settings.
- `build-image.ps1` / `build-image.sh`: build the backend JAR and Docker image.
- `init-sqlserver.ps1` / `init-sqlserver.sh`: create the enterprise database if needed and load maintained initialization data.
- `deploy.ps1` / `deploy.sh`: build, optionally initialize SQL Server, and start the enterprise backend compose service.
- `docker-compose.yml`: enterprise backend plus its Redis sidecar.

## Windows

```powershell
Copy-Item .env.example .env
.\deploy.ps1 -Fresh
```

## Linux

```bash
cp .env.example .env
chmod +x ./*.sh
FRESH=true ./deploy.sh
```

If the vendor backend is deployed separately, keep both projects on the same `FX_DOCKER_NETWORK` or set `ENTERPRISE_VENDOR_OPEN_BASE_URL` to a reachable vendor backend URL.
