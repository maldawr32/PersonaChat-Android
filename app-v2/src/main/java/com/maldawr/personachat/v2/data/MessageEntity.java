package com.maldawr.personachat.v2.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages", indices = {@Index("groupId"), @Index("senderPersonaId")})
public class MessageEntity {
    @PrimaryKey public long id;
    public long groupId;
    public long senderPersonaId; // 0 = user (Taj)
    @NonNull public String text = "";
    @NonNull public String type = "text";
    public long createdAt;

    public MessageEntity() {}

    public MessageEntity(long id, long groupId, long senderPersonaId, String text, long createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.senderPersonaId = senderPersonaId;
        this.text = text;
        this.createdAt = createdAt;
    }
}
