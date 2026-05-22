package com.rpgvtt.montador_de_rpg_backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtTokenProvider tokenProvider;

    // Injeta a URL do React definida no application.yaml (http://localhost:5173/oauth2/redirect)
    @Value("${app.oauth2.authorizedRedirectUri}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {
        
        if (response.isCommitted()) {
            logger.debug("A resposta já foi enviada. Não é possível redirecionar para " + authorizedRedirectUri);
            return;
        }

        // 1. Obtém o utilizador que acabou de logar com sucesso via OAuth2
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail não encontrado no provedor de autenticação.");
        }

        // 2. Gera o token JWT baseado no e-mail do utilizador
        String token = tokenProvider.generateToken(email);

        // 3. Constrói a URL final anexando o token como parâmetro (query param)
        String targetUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                .queryParam("token", token)
                .build().toUriString();

        // 4. Limpa os dados temporários de autenticação que o Spring guarda na sessão
        clearAuthenticationAttributes(request);

        // 5. Redireciona o utilizador de volta para o frontend (React / Vite)
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}