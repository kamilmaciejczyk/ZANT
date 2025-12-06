# Multi-stage build for Angular frontend
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the Angular app
RUN npm run build

# Runtime stage with nginx
FROM nginx:alpine

# Copy custom nginx config
COPY nginx.conf /etc/nginx/nginx.conf

# Copy built app from build stage
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Expose port
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
