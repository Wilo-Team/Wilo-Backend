package com.wilo.server.user.entity;

import com.wilo.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "guest_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuestProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="guest_id", nullable=false, unique=true)
    private String guestId;

    @Column(length=20)
    private String nickname;


    @Column(name = "chatbot_type_id")
    private Long chatbotTypeId;

    public static GuestProfile create(String guestId) {
        GuestProfile p = new GuestProfile();
        p.guestId = guestId;
        p.nickname = null;
        p.chatbotTypeId = null;
        return p;
    }

    public void updateChatbotTypeId(Long chatbotTypeId) {
        this.chatbotTypeId = chatbotTypeId;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
