package com.maldawr.personachat.v2.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "groups")
public class GroupEntity {
    @PrimaryKey public long id;
    @NonNull public String name = "";
    @NonNull public String description = "";
    @NonNull public String avatarUri = "";
    @NonNull public String userDisplayName = "Taj";
    public boolean allowAutonomousConversation = true;
    public int autonomyLevel = 70;

    public GroupEntity() {}

    public GroupEntity(long id, String name, String description, String userDisplayName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userDisplayName = userDisplayName;
    }
}
