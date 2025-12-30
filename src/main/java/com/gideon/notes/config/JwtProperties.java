package com.gideon.notes.config;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties("jwt")
public class JwtProperties {
    private String secretKey;
    private long tokenExp;
    private long refreshTokenExp;
}
