FROM node:24-alpine as build-stage

WORKDIR /app

# Copy package files first for better caching
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Set production environment
ENV NODE_ENV=production

# Set API URL to use relative path (proxied by nginx)
ARG VITE_API_URL=/api
ENV VITE_API_URL=${VITE_API_URL}

# Build application with environment variables baked in
RUN npm run build

FROM nginx:alpine as production-stage

# Copy built application from build stage
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Create non-root user for security
# RUN addgroup -g 1001 -S nginx && \
#     adduser -S -u 1001 -G nginx nginx && \
#     chown -R nginx:nginx /usr/share/nginx/html && \
#     chown -R nginx:nginx /var/cache/nginx && \
#     chown -R nginx:nginx /var/log/nginx && \
#     chown -R nginx:nginx /etc/nginx/conf.d && \
#     touch /var/run/nginx.pid && \
#     chown -R nginx:nginx /var/run/nginx.pid
RUN chown -R nginx:nginx /usr/share/nginx/html \
    && chown -R nginx:nginx /var/cache/nginx \
    && chown -R nginx:nginx /var/log/nginx \
    && chown -R nginx:nginx /var/run \
    && touch /var/run/nginx.pid \
    && chown nginx:nginx /var/run/nginx.pid

USER nginx

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]