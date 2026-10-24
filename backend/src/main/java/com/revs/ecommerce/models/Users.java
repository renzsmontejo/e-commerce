package com.revs.ecommerce.models;

// PACKAGES IMPORT
import com.fasterxml.jackson.annotation.JsonFormat;
import com.revs.ecommerce.models.enums.UserStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 100)
    private String first_name;

    @Column(nullable = false, length = 100)
    private String last_name;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(nullable = false, length = 200)
    private String password_hash;

    @Column(nullable = true, length= 50)
    private Long phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @JsonFormat(pattern = "MM-DD-YYYY HH:MM")
    private LocalDateTime emailVerifiedAt;

    @JsonFormat(pattern = "MM-DD-YYYY")
    private LocalDate created_at;

    @JsonFormat(pattern = "MM-DD-YYYY HH:MM:SS")
    private LocalDateTime updated_at;
}
