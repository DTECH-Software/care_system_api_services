/**
 * User: Himal_J
 * Date: 1/31/2025
 * Time: 3:46 PM
 * <p>
 */

package com.dtech.token.util;

import io.jsonwebtoken.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Log4j2
public class JwtUtil {

    @Value("${jwt.token.secret.key}")
    private String SECRET_KEY;

    @Value("${jwt.token.exp.time}")
    private long EXP_TIME;

    //generate token
    public String generateToken(String username) {
        log.info("Generate token {}", username);

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXP_TIME))
                .signWith(getSignInKey())
                .compact();

    }

    public String extractUsername(String token) {
        log.info("Extract username from token {}", token);
        return extractClaims(token).getSubject();
    }

    public Claims extractClaims(String token) {
        log.info("Extracting claims from token {}", token);
        return Jwts.parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    private SecretKey getSignInKey() {
        log.info("Get Sign In Key {}", SECRET_KEY);
        return new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    //validate token
    public boolean validateToken(String token,String username) {
        try {
            log.info("Validate token {}", token);
            Jws<Claims> claimsJws = Jwts.parser()
                    .setSigningKey(getSignInKey())
                    .build().parseSignedClaims(token);
            Date expiration = claimsJws.getPayload().getExpiration();
            if (expiration.before(new Date()) || !extractUsername(token).equals(username)) {
                log.warn("Token is expired {}", expiration);
                return false;
            }
            log.info("Token is valid.");
            return true;
        } catch (ExpiredJwtException e) {
            log.error("Token is expired", e);
            return false;
        } catch (Exception e) {
            log.error("Invalid token", e);
            return false;
        }
    }


}
