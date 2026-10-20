# =====================================================================
# Construction en DEUX ETAPES (multi-stage build).
#
# Etape 1 : compiler avec Maven et le JDK complet.
# Etape 2 : ne garder que le .jar et une JRE.
#
# Interet : l'image finale ne contient ni Maven, ni le JDK, ni le code
# source. Elle pese quelques dizaines de megaoctets au lieu de
# plusieurs centaines, et sa surface d'attaque est reduite d'autant.
# =====================================================================

# ---------- Etape 1 : compilation ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# On copie d'abord le seul pom.xml pour telecharger les dependances.
# Docker met cette couche en cache : tant que le pom ne change pas, les
# dependances ne sont pas retelechargees a chaque build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etape 2 : execution ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Utilisateur NON PRIVILEGIE. Par defaut un conteneur tourne en root ;
# une faille dans l'application donnerait alors les pleins pouvoirs
# dans le conteneur.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
