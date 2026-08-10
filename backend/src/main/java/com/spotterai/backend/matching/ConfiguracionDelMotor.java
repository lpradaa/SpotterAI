package com.spotterai.backend.matching;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El puente entre la configuracion y el motor.
 *
 * <p>{@link CalculadoraCompatibilidad} es deliberadamente una clase estatica
 * sin estado de Spring —"el calculo es deterministico... cuesta cero"—, y eso
 * es justo lo que hace que decenas de pruebas puedan llamarla sin levantar un
 * contexto. Convertirla en un bean para poder inyectarle configuracion habria
 * significado tocar cada uno de esos sitios para no ganar nada a cambio.
 *
 * <p>Este componente si es un bean, y su unico trabajo es leer los ocho pesos
 * de donde sea que vengan —variables de entorno, {@code application.properties},
 * lo que Spring resuelva— y empujarlos al motor una vez, al arrancar. Antes de
 * esto, cambiar cualquier peso significaba recompilar y desplegar; ahora es una
 * variable de entorno.
 */
@Component
public class ConfiguracionDelMotor {

    private final double horario;
    private final double nivel;
    private final double fuerza;
    private final double objetivo;
    private final double constancia;
    private final double rutina;
    private final double gimnasio;
    private final double edad;

    public ConfiguracionDelMotor(
            @Value("${spotterai.motor.peso-horario:40}") double horario,
            @Value("${spotterai.motor.peso-nivel:10}") double nivel,
            @Value("${spotterai.motor.peso-fuerza:10}") double fuerza,
            @Value("${spotterai.motor.peso-objetivo:12}") double objetivo,
            @Value("${spotterai.motor.peso-constancia:10}") double constancia,
            @Value("${spotterai.motor.peso-rutina:5}") double rutina,
            @Value("${spotterai.motor.peso-gimnasio:8}") double gimnasio,
            @Value("${spotterai.motor.peso-edad:5}") double edad) {
        this.horario = horario;
        this.nivel = nivel;
        this.fuerza = fuerza;
        this.objetivo = objetivo;
        this.constancia = constancia;
        this.rutina = rutina;
        this.gimnasio = gimnasio;
        this.edad = edad;
    }

    /**
     * Empuja los pesos al motor.
     *
     * <p>En el constructor y no aqui se leen los valores porque el constructor
     * es donde Spring resuelve los {@code @Value}; este metodo solo los
     * traslada. Separarlo en dos pasos es lo que permite escribir una prueba
     * que instancie la clase con valores concretos sin depender de que Spring
     * este levantado —ver {@code ConfiguracionDelMotorTest}—.
     */
    @PostConstruct
    void aplicar() {
        CalculadoraCompatibilidad.configurar(
                new PesosDelMotor(horario, nivel, fuerza, objetivo, constancia, rutina, gimnasio, edad));
    }
}
