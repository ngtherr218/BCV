FROM node:22-alpine AS web
WORKDIR /web
COPY web/package*.json ./
RUN npm ci
COPY web/ ./
ARG VITE_GOONG_MAPTILES_KEY
ENV VITE_GOONG_MAPTILES_KEY=$VITE_GOONG_MAPTILES_KEY
RUN npm run build

FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src
COPY backend/Bcv.Api/ ./Bcv.Api/
RUN dotnet publish Bcv.Api/Bcv.Api.csproj -c Release -o /out /p:UseAppHost=false
COPY --from=web /web/dist/ /out/wwwroot/

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=build /out/ ./
ENV ASPNETCORE_URLS=http://0.0.0.0:10000
EXPOSE 10000
ENTRYPOINT ["dotnet", "Bcv.Api.dll"]
