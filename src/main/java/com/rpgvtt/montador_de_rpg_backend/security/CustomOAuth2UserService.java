package com.rpgvtt.montador_de_rpg_backend.security;

import com.rpgvtt.montador_de_rpg_backend.domain.model.usuario.Usuario;
import com.rpgvtt.montador_de_rpg_backend.repository.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Carrega os dados do utilizador vindos do provedor (Google/Discord)
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            // 2. Identifica qual é o provedor (ex: "google" ou "discord")
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            
            // 3. Processa e salva/atualiza o utilizador no Supabase
            return processOAuth2User(registrationId, oAuth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(String registrationId, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail não encontrado no provedor de autenticação.");
        }

        // Extrai o apelido/nome dependendo do provedor
        String apelido = "";
        if ("google".equalsIgnoreCase(registrationId)) {
            apelido = (String) attributes.get("name");
        } else if ("discord".equalsIgnoreCase(registrationId)) {
            apelido = (String) attributes.get("username");
        }

        if (apelido == null || apelido.isBlank()) {
            apelido = "Utilizador_RPG";
        }

        // 4. Verifica se o utilizador já existe no Supabase
        Optional<Usuario> usuarioOptional = usuarioRepository.findByEmail(email);
        Usuario usuario;

        if (usuarioOptional.isPresent()) {
            // Se já existe, atualiza as informações básicas caso tenham mudado
            usuario = usuarioOptional.get();
            usuario.setApelido(apelido);
            usuarioRepository.save(usuario);
        } else {
            // Se é um novo utilizador, cria um registo do zero
            usuario = registerNewUser(email, apelido);
        }

        return oAuth2User;
    }

    private Usuario registerNewUser(String email, String apelido) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setApelido(apelido);
        usuario.setSenha(null); // Login social não utiliza senha local
        usuario.setE_admin(false); // Novos utilizadores não nascem administradores por padrão
        
        return usuarioRepository.save(usuario);
    }
}