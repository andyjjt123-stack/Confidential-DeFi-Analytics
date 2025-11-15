# Build 前端
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Run：用 "serve" 依照 $PORT 提供 dist
FROM node:20
WORKDIR /app
RUN npm i -g serve
COPY --from=build /app/dist ./dist
ENV PORT=10000
EXPOSE 10000
CMD ["sh", "-c", "serve -s dist -l ${PORT}"]