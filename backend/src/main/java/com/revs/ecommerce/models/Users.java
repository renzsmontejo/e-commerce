package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revs.ecommerce.models.enums.Status;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 200)
    private String passwordHash;

    @Column(name = "contact", nullable = true, length = 50)
    private String phone;


    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private Status status;

    @JsonFormat(pattern = "MM-dd-yyyy HH:mm:ss")
    @Column(name="email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @JsonFormat(pattern = "MM-dd-yyyy HH:mm:ss")
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        LocalDateTime now  = LocalDateTime.now();
        if(createdAt == null){
            createdAt = now;
        }
        if (updatedAt == null){
            updatedAt = now;
        }
    }

    @JsonFormat(pattern = "MM-dd-yyyy HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }


    @ManyToMany
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id") )
    private Set<Roles> roles = new HashSet<>();

    // ONE USER CAN HAVE MANY ADDRESS
    @OneToMany(mappedBy = "user")
    private List<Addresses> addresses = new ArrayList<>();

    // ONE USER
    @OneToMany(mappedBy = "user")
    private Set<AuthTokens> authTokens = new HashSet<>();

    // ONE USER HAS MANY INVENTORY MOVEMENTS
    @OneToMany(mappedBy = "users")
    private List<InventoryMovements> inventoryMovements = new ArrayList<>();
}
