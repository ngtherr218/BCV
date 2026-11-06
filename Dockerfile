FROM node:22-bookworm-slim AS web
WORKDIR /src/web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
ARG VITE_GOONG_MAPTILES_KEY
RUN test -n "$VITE_GOONG_MAPTILES_KEY" || (echo 'Set VITE_GOONG_MAPTILES_KEY for the map build.' >&2; exit 1)
RUN npm run build

FROM mcr.microsoft.com/dotnet/sdk:10.0 AS api
WORKDIR /src
COPY backend/Bcv.Api/Bcv.Api.csproj backend/Bcv.Api/
COPY db/021_versioned_recommendations.sql db/
COPY db/022_community_media.sql db/
COPY db/023_place_type_features.sql db/
COPY db/024_media_moderation.sql db/
COPY db/025_place_videos.sql db/
RUN dotnet restore backend/Bcv.Api/Bcv.Api.csproj
COPY backend/Bcv.Api/ backend/Bcv.Api/
RUN dotnet publish backend/Bcv.Api/Bcv.Api.csproj -c Release --no-restore -p:UseAppHost=false -o /publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app
COPY --from=api /publish/ ./
COPY --from=web /src/web/dist/ ./wwwroot/
RUN mkdir -p /app/data && chown app:app /app/data
ENV ASPNETCORE_ENVIRONMENT=Production ASPNETCORE_HTTP_PORTS=8080 Sessions__FilePath=/app/data/sessions.json
USER app
EXPOSE 8080
ENTRYPOINT ["dotnet", "Bcv.Api.dll"]
