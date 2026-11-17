package br.com.fiap.zelo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret:troque-por-um-segredo-bem-grande-e-aleatorio-de-pelo-menos-32-caracteres}") String secret,
            @Value("${app.jwt.expirationMs:3600000}") long expirationMs) {
        // Garante no minimo 32 bytes para HS256
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(secretBytes, 0, padded, 0, secretBytes.length);
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(secretBytes);
        }
        this.expirationMs = expirationMs;
    }

    public String generateToken(String sub, String nome, String papel, UUID idosoId, UUID casaId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("nome", nome);
        claims.put("papel", papel);
        claims.put("role", papel);
        if (idosoId != null) {
            claims.put("idosoId", idosoId.toString());
        }
        if (casaId != null) {
            claims.put("casaId", casaId.toString());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(sub)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public List<GrantedAuthority> getAuthorities(String token) {
        Claims claims = getClaims(token);
        String papel = (String) claims.get("papel");
        if (papel == null) {
            papel = (String) claims.get("role");
        }
        if (papel == null && claims.get("roles") instanceof List<?> list && !list.isEmpty()) {
            papel = list.get(0).toString();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (papel != null) {
            String upper = papel.toUpperCase().trim();
            if (upper.startsWith("ROLE_")) {
                upper = upper.substring(5);
            }

            switch (upper) {
                case "FAMILIAR" -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_FAMILIAR"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_CUIDADOR"));
                }
                case "GUARDIAO" -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_GUARDIAO"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_CUIDADOR"));
                }
                case "CUIDADOR" -> authorities.add(new SimpleGrantedAuthority("ROLE_CUIDADOR"));
                case "IDOSO" -> authorities.add(new SimpleGrantedAuthority("ROLE_IDOSO"));
                case "ADMIN" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                case "SERVICO" -> authorities.add(new SimpleGrantedAuthority("ROLE_SERVICO"));
                default -> authorities.add(new SimpleGrantedAuthority("ROLE_" + upper));
            }
        }
        return authorities;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
