package com.maldawr.personachat.v2.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "personas")
public class PersonaEntity {
    @PrimaryKey public long id;
    @NonNull public String displayName = "";
    @NonNull public String nicknameForUser = "تاج";
    @NonNull public String relationship = "friend";
    @NonNull public String dialect = "Levantine Arabic";
    @NonNull public String style = "natural";
    @NonNull public String instructions = "";
    @NonNull public String avatarUri = "";
    public int accentColor = 0xFF25B889;
    public int humor = 40;
    public int warmth = 70;
    public int initiative = 60;

    public PersonaEntity() {}

    public PersonaEntity(long id, String displayName, String nicknameForUser,
                         String relationship, String dialect, String style,
                         String instructions, int accentColor,
                         int humor, int warmth, int initiative) {
        this.id = id;
        this.displayName = displayName;
        this.nicknameForUser = nicknameForUser;
        this.relationship = relationship;
        this.dialect = dialect;
        this.style = style;
        this.instructions = instructions;
        this.accentColor = accentColor;
        this.humor = humor;
        this.warmth = warmth;
        this.initiative = initiative;
    }
}
