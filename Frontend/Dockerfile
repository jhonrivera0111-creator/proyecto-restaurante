# ── Stage 1: Build de React ─────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# --base / genera rutas absolutas en los assets, necesario para SPA routing en nginx.
# (La app Electron usa base "./" en vite.config.js; esta flag lo sobreescribe solo
# para el build de Docker.)
RUN npm run build:react -- --base /

# ── Stage 2: Servidor nginx ──────────────────────────────────────────────────
FROM nginx:1.27-alpine

# Copia los archivos estáticos construidos
COPY --from=builder /app/dist /usr/share/nginx/html

# Configuración: proxy /api/ → backend, SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
