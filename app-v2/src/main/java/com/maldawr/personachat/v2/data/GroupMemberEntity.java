package com.maldawr.personachat.v2.data;

import androidx.room.Entity;
import androidx.room.Index;

@Entity(tableName = "group_members", primaryKeys = {"groupId", "personaId"}, indices = {@Index("groupId"), @Index("personaId")})
public class GroupMemberEntity {
    public long groupId;
    public long personaId;
    public int speakingWeight = 70;
    public boolean canInitiate = true;

    public GroupMemberEntity() {}

    public GroupMemberEntity(long groupId, long personaId, int speakingWeight, boolean canInitiate) {
        this.groupId = groupId;
        this.personaId = personaId;
        this.speakingWeight = speakingWeight;
        this.canInitiate = canInitiate;
    }
}
