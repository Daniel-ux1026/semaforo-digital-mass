FROM node:24.18.0-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci --ignore-scripts

COPY angular.json tsconfig*.json ngsw-config.json ./
COPY src src
COPY public public
RUN npm run build

FROM nginx:1.30.5-alpine
ENV PORT=8080
ENV NGINX_ENVSUBST_FILTER="^(PORT|API_HOST|CHATBOT_HOST)$"

COPY infra/railway/nginx.conf /etc/nginx/templates/default.conf.template
COPY infra/railway/19-check-config.sh /docker-entrypoint.d/19-check-config.sh
RUN chmod +x /docker-entrypoint.d/19-check-config.sh

COPY --from=build /app/dist/semaforo-digital-mass/browser/ /usr/share/nginx/html/

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
