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
import com.spotterai.backend.seguridad.FiltroGalletaCsrf;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
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

    /**
     * Donde vive el token de CSRF: en una galleta que el JavaScript SI puede leer.
     *
     * <p>Es un bean y no algo montado dentro de la cadena porque el login lo
     * necesita tambien: esa ruta esta exenta de CSRF —quien inicia sesion aun no
     * tiene token que enviar— y en las rutas exentas el mecanismo perezoso de
     * Spring ni se dispara, asi que la galleta hay que escribirla a mano alli.
     * Compartir el bean es lo que garantiza que las dos partes usen el mismo
     * nombre de galleta y la misma configuracion.
     */
    @Bean
    public org.springframework.security.web.csrf.CsrfTokenRepository repositorioCsrf() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF encendido, y no es opcional desde que la sesion va en una
            //    galleta. Estaba apagado con la razon correcta para entonces: el
            //    token viajaba en una cabecera que el frontend ponia a mano, y
            //    otra pagina no puede poner cabeceras en una peticion ajena. Una
            //    galleta, en cambio, la manda el navegador sola, asi que
            //    cualquier sitio puede provocar peticiones a esta con la sesion
            //    puesta. Eso es exactamente el CSRF.
            //
            //    withHttpOnlyFalse porque este token SI tiene que leerlo el
            //    JavaScript: la defensa consiste en que la pagina lo copie a una
            //    cabecera, y otro sitio no puede leer nuestras galletas para
            //    hacer lo mismo. Es el patron de doble envio, y aqui el secreto
            //    de sesion sigue siendo inaccesible.
            .csrf(csrf -> csrf
                .csrfTokenRepository(repositorioCsrf())
                // Sin enmascarado XOR. Con el que trae por defecto, el valor de
                // la galleta y el que el servidor espera no coinciden, y copiar
                // uno en el otro —que es lo que hace el interceptor— no valdria
                // nunca. El enmascarado protege de BREACH, que es un ataque
                // sobre respuestas comprimidas; aqui el token va en una galleta,
                // no en el HTML, asi que no aplica.
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Estas tres se llaman sin sesion previa, o sea sin que haya
                // ninguna galleta de CSRF todavia. Exigirla convertiria el
                // primer login en un 403 imposible de resolver. Y no hay nada
                // que proteger: quien las llama no tiene sesion que usurpar.
                .ignoringRequestMatchers(
                        "/api/auth/login",
                        "/api/auth/olvide",
                        "/api/auth/restablecer",
                        "/api/usuarios/registro",
                        "/api/avisos/baja",
                        "/api/avisos/alta"))
            
            // 1b. Y que la galleta del CSRF exista siempre, no solo cuando algo
            //     la pide. Spring la genera de forma perezosa, asi que la
            //     respuesta del login —que esta exento— salia sin ella y el
            //     primer POST de despues se llevaba un 403. Ver FiltroGalletaCsrf.
            .addFilterAfter(new FiltroGalletaCsrf(), CsrfFilter.class)

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

                // Darse de baja de los avisos, desde el enlace del propio correo.
                // Quien quiere dejar de recibirlos no va a iniciar sesion para
                // conseguirlo: lo que hace es marcar el remitente como no deseado,
                // y entonces tampoco le llegan los avisos que si queria. Lo que se
                // arriesga esta acotado: con esa llave —aleatoria, de 32 bytes— lo
                // unico que se puede hacer es cambiar esa preferencia.
                .requestMatchers(HttpMethod.POST, "/api/avisos/baja", "/api/avisos/alta").permitAll()

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
        
        // 3. Cabeceras. Authorization ya no esta: la sesion va en una galleta y el
        //    filtro ni la mira. X-XSRF-TOKEN es la que lleva la defensa contra
        //    CSRF, y sin permitirla aqui el navegador tumba toda peticion que
        //    cambie algo —desde otro origen, que es el caso de `ng serve`—.
        configuration.setAllowedHeaders(java.util.List.of(
                "X-XSRF-TOKEN", "Cache-Control", "Content-Type"));
        
        // 4. Credenciales permitidas. Exige un origen concreto y no "*", que es
        //    justo lo que hace la línea 1: con comodín, el navegador lo rechaza.
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = 
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}