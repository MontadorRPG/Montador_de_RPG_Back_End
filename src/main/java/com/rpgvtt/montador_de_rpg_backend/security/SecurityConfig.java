package com.rpgvtt.montador_de_rpg_backend.security;

import org.springframework.beans.factory.annotation.Value; // IMPORTANTE
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    // Injeta a lista dinâmica definida no seu application.yml / application-dev.yml
    @Value("${app.cors.allowed-origins:#{T(java.util.List).of('http://localhost:5173','http://localhost:3000','https://*.githubpreview.dev','https://*.app.github.dev')}}")
    private List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Ativa o CORS com as configurações definidas no Bean abaixo
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. Desativa o CSRF, pois os tokens JWT são imunes a este tipo de ataque (não usamos cookies de sessão)
            .csrf(csrf -> csrf.disable())
            
            // 3. Define a arquitetura como STATELESS (sem estado no servidor)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 4. Define as regras de autorização das rotas da API
            .authorizeHttpRequests(auth -> auth
                // Permite acesso livre a recursos estáticos comuns se necessário
                .requestMatchers("/", "/error", "/favicon.ico", "/**/*.png", "/**/*.gif", "/**/*.svg", "/**/*.jpg", "/**/*.html", "/**/*.css", "/**/*.js").permitAll()
                // Permite os endpoints de autenticação e os callbacks do OAuth2
                .requestMatchers("/auth/**", "/oauth2/**", "/login/**").permitAll()
                // Qualquer outra rota do sistema (Campanhas, Personagens, Mapas) exigirá autenticação
                .anyRequest().authenticated()
            )
            
            // 5. Configuração do fluxo de Login OAuth2 (Google / Discord)
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    // Endpoint base para iniciar o login (Ex: http://localhost:8080/oauth2/authorize/google)
                    .baseUri("/oauth2/authorize")
                )
                .redirectionEndpoint(redirection -> redirection
                    // Endpoint de callback que o Google/Discord chamam após o utilizador aceitar
                    .baseUri("/login/oauth2/code/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    // Define o nosso serviço customizado para salvar/atualizar no Supabase
                    .userService(customOAuth2UserService)
                )
                // Define o nosso manipulador para gerar o JWT e redirecionar para o React
                .successHandler(oAuth2AuthenticationSuccessHandler)
            );

        // 6. Insere o nosso filtro do JWT antes do filtro padrão de autenticação do Spring
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuração de CORS desacoplada e dinâmica
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Alterado de setAllowedOrigins para setAllowedOriginPatterns para aceitar as URLs com "*" do Codespaces
        configuration.setAllowedOriginPatterns(allowedOrigins);
        
        // Métodos HTTP aceitos pela API
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Cabeçalhos permitidos. O "Authorization" é crucial para o envio do Bearer Token JWT
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control", "X-Requested-With", "Accept"));
        
        // Permite o envio de credenciais/cookies se necessário
        configuration.setAllowCredentials(true);
        
        // Cache pré-flight por 1 hora
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica estas regras a todos os endpoints da aplicação
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}