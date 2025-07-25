package Quick.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import Quick.model.RefreshToken;
import Quick.repository.RefreshTokenRepository;
import org.springframework.security.core.userdetails.UserDetails;
import java.security.Key;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtUtils {

    private final Key secretKey;
    private final long jwtExpirationMs;
    private RefreshTokenRepository _refreshTokenRepository;

    public JwtUtils(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expirationMs}") long jwtExpirationMs,
            RefreshTokenRepository refreshTokenRepository) 
    {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.jwtExpirationMs = jwtExpirationMs;
        this._refreshTokenRepository = refreshTokenRepository;
    }

    public String generateToken(UserDetails userDetails)
    {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public RefreshToken generateRefreshToken(long userId) 
    {
        String token = UUID.randomUUID().toString();
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationMs * 30);
        RefreshToken refreshToken = new RefreshToken(token, userId, expiryDate);
        return refreshToken;
    }

    public String extractUsername(String token) 
    {
        return Jwts
                .parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) 
    {
        try 
        {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } 
        catch (JwtException | IllegalArgumentException e) 
        {
            return false;
        }
    }


    public boolean validateRefreshToken(String token) 
    {
        try 
        {
           Optional<RefreshToken> refreshToken = _refreshTokenRepository.findByToken(token);
            return refreshToken.isPresent() && 
                   refreshToken.get().getExpiryDate().after(new Date()) && 
                   !refreshToken.get().isRevoked();
        } 
        catch (JwtException | IllegalArgumentException e) 
        {
            return false;
        }
    }

    public long getUserIdFromRefreshToken(String token) 
    {
        Optional<RefreshToken> refreshToken = _refreshTokenRepository.findByToken(token);
        if (refreshToken.isPresent()) {
            return refreshToken.get().getUserId();
        } else {
            throw new RuntimeException("Invalid refresh token");
        }
    }

    
}
