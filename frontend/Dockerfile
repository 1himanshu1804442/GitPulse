# ==============================================================================
# GitPulse Frontend - Multi-Stage Dockerfile
# Stage 1: Build React 18 + Vite static bundle with Node.js 20 Alpine
# Stage 2: Serve optimized static assets with Nginx Alpine
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application
# ------------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Cache package manifests first for faster docker build layers
COPY package*.json ./
RUN npm ci || npm install

# Copy source files and build production distribution
COPY . .
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Nginx Web Server Runtime
# ------------------------------------------------------------------------------
FROM nginx:alpine AS runner

# Remove default nginx welcome page
RUN rm -rf /usr/share/nginx/html/*

# Copy built distribution from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration with reverse proxy and SPA routing
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose standard HTTP port
EXPOSE 80

# Healthcheck to verify Nginx status
HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
