package com.shepherdsstories.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.shepherdsstories.utils.ValidationConstants.*;

@Getter
@Setter
@Entity
@Table(name = "missionary_profiles")
public class MissionaryProfile {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reference_number", unique = true, nullable = false, length = REF_CODE_LENGTH)
    private String referenceNumber;

    @Column(name = "is_reference_disabled", nullable = false)
    private Boolean isReferenceDisabled;

    @Column(name = "missionary_name", nullable = false, length = NAME_MAX_LENGTH)
    private String missionaryName;

    @Column(name = "location_region", length = MISSION_AREA_MAX_LENGTH)
    private String locationRegion;

    @Column(name = "biography", length = BIO_MAX_LENGTH)
    private String biography;

    @OneToMany(mappedBy = "missionary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC") // Shows the newest requests first
    private List<PrayerRequest> prayerRequests = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}