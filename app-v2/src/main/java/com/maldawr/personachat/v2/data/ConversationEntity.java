package com.maldawr.personachat.v2.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations", indices = {@Index("personaId"), @Index("groupId"), @Index("lastActivityAt")})
public class ConversationEntity {
    public static final String TYPE_DIRECT = "direct";
    public static final String TYPE_GROUP = "group";

    @PrimaryKey public long id;
    @NonNull public String type = TYPE_DIRECT;
    public long personaId; // direct only
    public long groupId;   // group only
    @NonNull public String title = "";
    @NonNull public String subtitle = "";
    @NonNull public String avatarUri = "";
    @NonNull public String lastMessage = "";
    public long lastActivityAt;
    public int unreadCount;
    public boolean pinned;
    public boolean archived;
    public boolean muted;

    public ConversationEntity() {}

    public static ConversationEntity direct(long id, PersonaEntity persona) {
        ConversationEntity c = new ConversationEntity();
        c.id = id;
        c.type = TYPE_DIRECT;
        c.personaId = persona.id;
        c.title = persona.displayName;
        c.avatarUri = persona.avatarUri;
        c.lastActivityAt = System.currentTimeMillis();
        return c;
    }

    public static ConversationEntity group(long id, GroupEntity group) {
        ConversationEntity c = new ConversationEntity();
        c.id = id;
        c.type = TYPE_GROUP;
        c.groupId = group.id;
        c.title = group.name;
        c.subtitle = group.description;
        c.avatarUri = group.avatarUri;
        c.lastActivityAt = System.currentTimeMillis();
        return c;
    }
}
