package com.maldawr.personachat.v2.data;

import android.content.Context;

public final class DemoSeeder {
    private DemoSeeder() {}

    public static void ensureSeeded(Context context) {
        AppDatabase db = AppDatabase.get(context);
        AppDatabase.AppDao dao = db.dao();
        if (dao.groupCount() > 0) return;

        PersonaEntity fareq = new PersonaEntity(
                101, "Fareq", "تاجو", "friend and teammate", "Levantine Arabic",
                "practical, friendly, concise",
                "Talk like a real teammate. Sometimes tease Taj lightly. Keep work context in mind.",
                0xFF9C6ADE, 55, 70, 65);
        PersonaEntity maimouna = new PersonaEntity(
                102, "Maimouna", "تاج", "close friend and teammate", "Levantine Arabic",
                "warm, supportive, direct",
                "Be caring but natural. Discuss project details with the other members and with Taj. Do not sound like an assistant.",
                0xFFFF9B4A, 45, 88, 72);

        GroupEntity group = new GroupEntity(
                5001, "Kazmoz Team Service",
                "Fictional project team simulation inspired by the user's reference layout.",
                "Taj");
        group.allowAutonomousConversation = true;
        group.autonomyLevel = 78;

        dao.putPersona(fareq);
        dao.putPersona(maimouna);
        dao.putGroup(group);
        dao.putMember(new GroupMemberEntity(group.id, fareq.id, 78, true));
        dao.putMember(new GroupMemberEntity(group.id, maimouna.id, 86, true));

        long now = System.currentTimeMillis();
        dao.putMessage(new MessageEntity(now - 600000, group.id, 0,
                "مساء الخير جميعاً، آسف على التأخير بتصميم الموقع. عم رتّب آخر التفاصيل وإن شاء الله بخلصه قريب.", now - 600000));
        dao.putMessage(new MessageEntity(now - 480000, group.id, fareq.id,
                "احتجتني بشغلة بتجديد شهادة الأمان واشتراك الشات... جلطتني شوي 😅", now - 480000));
        dao.putMessage(new MessageEntity(now - 360000, group.id, maimouna.id,
                "ما عليك يا تاج، أنا بدفعن. بس لازم يكون الموقع جاهز للشات، والباقي منرتبه سوا 🤍", now - 360000));
        dao.putMessage(new MessageEntity(now - 240000, group.id, fareq.id,
                "هيك خفّت الجلطة شوي 😂", now - 240000));
        dao.putMessage(new MessageEntity(now - 120000, group.id, 0,
                "تسلموا جميعاً، ووعد الموقع يكون جاهز قبل الشتا إن شاء الله 👌", now - 120000));
    }
}
