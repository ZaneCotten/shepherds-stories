package com.shepherdsstories.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.shepherdsstories.utils.ValidationConstants.REF_CODE_LENGTH;

@Getter
@Setter
@Entity
@Table(name = "invite_codes")
public class InviteCode {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "missionary_id")
    private MissionaryProfile missionary;

    @Column(name = "code_string", nullable = false, length = REF_CODE_LENGTH)
    private String codeString;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;


}