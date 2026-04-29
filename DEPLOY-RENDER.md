# Deploying to Render.com

This guide covers two ways to deploy the Banking Transaction Analyzer on [Render](https://render.com):

1. **From GitHub** — Render builds from your Dockerfile on every push
2. **Manual** — Push a prebuilt Docker image and deploy it

---

## Prerequisites

- A [Render account](https://dashboard.render.com/register)
- Repository pushed to GitHub (for Option 1)
- Docker installed locally (for Option 2)

---

## Option 1: Deploy from GitHub (Recommended)

Render connects to your GitHub repo, detects the Dockerfile, and builds + deploys automatically on every push.

### Steps

1. **Sign in** to the [Render Dashboard](https://dashboard.render.com/)

2. Click **+ New** → **Web Service**

3. Under **Source Code**, select **Connect a repository**
   - Connect your GitHub account if not already linked
   - Select the `jdbc-banking-analyzer` repository

4. Configure the service:

   | Setting | Value |
   |---------|-------|
   | Name | `banking-analyzer` |
   | Region | Choose nearest to your users |
   | Branch | `main` |
   | Runtime | **Docker** |
   | Instance Type | Free or Starter |

5. Under **Advanced**, set:

   | Setting | Value |
   |---------|-------|
   | Dockerfile Path | `./Dockerfile` |
   | Docker Command | _(leave empty — uses CMD from Dockerfile)_ |

6. Add a **Disk** for persistent H2 database:

   | Setting | Value |
   |---------|-------|
   | Name | `banking-data` |
   | Mount Path | `/app/data` |
   | Size | 1 GB |

7. Click **Deploy Web Service**

### What happens

- Render clones your repo and builds the Docker image using the multi-stage Dockerfile
- Tomcat starts on port 8080 (Render auto-detects the exposed port)
- On subsequent pushes to `main`, Render auto-redeploys

### Access

Your app will be available at:
```
https://banking-analyzer-xxxx.onrender.com/transaction-analyzer
```

---

## Option 2: Deploy a Prebuilt Docker Image (Manual)

Push your image to a container registry (Docker Hub or GitHub Container Registry), then tell Render to pull it.

### Step 1 — Build for linux/amd64

Render requires `linux/amd64` images:

```bash
docker build --platform linux/amd64 -t banking-analyzer .
```

### Step 2 — Push to Docker Hub

```bash
# Tag the image
docker tag banking-analyzer YOUR_DOCKERHUB_USERNAME/banking-analyzer:latest

# Log in and push
docker login
docker push YOUR_DOCKERHUB_USERNAME/banking-analyzer:latest
```

**Alternative: GitHub Container Registry**
```bash
# Log in to ghcr.io
echo $GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# Tag and push
docker tag banking-analyzer ghcr.io/YOUR_GITHUB_USERNAME/banking-analyzer:latest
docker push ghcr.io/YOUR_GITHUB_USERNAME/banking-analyzer:latest
```

### Step 3 — Create Web Service on Render

1. **Sign in** to the [Render Dashboard](https://dashboard.render.com/)

2. Click **+ New** → **Web Service**

3. Under **Source Code**, click **Existing Image**

4. Enter your image URL:
   - Docker Hub: `docker.io/YOUR_DOCKERHUB_USERNAME/banking-analyzer:latest`
   - GitHub: `ghcr.io/YOUR_GITHUB_USERNAME/banking-analyzer:latest`

5. If the image is private, click **Add credential**:
   - **Docker Hub**: username + [personal access token](https://hub.docker.com/settings/security)
   - **GitHub**: username + token with `read:packages` scope

6. Click **Connect**, then configure:

   | Setting | Value |
   |---------|-------|
   | Name | `banking-analyzer` |
   | Region | Choose nearest |
   | Instance Type | Free or Starter |

7. Add a **Disk**:

   | Setting | Value |
   |---------|-------|
   | Name | `banking-data` |
   | Mount Path | `/app/data` |
   | Size | 1 GB |

8. Click **Deploy Web Service**

### Redeploying after image updates

Image-backed services don't auto-redeploy. After pushing a new image:
- Go to Dashboard → your service → **Manual Deploy** → **Deploy latest reference**
- Or use the [Deploy Hook URL](https://render.com/docs/deploy-hooks) for CI/CD automation

---

## Environment Variables (Optional)

| Variable | Purpose | Default |
|----------|---------|---------|
| `JAVA_OPTS` | JVM options | `-Duser.home=/app/data` (set in Dockerfile) |
| `PORT` | Render provides this automatically | `8080` |

---

## Persistent Storage

The H2 database writes to `~/banking_db` which resolves to `/app/data/banking_db` inside the container (set via `JAVA_OPTS`). The Render disk mounted at `/app/data` ensures data persists across deploys and restarts.

> **Note:** Render free-tier disks are limited. For production, consider switching to an external database (e.g., Render PostgreSQL).

---

## Verify Deployment

After deployment completes, test the API:

```bash
# Health check — should return JSON array
curl https://YOUR-APP.onrender.com/transaction-analyzer/api/transactions

# Create a transaction
curl -X POST https://YOUR-APP.onrender.com/transaction-analyzer/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"ACC001","transactionType":"CREDIT","amount":5000.00,"transactionDate":"2026-04-29 10:00:00","description":"Test","balanceAfter":5000.00}'
```

Access the UI at: `https://YOUR-APP.onrender.com/transaction-analyzer`

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Deploy fails with "Image pull failed" | Verify image URL and credentials |
| App shows 502/503 after deploy | Wait 1-2 min for Tomcat startup; check Render logs |
| H2 data lost after redeploy | Ensure disk is mounted at `/app/data` |
| Build fails on Render | Check Dockerfile uses `linux/amd64` compatible base images |
| Port not detected | Render reads `EXPOSE 8080` from Dockerfile — no action needed |
