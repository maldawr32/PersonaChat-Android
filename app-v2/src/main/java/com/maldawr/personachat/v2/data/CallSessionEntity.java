package com.maldawr.personachat.v2.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "call_sessions", indices = {@Index("conversationId"), @Index("state"), @Index(value = {"externalKey"}, unique = true)})
public class CallSessionEntity {
    public static final String RINGING = "ringing";
    public static final String ANSWERED = "answered";
    public static final String ENDED = "ended";
    public static final String MISSED = "missed";
    public static final String DECLINED = "declined";

    @PrimaryKey public long id;
    public long conversationId;
    @NonNull public String externalKey = "";
    @NonNull public String state = RINGING;
    public boolean video;
    public boolean incoming = true;
    public long createdAt;
    public long answeredAt;
    public long endedAt;

    public CallSessionEntity() {}

    public boolean terminal() {
        return ENDED.equals(state) || MISSED.equals(state) || DECLINED.equals(state);
    }
}
