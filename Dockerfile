FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine

COPY infra/railway/nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/semaforo-digital-mass/browser/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
