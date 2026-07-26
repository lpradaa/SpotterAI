# --- Etapa 1: compilar -------------------------------------------------------
# Maven y el codigo fuente solo hacen falta aqui. Meterlos en la imagen final
# la multiplicaria por cinco y dejaria un compilador dentro de produccion.
FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /construccion

# Las dependencias primero y en su propia capa: cambian mucho menos que el
# codigo, asi que Docker las reutiliza en cada build en el que solo se toca un
# fichero .java.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Los tests corren en CI, con su base de datos y su informe. Repetirlos aqui
# alarga cada build de imagen sin aportar una señal distinta.
RUN mvn -B clean package -DskipTests

# --- Etapa 2: ejecutar -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios: por defecto los contenedores corren como root, y un
# fallo dentro de la aplicacion no deberia dar root sobre el contenedor.
RUN addgroup -S spotterai && adduser -S spotterai -G spotterai

# Carpeta de fotos y videos. Va como volumen en compose: si vive dentro de la
# imagen, cada despliegue borra lo que haya subido la gente.
RUN mkdir -p /app/medios && chown -R spotterai:spotterai /app

COPY --from=construccion --chown=spotterai:spotterai /construccion/target/*.jar app.jar

USER spotterai
EXPOSE 8080

# Sin shell de por medio, para que la aplicacion sea el PID 1 y reciba las
# señales de parada en vez de quedarse colgada hasta el timeout.
ENTRYPOINT ["java", "-jar", "app.jar"]
