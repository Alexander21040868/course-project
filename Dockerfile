# Образы с public.ecr.aws (официальное зеркало library/*), а не с Docker Hub:
# иначе на macOS сборка часто падает на docker-credential-desktop вне PATH Desktop.
FROM public.ecr.aws/docker/library/eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*
COPY pom.xml .
COPY src src
RUN mvn package -DskipTests

FROM public.ecr.aws/docker/library/docker:24-cli AS dockercli

FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=dockercli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
