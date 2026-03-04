package com.wilo.server.user.entity;

import com.wilo.server.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(length = 120)
    private String description;

    @Column(length = 512)
    private String profileImageUrl;

    @Column(length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private boolean phoneVerified;

    @Builder
    private User(String email, String password, String nickname, String description, String profileImageUrl, String phoneNumber, boolean phoneVerified) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
        this.phoneNumber = phoneNumber;
        this.phoneVerified = phoneVerified;
    }

    public void updateProfile(String nickname, String description, String phoneNumber) {
        this.nickname = nickname;
        this.description = description;
        if ((this.phoneNumber == null && phoneNumber != null)
                || (this.phoneNumber != null && !this.phoneNumber.equals(phoneNumber))) {
            this.phoneVerified = false;
        }
        this.phoneNumber = phoneNumber;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void markPhoneVerified() {
        this.phoneVerified = true;
    }
}
