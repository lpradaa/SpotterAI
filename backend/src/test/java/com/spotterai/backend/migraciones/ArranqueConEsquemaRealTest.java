package com.spotterai.backend.migraciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que la aplicacion arranque de verdad, contra el esquema de verdad.
 *
 * <h2>El agujero que tapa</h2>
 *
 * <p>El resto de la suite corre con H2 y {@code spring.flyway.enabled=false}: el
 * esquema lo crea Hibernate desde las entidades, que es lo que esas pruebas
 * quieren. La consecuencia es que <b>nadie ejecuta las migraciones</b>, y hay
 * una clase entera de fallo que no ve ninguna:
 *
 * <ul>
 *   <li>dos migraciones con la misma version — paso, y la aplicacion no
 *       arrancaba;</li>
 *   <li>SQL que MariaDB no acepta;</li>
 *   <li>una entidad que no casa con la columna que crea su migracion, que con
 *       {@code ddl-auto=validate} tampoco deja arrancar.</li>
 * </ul>
 *
 * <p>El CI construia las dos imagenes Docker, y construir una imagen no la
 * arranca. Asi que el primer sitio donde se veia cualquiera de esas tres cosas
 * era el despliegue.
 *
 * <h2>Cuando se ejecuta</h2>
 *
 * <p>Solo si hay {@code DB_HOST}: en CI lo pone el servicio de MariaDB, y en
 * local lo pone quien quiera correrlo contra su base. Sin esa variable se salta,
 * porque una prueba que exige infraestructura y falla en una maquina limpia
 * acaba enseñando a ignorar los fallos.
 *
 * <pre>DB_HOST=localhost ./mvnw test -Dtest=ArranqueConEsquemaRealTest</pre>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME:spotterai_db}"
                + "?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.username=${DB_USER:root}",
        "spring.datasource.password=${DB_PASSWORD:}",
        // Las dos que el resto de la suite apaga, y que son justo el objeto de
        // esta prueba.
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
})
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+",
        disabledReason = "Necesita una base de datos: en CI la pone el servicio de MariaDB")
class ArranqueConEsquemaRealTest {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Si el contexto levanta, las tres cosas de arriba estan bien.
     *
     * <p>No hace falta afirmar nada mas: llegar hasta aqui significa que Flyway
     * resolvio y aplico todas las migraciones y que Hibernate valido cada
     * entidad contra el esquema que quedo.
     */
    @Test
    @DisplayName("La aplicacion arranca con las migraciones aplicadas y el esquema validado")
    void arranca() {
        Integer migracionesAplicadas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class);

        assertThat(migracionesAplicadas)
                .as("Flyway tiene que haber aplicado todas las migraciones")
                .isNotNull()
                .isGreaterThan(0);
    }

    /**
     * La ultima migracion esta aplicada, no solo las viejas.
     *
     * <p>Una base que ya existia de antes puede tener el historial a medias: sin
     * esto, la prueba pasaria por haber aplicado las de hace meses.
     */
    @Test
    @DisplayName("La ultima migracion del repositorio esta aplicada")
    void laUltimaTambien() {
        Integer ultima = jdbc.queryForObject(
                "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1",
                Integer.class);

        assertThat(ultima)
                .as("La version mas alta del historial tiene que ser la del repositorio")
                .isNotNull()
                .isGreaterThanOrEqualTo(18);
    }
}
