package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="auth_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuthTokens {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false

    )
    private Users user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "token_type", length = 30)
    private String token_type;

    @JsonFormat(pattern = "MM-dd-yyyy HH:mm:ss")
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @JsonFormat(pattern = "MM-dd-YYYY HH:mm:ss")
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @JsonFormat(pattern = "MM-dd-YYYY HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        if(createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }
}
