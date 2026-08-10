package com.spotterai.backend.matching;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El otro extremo del puente: que las propiedades lleguen de verdad al motor.
 *
 * <p>{@code ConfigurarPesosTest} prueba {@code CalculadoraCompatibilidad.configurar()}
 * a pelo, sin Spring. Esto prueba lo que hay delante: que
 * {@code spotterai.motor.peso-horario} en las propiedades acabe siendo
 * {@code CalculadoraCompatibilidad.PESO_HORARIO} despues de que el contexto
 * arranque. Las dos pruebas hacen falta porque la primera no dice nada de si el
 * cableado de Spring esta bien, y esta no dice nada de si el propio calculo usa
 * el valor que le llega.
 *
 * <p>Contexto acotado a esta sola clase —{@code classes = ConfiguracionDelMotor.class}—
 * y no la aplicacion entera: mas rapido, y lo unico que hace falta para
 * comprobar la resolucion de {@code @Value} es esta clase.
 *
 * <p>El valor de la propiedad es distinto del de fabrica (99 en vez de 40) a
 * proposito, para no poder confundir "se aplico el valor correcto" con "nunca
 * se aplico nada y seguia el de fabrica por casualidad". Y como toca el mismo
 * estado estatico compartido que {@code ConfigurarPesosTest}, se restaura en
 * {@code @AfterEach} exactamente igual.
 */
@SpringBootTest(classes = ConfiguracionDelMotor.class)
@TestPropertySource(properties = "spotterai.motor.peso-horario=99")
class ConfiguracionDelMotorTest {

    @AfterEach
    void restaurar() {
        CalculadoraCompatibilidad.configurar(new PesosDelMotor(40, 10, 10, 12, 10, 5, 8, 5));
    }

    @Test
    @DisplayName("Al arrancar, la propiedad configurada llega al motor")
    void laPropiedadLlegaAlMotor() {
        assertEquals(99, CalculadoraCompatibilidad.PESO_HORARIO);
    }

    @Test
    @DisplayName("Sin configurar un peso, se queda en el valor de fabrica")
    void sinConfigurarQuedaElValorDeFabrica() {
        // Solo se sobreescribe peso-horario en este contexto de prueba; el
        // resto tiene que resolver al valor por defecto del @Value, que es el
        // mismo de fabrica.
        assertEquals(10, CalculadoraCompatibilidad.PESO_NIVEL);
        assertEquals(5, CalculadoraCompatibilidad.PESO_EDAD);
    }
}
