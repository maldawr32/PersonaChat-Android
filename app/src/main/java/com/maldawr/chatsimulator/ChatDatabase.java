package com.maldawr.chatsimulator;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;

@Database(
        entities = {
                ChatDatabase.BotRow.class,
                ChatDatabase.MessageRow.class,
                ChatDatabase.CallRow.class,
                ChatDatabase.GroupMemberRow.class
        },
        version = 3,
        exportSchema = false
)
public abstract class ChatDatabase extends RoomDatabase {
    private static volatile ChatDatabase INSTANCE;

    public abstract DaoApi dao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE bots ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE bots ADD COLUMN groupChat INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE bots ADD COLUMN groupSubtitle TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE bots ADD COLUMN lastSender TEXT NOT NULL DEFAULT ''");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE bots ADD COLUMN personality TEXT NOT NULL DEFAULT 'friendly'");
            database.execSQL("ALTER TABLE bots ADD COLUMN emojiRate INTEGER NOT NULL DEFAULT 45");
            database.execSQL("ALTER TABLE bots ADD COLUMN humorRate INTEGER NOT NULL DEFAULT 25");
            database.execSQL("ALTER TABLE bots ADD COLUMN replyChance INTEGER NOT NULL DEFAULT 84");
            database.execSQL("ALTER TABLE bots ADD COLUMN onlineContent INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE bots ADD COLUMN quietStart INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE bots ADD COLUMN quietEnd INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE bots ADD COLUMN lastTopic TEXT NOT NULL DEFAULT ''");

            database.execSQL("ALTER TABLE messages ADD COLUMN sender TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE messages ADD COLUMN reaction TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE messages ADD COLUMN replyToId INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE messages ADD COLUMN kind TEXT NOT NULL DEFAULT 'text'");

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS group_members (" +
                            "id INTEGER NOT NULL PRIMARY KEY, " +
                            "botId INTEGER NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "style TEXT NOT NULL, " +
                            "emoji TEXT NOT NULL, " +
                            "activity INTEGER NOT NULL, " +
                            "humor INTEGER NOT NULL, " +
                            "color INTEGER NOT NULL)"
            );
            database.execSQL("CREATE INDEX IF NOT EXISTS index_group_members_botId ON group_members(botId)");
        }
    };

    public static ChatDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (ChatDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ChatDatabase.class,
                                    "chat_simulator_v5.db"
                            )
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    @Entity(tableName = "bots")
    public static class BotRow {
        @PrimaryKey public long id;
        public String name;
        public String phone;
        public String status;
        public int unread;
        public String lastMessage;
        public long lastTime;
        public boolean autoReply;
        public String avatarUri;
        public int activeFrom;
        public int activeTo;
        public String replyMode;
        public boolean initiative;
        public int maxBurst;
        @ColumnInfo(defaultValue = "0") public boolean favorite;
        @ColumnInfo(defaultValue = "0") public boolean groupChat;
        @ColumnInfo(defaultValue = "''") public String groupSubtitle = "";
        @ColumnInfo(defaultValue = "''") public String lastSender = "";
        @ColumnInfo(defaultValue = "'friendly'") public String personality = "friendly";
        @ColumnInfo(defaultValue = "45") public int emojiRate = 45;
        @ColumnInfo(defaultValue = "25") public int humorRate = 25;
        @ColumnInfo(defaultValue = "84") public int replyChance = 84;
        @ColumnInfo(defaultValue = "1") public boolean onlineContent = true;
        @ColumnInfo(defaultValue = "0") public int quietStart = 0;
        @ColumnInfo(defaultValue = "0") public int quietEnd = 0;
        @ColumnInfo(defaultValue = "''") public String lastTopic = "";
    }

    @Entity(tableName = "messages")
    public static class MessageRow {
        @PrimaryKey public long id;
        public long botId;
        public String text;
        public boolean incoming;
        public long time;
        @ColumnInfo(defaultValue = "''") public String sender = "";
        @ColumnInfo(defaultValue = "''") public String reaction = "";
        @ColumnInfo(defaultValue = "0") public long replyToId = 0L;
        @ColumnInfo(defaultValue = "'text'") public String kind = "text";
    }

    @Entity(tableName = "calls")
    public static class CallRow {
        @PrimaryKey public long id;
        public long botId;
        public String type;
        public long time;
        public int durationSec;
    }

    @Entity(tableName = "group_members", indices = {@Index("botId")})
    public static class GroupMemberRow {
        @PrimaryKey public long id;
        public long botId;
        public String name;
        public String style;
        public String emoji;
        public int activity;
        public int humor;
        public int color;
    }

    @Dao
    public interface DaoApi {
        @Query("SELECT COUNT(*) FROM bots") int botCount();
        @Query("SELECT * FROM bots ORDER BY lastTime DESC") List<BotRow> botsOrdered();
        @Query("SELECT * FROM bots WHERE unread > 0 ORDER BY lastTime DESC") List<BotRow> unreadBotsOrdered();
        @Query("SELECT * FROM bots WHERE favorite = 1 ORDER BY lastTime DESC") List<BotRow> favoriteBotsOrdered();
        @Query("SELECT * FROM bots WHERE groupChat = 1 ORDER BY lastTime DESC") List<BotRow> groupBotsOrdered();
        @Query("SELECT COUNT(*) FROM bots WHERE favorite = 1") int favoriteCount();
        @Query("SELECT COUNT(*) FROM bots WHERE groupChat = 1") int groupCount();
        @Query("SELECT * FROM bots WHERE id = :id LIMIT 1") BotRow bot(long id);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putBot(BotRow row);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putBots(List<BotRow> rows);
        @Query("UPDATE bots SET unread = 0 WHERE id = :botId") void markRead(long botId);
        @Query("UPDATE bots SET favorite = :favorite WHERE id = :botId") void setFavorite(long botId, boolean favorite);
        @Query("SELECT COALESCE(SUM(unread), 0) FROM bots") int totalUnread();
        @Query("DELETE FROM bots") void clearBots();

        @Query("SELECT * FROM messages WHERE botId = :botId ORDER BY time ASC, id ASC") List<MessageRow> messages(long botId);
        @Query("SELECT * FROM messages WHERE id = :id LIMIT 1") MessageRow message(long id);
        @Query("SELECT COUNT(*) FROM messages WHERE botId = :botId") int messageCount(long botId);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putMessage(MessageRow row);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putMessages(List<MessageRow> rows);
        @Query("UPDATE messages SET reaction = :reaction WHERE id = :messageId") void setReaction(long messageId, String reaction);
        @Query("DELETE FROM messages") void clearMessages();

        @Query("SELECT * FROM calls ORDER BY time DESC") List<CallRow> calls();
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putCall(CallRow row);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putCalls(List<CallRow> rows);
        @Query("DELETE FROM calls") void clearCalls();

        @Query("SELECT * FROM group_members WHERE botId = :botId ORDER BY activity DESC, id ASC") List<GroupMemberRow> groupMembers(long botId);
        @Query("SELECT COUNT(*) FROM group_members WHERE botId = :botId") int groupMemberCount(long botId);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putGroupMember(GroupMemberRow row);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putGroupMembers(List<GroupMemberRow> rows);
        @Query("DELETE FROM group_members WHERE botId = :botId") void clearGroupMembers(long botId);
        @Query("DELETE FROM group_members") void clearAllGroupMembers();
    }
}
