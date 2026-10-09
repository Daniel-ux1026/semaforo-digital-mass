FROM node:24.18.0-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci --ignore-scripts
COPY angular.json tsconfig*.json ngsw-config.json ./
COPY src src
COPY public public
RUN npm run build
FROM nginx:1.30.5-alpine
COPY infra/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/semaforo-digital-mass/browser /usr/share/nginx/html
