# --- Etapa 1: compilar -------------------------------------------------------
FROM node:20-alpine AS construccion
WORKDIR /construccion

# npm ci y no npm install: instala exactamente lo del lock, que es lo que
# convierte una compilacion en reproducible.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# --- Etapa 2: servir ---------------------------------------------------------
FROM nginx:alpine

# Angular compila a dist/<proyecto>/browser desde la version 17
COPY --from=construccion /construccion/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
