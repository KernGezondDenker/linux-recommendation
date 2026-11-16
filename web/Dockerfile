# ---------- Etape 1 : construction des fichiers statiques ----------
FROM node:22-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

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
#
# Variante NON PRIVILEGIEE de l'image officielle (§7.2) : nginx tourne
# sous l'utilisateur nginx (uid 101) et non en root. Un utilisateur
# ordinaire ne peut pas ouvrir un port inferieur a 1024 : l'image
# ecoute donc sur 8080 au lieu de 80.
#
# Branche 1.30 = branche STABLE maintenue. La 1.27 utilisee jusqu'ici
# est en fin de vie depuis juin 2025 (plus aucun correctif de securite).
FROM nginxinc/nginx-unprivileged:1.30-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
