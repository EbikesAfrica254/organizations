# Secrets Handling

## Local Development

Secrets are provided via a gitignored `.env` file.

```bash
cp .env.example .env
# fill in values locally
```

* `.env` **must never be committed**
* Used for IntelliJ / local runs only
* Contains values like:

    * `SPRING_DATASOURCE_PASSWORD`
    * `SPRING_RABBITMQ_PASSWORD`

## Upstream Environments

Secrets are **not stored in `.env`**.

They are managed via:

```
AWS SSM Parameter Store
→ Deployment contract (infrastructure repo)
→ Injected as runtime secrets
```

Examples:

```
/ebikes/dev/app/routing.db.password
/ebikes/dev/app/routing.rabbitmq.password
/ebikes/dev/app/routing.redis.password
```

## Key Principle

* **Local = convenience (.env)**
* **Upstream = secure (SSM + runtime injection)**

The application configuration is designed to support both without changes.