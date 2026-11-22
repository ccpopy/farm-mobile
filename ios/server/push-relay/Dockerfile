FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY src ./src
COPY certs ./certs
ENV NODE_ENV=production
EXPOSE 8787
VOLUME ["/app/data"]
CMD ["node", "src/index.js"]
