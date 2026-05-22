package com.rpgvtt.montador_de_rpg_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Injeta a chave secreta definida no seu application.yaml
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Injeta o tempo de expiração definido no seu application.yaml
    @Value("${app.jwt.expirationInMs}")
    private long jwtExpirationInMs;

    /**
     * Transforma a String do application.yaml em uma SecretKey segura.
     * O JJWT 0.12.x exige chaves com tamanho seguro (mínimo de 256 bits / 32 caracteres) para o algoritmo HS256.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera um token JWT contendo o e-mail do usuário como "Subject"
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrai o e-mail de dentro de um token JWT válido
     */
    public String getEmailFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Valida se o token JWT é íntegro, se não expirou e se possui assinatura válida
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            System.err.println("Token JWT inválido ou malformado.");
        } catch (ExpiredJwtException ex) {
            System.err.println("Token JWT expirado.");
        } catch (UnsupportedJwtException ex) {
            System.err.println("Token JWT não suportado.");
        } catch (IllegalArgumentException ex) {
            System.err.println("A string do claims JWT está vazia.");
        } catch (JwtException ex) {
            System.err.println("Erro na assinatura do token JWT.");
        }
        return false;
    }
}