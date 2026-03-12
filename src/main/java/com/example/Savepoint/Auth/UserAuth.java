package com.example.Savepoint.Auth;

import com.example.Savepoint.User.UserProfile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class UserAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;
    @Nullable
    private String providerUserId;
    @Nullable
    private String passwordHash;
    @ManyToOne(fetch = FetchType.LAZY)
    //by default this table will have the column name as <class table name>_<primary key of that table name>
    //@JoinColumn(name = "owner_user_id") //this is for customising the column name
    @JsonIgnore
    private UserProfile user;
    @Column(unique = true,nullable = true)
    private String mailId;
    @CreationTimestamp
    private LocalDateTime authCreatedAt;
}
