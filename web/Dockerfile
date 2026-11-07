# ---------- Etape 1 : construction des fichiers statiques ----------
FROM node:22-alpine AS build
WORKDIR /build

COPY package.json ./
RUN npm install

COPY . .

# L'URL de l'API est injectee AU MOMENT DU BUILD : Vite la remplace
# dans le code genere. Un navigateur n'a pas de variables
# d'environnement, contrairement a un service Java.
ARG VITE_RECOMMENDATION_URL=http://localhost:8082
ENV VITE_RECOMMENDATION_URL=$VITE_RECOMMENDATION_URL
RUN npm run build

# ---------- Etape 2 : service des fichiers ----------
# nginx n'est ni un framework ni un concurrent de React : c'est le
# serveur web qui delivre au navigateur les fichiers produits par Vite.
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
