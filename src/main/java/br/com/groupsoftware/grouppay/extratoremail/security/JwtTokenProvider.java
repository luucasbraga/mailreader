package br.com.groupsoftware.grouppay.extratoremail.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Provedor de tokens JWT para autenticação e autorização.
 *
 * <p>Responsável por gerar, validar e extrair informações de tokens JWT.
 * Utiliza a biblioteca JJWT para lidar com a criação e parsing dos tokens.</p>
 *
 * <p>As configurações, como a chave secreta e o tempo de expiração, são definidas
 * no arquivo de propriedades da aplicação.</p>
 *
 * <p>Este componente é essencial para a segurança de APIs baseadas em autenticação JWT.</p>
 *
 * @author Marco Willy
 * @version 1.0
 * @since 2024
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt-secret-key}")
    private String secretKey;

    @Value("${jwt-expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody().getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}

