package com.maldawr.personachat.v2.data;

import android.content.Context;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.List;

@Database(
        entities = {
                PersonaEntity.class,
                GroupEntity.class,
                GroupMemberEntity.class,
                MessageEntity.class,
                ConversationEntity.class,
                PersonaStateEntity.class,
                CallSessionEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract AppDao dao();

    public static AppDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "personachat_v2.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    @Dao
    public interface AppDao {
        @Query("SELECT COUNT(*) FROM groups") int groupCount();
        @Query("SELECT * FROM groups ORDER BY id") List<GroupEntity> groups();
        @Query("SELECT * FROM personas WHERE id = :id LIMIT 1") PersonaEntity persona(long id);
        @Query("SELECT p.* FROM personas p INNER JOIN group_members gm ON gm.personaId = p.id WHERE gm.groupId = :groupId ORDER BY gm.speakingWeight DESC, p.id")
        List<PersonaEntity> personasForGroup(long groupId);
        @Query("SELECT * FROM messages WHERE groupId = :groupId ORDER BY createdAt ASC, id ASC")
        List<MessageEntity> messagesForGroup(long groupId);

        @Query("SELECT * FROM conversations WHERE archived = 0 ORDER BY pinned DESC, lastActivityAt DESC")
        List<ConversationEntity> activeConversations();
        @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1") ConversationEntity conversation(long id);
        @Query("SELECT * FROM persona_states WHERE conversationId = :conversationId")
        List<PersonaStateEntity> personaStates(long conversationId);
        @Query("SELECT * FROM call_sessions WHERE id = :id LIMIT 1") CallSessionEntity call(long id);
        @Query("SELECT * FROM call_sessions WHERE externalKey = :externalKey LIMIT 1") CallSessionEntity callByExternalKey(String externalKey);
        @Query("SELECT * FROM call_sessions WHERE state = 'ringing' ORDER BY createdAt DESC") List<CallSessionEntity> ringingCalls();

        @Insert(onConflict = OnConflictStrategy.REPLACE) void putPersona(PersonaEntity value);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putGroup(GroupEntity value);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putMember(GroupMemberEntity value);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putMessage(MessageEntity value);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putConversation(ConversationEntity value);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putPersonaState(PersonaStateEntity value);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putCall(CallSessionEntity value);
    }
}
