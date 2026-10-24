package com.revs.ecommerce.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Addresses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MANY ADDRESS BELONGS TO ONE USER
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private Users user;

    @Column(name = "recipient_name", length = 150, nullable = false)
    private String recipientName;


    @Column(length = 50, nullable = false)
    private String phone;

    @Column(name="address_line_one", nullable = false, length = 255)
    private String addressLineOne;

    @Column(name = "address_line_two", nullable = true, length = 255)
    private String addressLineTwo;

    @Column(name="barangay", nullable = true, length = 255)
    private String barangay;

    @Column(name="city", nullable = false, length = 255)
    private String city;

    @Column(name="province", nullable = false, length = 255)
    private String province;

    @Column(name="postal_code", nullable = false, length = 255)
    private Integer postalCode;

    @Column(name="country", nullable = false, length = 255)
    private String country;

    @Column(name = "is_default")
    private Boolean isDefault;

    @JsonFormat(pattern = "MM-dd-yyyy HH:mm:ss")
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate(){
        if(createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }
}
