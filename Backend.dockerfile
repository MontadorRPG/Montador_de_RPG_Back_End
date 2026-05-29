## Não é preciso usar Dockerfile agora. Dockerfile serve para fazer builds finais para upar na VPS depois.
## Usar isso agora só traz mais complexidade para testar o aplicativo. O arquivo docker-compose sozinho já faz o
## necessário por enquanto.

## Estágio 1: Build da aplicação utilizando a imagem oficial do Java 25 JDK
#FROM eclipse-temurin:25-jdk AS build
#
## Instalar utilitários necessários para baixar e descompactar o Maven
#RUN apt-get update && apt-get install -y curl tar && rm -rf /var/lib/apt/lists/*
#
## Baixar e instalar o Maven 3.9.9 oficial diretamente no contêiner
#RUN curl -sLf https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz | tar -xz -C /usr/share \
#    && ln -s /usr/share/apache-maven-3.9.9/bin/mvn /usr/bin/mvn
#
#WORKDIR /app
#
## Copiar os ficheiros de configuração do Maven e o código-fonte
#COPY pom.xml .
#COPY src ./src
#
## Compilar e gerar o arquivo .jar (ignorando os testes para acelerar o build inicial)
#RUN mvn clean package -DskipTests
#
## Estágio 2: Ambiente de execução com Java 25 JRE oficial
#FROM eclipse-temurin:25-jre
#WORKDIR /app
#
## Copiar o .jar gerado no estágio anterior
#COPY --from=build /app/target/*.jar app.jar
#
## Expor a porta padrão do Spring Boot
#EXPOSE 8080
#
## Comando para iniciar a aplicação
#ENTRYPOINT ["java", "-jar", "app.jar"]