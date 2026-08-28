package com.maldawr.personachat.v2.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(tableName = "persona_states", primaryKeys = {"conversationId", "personaId"}, indices = {@Index("personaId")})
public class PersonaStateEntity {
    public long conversationId;
    public long personaId;
    @NonNull public String mood = "neutral";
    @NonNull public String lastTopic = "";
    public long lastSpokeAt;
    public long lastAddressedPersonaId;
    public int engagement = 60;
    public int tension = 0;
    public int affection = 60;
    public int initiativeBoost = 0;

    public PersonaStateEntity() {}

    public PersonaStateEntity(long conversationId, long personaId) {
        this.conversationId = conversationId;
        this.personaId = personaId;
    }
}
