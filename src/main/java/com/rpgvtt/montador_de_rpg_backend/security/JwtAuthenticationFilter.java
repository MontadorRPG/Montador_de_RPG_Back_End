package com.rpgvtt.montador_de_rpg_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.UsuarioRepository;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Extrai o token JWT da requisição (Cabeçalho Authorization)
            String jwt = getJwtFromRequest(request);

            // 2. Valida o token e verifica se o utilizador já não está autenticado nesta requisição
            // if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            //     String email = tokenProvider.getEmailFromJWT(jwt);

            //     // 3. Cria um objeto UserDetails simples do Spring Security usando o e-mail extraído
            //     // Como usamos JWT e OAuth2, não precisamos da password aqui no contexto do filtro
            //     UserDetails userDetails = User.withUsername(email)
            //             .password("")
            //             .authorities(Collections.emptyList()) // Adicione permissões/roles aqui no futuro, se necessário
            //             .build();

            //     // 4. Cria o objeto de autenticação do Spring Security
            //     UsernamePasswordAuthenticationToken authentication = 
            //             new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
            //     authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            //     // 5. Define o utilizador como autenticado no contexto global do Spring para esta requisição
            //     SecurityContextHolder.getContext().setAuthentication(authentication);
            // }

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String email = tokenProvider.getEmailFromJWT(jwt);

                // Carrega o usuário real do banco
                Usuario usuario = usuarioRepository.findByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException(email));

                UsuarioPrincipal principal = new UsuarioPrincipal(usuario);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Não foi possível definir a autenticação do utilizador no contexto de segurança", ex);
        }

        // Continua o fluxo normal da requisição (vai para o Controller ou para o próximo filtro)
        filterChain.doFilter(request, response);
    }

    /**
     * Método utilitário para extrair o token do cabeçalho "Authorization: Bearer <token>"
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}