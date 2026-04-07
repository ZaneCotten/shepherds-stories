package com.shepherdsstories.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.shepherdsstories.utils.ValidationConstants.NAME_MAX_LENGTH;


@Getter
@Setter
@Entity
@Table(name = "supporter_profiles")
public class SupporterProfile {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Column(name = "first_name", nullable = false, length = NAME_MAX_LENGTH)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = NAME_MAX_LENGTH)
    private String lastName;

    @ColumnDefault("false")
    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;


}