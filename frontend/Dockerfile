# Build 前端
FROM node:20 AS build
WORKDIR /app

# 將 Render 傳進來的變數宣告成 build ARG，並設為 ENV 給後續 npm build 用
ARG VITE_API_BASE
ENV VITE_API_BASE=${VITE_API_BASE}

COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Run：用 "serve" 啟動靜態檔
FROM node:20
WORKDIR /app
RUN npm i -g serve
COPY --from=build /app/dist ./dist
ENV PORT=10000
EXPOSE 10000
CMD ["sh", "-c", "serve -s dist -l ${PORT}"]