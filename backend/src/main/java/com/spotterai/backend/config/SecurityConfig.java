package com.spotterai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; 
import java.util.List; 

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final String allowedOrigin;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          @org.springframework.beans.factory.annotation.Value("${spotterai.cors.allowed-origin}") String allowedOrigin) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.allowedOrigin = allowedOrigin;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Desactivamos CSRF porque usamos tokens JWT de forma stateless
            .csrf(csrf -> csrf.disable())
            
            // 2. Configuramos el CORS para que Angular pueda hablar con Spring Boot
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) 
            
            // 3. Filtramos las rutas protegidas y públicas
            .authorizeHttpRequests(auth -> auth
                // Permitimos el login, el registro y la ruta de /error sin necesidad de token
                .requestMatchers("/api/auth/**", "/api/usuarios/registro", "/error").permitAll()

                // El canal de eventos no puede pasar por el filtro JWT: EventSource
                // no permite mandar cabeceras. Se autentica con un ticket de un solo
                // uso que se pide antes con el token (ver TicketsSse).
                .requestMatchers("/api/eventos/suscribir").permitAll()

                // La entrega de fotos y videos es publica; la subida no.
                // Un <img> o un <video> no pueden mandar la cabecera Authorization
                // —la misma limitacion del navegador que obligo a los tickets del
                // canal de eventos—, asi que exigir token aqui haria imposible
                // usar la URL directamente. Lo que protege el contenido es que la
                // ruta lleva un UUID y no se adivina.
                .requestMatchers(HttpMethod.GET, "/api/medios/**").permitAll()
                
                // 🔥 MODIFICADO: Añadimos explícitamente /api/gimnasios a las rutas que requieren estar logueado
                .requestMatchers("/api/usuarios/**", "/api/gimnasios/**").authenticated() 
                
                // Cualquier otra petición necesitará estar logueado
                .anyRequest().authenticated()
            )
            
            // 4. Añadimos el filtro de JWT para que lea la cabecera 'Authorization'
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * La única política de orígenes de la aplicación.
     *
     * <p>Y única de verdad desde hace poco: cinco controladores llevaban
     * {@code @CrossOrigin(origins = "http://localhost:4200")} escrito a fuego,
     * herencia del TFG que sobrevivió a hacer el origen configurable. El
     * resultado era que {@code FRONTEND_ORIGIN} no gobernaba lo que decía
     * gobernar —ni el README, que lo prometía igual—, y con la aplicación
     * servida desde otro sitio esos cinco seguían anunciando localhost.
     *
     * <p>Una configuración que solo manda sobre parte de la aplicación es peor
     * que no tenerla, porque nadie sabe cuál es la parte. Si alguien vuelve a
     * poner la anotación en un controlador, esto deja de ser cierto: la
     * política tiene que quedarse aquí.
     *
     * <p>Con nginx delante, además, el frontend y la API comparten origen y esto
     * no llega ni a usarse. Se mantiene por si alguien ataca la API directa,
     * como en desarrollo con {@code ng serve}.
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration =
                new org.springframework.web.cors.CorsConfiguration();

        // 1. Origen del frontend, configurable via FRONTEND_ORIGIN
        configuration.setAllowedOrigins(java.util.List.of(allowedOrigin));
        
        // 2. Autorizamos los métodos estándar
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 3. Autorizamos las cabeceras críticas (especialmente Authorization para vuestro JWT)
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Cache-Control", "Content-Type"));
        
        // 4. Credenciales permitidas. Exige un origen concreto y no "*", que es
        //    justo lo que hace la línea 1: con comodín, el navegador lo rechaza.
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = 
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}