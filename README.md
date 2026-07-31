# XinYueAtelier

npm run build
aws s3 sync dist/ s3://xinyueatelier-frontend --delete
aws cloudfront create-invalidation --distribution-id E3EMQENX55CGZT --paths "/*"


# Useful Commands
to check what's inside localstack S3 bucket
docker compose exec localstack awslocal s3 ls s3://xin-yue-atelier --recursive




# Atelier — Sewing Pattern Library Manager

A full-stack web application for organising and managing sewing patterns. Patterns are stored as PDFs in AWS S3, grouped into a hierarchical folder structure, and served via presigned URLs. Authentication is handled through Google OAuth2 with JWT sessions.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React + Vite |
| Backend | Java / Spring Boot |
| Database | PostgreSQL |
| File Storage | AWS S3 |
| Auth | Google OAuth2 + JWT |
| Containerisation | Docker + Docker Compose |

---

## Project Structure

```
/
├── atelier-frontend/     # React + Vite app
├── atelier-backend/      # Spring Boot API
└── docker-compose.yml
```

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- A Google OAuth2 app (Client ID + Secret)
- An AWS S3 bucket with appropriate IAM credentials

### Environment Variables

Create a `.env` file in the project root:

```env
# JWT
JWT_SECRET=your_jwt_secret

# Google OAuth
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8080/oauth2/callback/google
APP_FRONTEND_URL=http://localhost:5173

# AWS S3
S3_BUCKET=your_bucket_name
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=eu-west-2

# Frontend
VITE_API_URL=http://localhost:8080
```

### Run with Docker Compose

```bash
docker-compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| PostgreSQL | localhost:5433 |

---

## API Reference

### Folders — `/folder`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/folder` | List all root folders |
| `GET` | `/folder/{folderId}` | Get a folder by ID |
| `GET` | `/folder/{parentId}/children` | List children of a folder |
| `POST` | `/folder` | Create a root folder |
| `POST` | `/folder/{parentId}` | Create a child folder |
| `PUT` | `/folder/{id}` | Update a folder |
| `DELETE` | `/folder/{id}` | Delete a folder (cascades to subfolders and patterns) |

**Folder form fields:** `ref` (int), `title`, `garmentType`, `origin`, `level`, `image` (multipart)

### Patterns — `/patterns`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/patterns/{folderId}` | Upload a pattern PDF to a folder |
| `GET` | `/patterns/{folderId}/files` | List patterns in a folder |
| `DELETE` | `/patterns/{patternId}` | Delete a pattern |
| `GET` | `/patterns/download/{patternId}` | Redirect to S3 download URL |
| `GET` | `/patterns/preview/{patternId}` | Get a presigned S3 preview URL |


---

## Domain Model

### Folder

Folders are hierarchical (a folder can have a parent and many children). Each folder carries metadata about the patterns it contains:

- `ref` — numeric reference code
- `origin` — `DRAFTED` or `ACQUIRED`
- `level` — `BEGINNER`, `INTERMEDIATE`, or `ADVANCE`
- `garmentType` — e.g. `DRESS`, `BLAZER`, `SKIRT`, `TROUSERS` (see full list below)

Deleting a folder cascades to all subfolders and patterns within it.

### Pattern

A pattern belongs to a folder and is stored as a PDF on S3. The `pdfPath` field holds the S3 object key.

### Enums

**`GarmentType`:** `COURSE`, `ACCESSORY`, `BLAZER`, `BLOUSE`, `BRIDAL`, `DRESS`, `KNIT`, `OUTERWEAR`, `SET`, `SKIRT`, `TROUSERS`, `UNDERWEAR`

**`Level`:** `BEGINNER`, `INTERMEDIATE`, `ADVANCE`

**`PatternOrigin`:** `DRAFTED`, `ACQUIRED`

---

## Authentication

Login is via Google OAuth2. On successful login, the backend issues a JWT which the frontend stores and sends as a `Bearer` token on subsequent requests. User records store the Google `sub` claim as `googleId` for account linking.