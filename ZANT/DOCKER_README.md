# ZANT - Docker Deployment Guide

This guide explains how to run the ZANT application using Docker and Docker Compose on both Mac (ARM/M1) and x86 architectures.

## Prerequisites

- Docker Desktop installed (version 20.10+)
- Docker Compose installed (version 2.0+)
- At least 4GB of available RAM
- At least 10GB of free disk space

### Install Docker Desktop

**For Mac (both Intel and Apple Silicon/M1/M2/M3):**
- Download from: https://www.docker.com/products/docker-desktop/
- Install and start Docker Desktop
- Ensure Docker Desktop is running (you'll see the whale icon in your menu bar)

**For Linux (x86_64):**
```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

## Architecture Support

The Docker images are built using multi-platform base images and will work on:
- **Mac with Apple Silicon (M1/M2/M3)** - ARM64 architecture
- **Mac with Intel processors** - x86_64 architecture  
- **Linux x86_64**
- **Windows with WSL2** - x86_64 architecture

Docker will automatically pull the correct image for your architecture.

## Quick Start

### 1. Navigate to the project directory

```bash
cd /path/to/ZANT
```

### 2. Build and start all services

```bash
docker-compose up --build
```

This command will:
- Build the backend Spring Boot application
- Build the frontend Angular application
- Start PostgreSQL database
- Start all services in the correct order

**Note:** The first build will take 5-10 minutes as it downloads dependencies and builds the applications.

### 3. Access the application

Once all services are running, you can access:

- **Frontend (Angular):** http://localhost:4200
- **Backend API:** http://localhost:8080
- **PostgreSQL Database:** localhost:5432
  - Database: `zant`
  - Username: `zant`
  - Password: `zant123`

## Common Commands

### Start services (after first build)
```bash
docker-compose up
```

### Start services in background (detached mode)
```bash
docker-compose up -d
```

### Stop services
```bash
docker-compose down
```

### Stop services and remove volumes (clean database)
```bash
docker-compose down -v
```

### View logs
```bash
# All services
docker-compose logs

# Specific service
docker-compose logs backend
docker-compose logs frontend
docker-compose logs db

# Follow logs in real-time
docker-compose logs -f
```

### Rebuild a specific service
```bash
docker-compose up --build backend
docker-compose up --build frontend
```

### Check service status
```bash
docker-compose ps
```

### Restart a service
```bash
docker-compose restart backend
docker-compose restart frontend
```

## Development Workflow with Docker

### Making changes to backend code

1. Edit your Java files in `backend/src/`
2. Rebuild the backend:
   ```bash
   docker-compose up --build backend
   ```

### Making changes to frontend code

1. Edit your TypeScript/HTML files in `frontend/src/`
2. Rebuild the frontend:
   ```bash
   docker-compose up --build frontend
   ```

## Troubleshooting

### Port already in use

If you see errors about ports already in use:

```bash
# Check what's using the ports
lsof -i :4200  # Frontend
lsof -i :8080  # Backend
lsof -i :5432  # Database

# Kill the process or change ports in docker-compose.yml
```

### Out of disk space

Docker images can take up significant space. Clean up:

```bash
# Remove unused images
docker image prune -a

# Remove unused volumes
docker volume prune

# Remove everything unused
docker system prune -a --volumes
```

### Backend not connecting to database

Make sure the database is healthy:

```bash
docker-compose logs db
docker-compose ps
```

Wait for the healthcheck to pass before the backend starts.

### Mac M1/M2/M3 specific issues

If you encounter build issues on Apple Silicon:

```bash
# Ensure Rosetta 2 is installed (for some x86 compatibility)
softwareupdate --install-rosetta

# Use platform flag if needed
docker-compose build --platform linux/arm64
```

### Frontend build fails

If the Angular build fails:

```bash
# Clear npm cache
rm -rf frontend/node_modules
rm -rf frontend/.angular

# Rebuild
docker-compose up --build frontend
```

## Production Deployment

For production deployment, consider:

1. **Environment variables:** Create a `.env` file for sensitive data
2. **Reverse proxy:** Use Nginx or Traefik in front of services
3. **SSL/TLS:** Add certificates for HTTPS
4. **Scaling:** Use Docker Swarm or Kubernetes for multi-instance deployment
5. **Monitoring:** Add Prometheus, Grafana for monitoring
6. **Backups:** Set up automated PostgreSQL backups

### Example .env file

```env
POSTGRES_DB=zant
POSTGRES_USER=zant
POSTGRES_PASSWORD=change-this-in-production
BACKEND_PORT=8080
FRONTEND_PORT=4200
```

Then modify `docker-compose.yml` to use these variables:

```yaml
environment:
  POSTGRES_DB: ${POSTGRES_DB}
  POSTGRES_USER: ${POSTGRES_USER}
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

## Application Features

### EWYP Form
- Access at: http://localhost:4200/ewyp-form
- Multi-step accident reporting form
- Save as draft functionality
- Access saved drafts via: http://localhost:4200/ewyp-form/{uuid}

### API Endpoints
- `POST /api/ewyp-reports` - Submit final report
- `POST /api/ewyp-reports/draft` - Save new draft
- `PUT /api/ewyp-reports/{id}` - Update existing draft
- `GET /api/ewyp-reports/{id}` - Get report by ID

## Support

For issues or questions:
- Check logs: `docker-compose logs`
- Verify system requirements
- Ensure Docker Desktop is running
- Check Docker version: `docker --version` and `docker-compose --version`

## License

[Your License Here]
