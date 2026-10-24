package com.revs.ecommerce.models;

import com.revs.ecommerce.models.enums.URoles;

import jakarta.persistence.*;
import lombok.*;
import org.apache.catalina.User;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = true, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "u_roles", nullable = false)
    private URoles role;

    @ManyToMany(mappedBy = "roles")
    private Set<Users> users = new HashSet<>();
}
