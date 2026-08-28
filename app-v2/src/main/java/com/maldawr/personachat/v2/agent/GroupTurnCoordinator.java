package com.maldawr.personachat.v2.agent;

import com.maldawr.personachat.v2.data.PersonaEntity;
import com.maldawr.personachat.v2.data.PersonaStateEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chooses plausible speakers for a fictional group conversation.
 * The language model writes the text; this coordinator owns turn-taking so
 * personas do not randomly overwrite one another or all answer every message.
 */
public final class GroupTurnCoordinator {
    private GroupTurnCoordinator() {}

    public static List<PersonaEntity> chooseSpeakers(
            List<PersonaEntity> personas,
            List<PersonaStateEntity> states,
            long lastSpeakerId,
            long addressedPersonaId,
            boolean userJustSpoke,
            int maxTurns) {

        Map<Long, PersonaStateEntity> byId = new HashMap<>();
        if (states != null) for (PersonaStateEntity s : states) byId.put(s.personaId, s);

        List<Scored> scored = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (PersonaEntity p : personas) {
            PersonaStateEntity s = byId.get(p.id);
            int score = p.initiative;
            if (s != null) {
                score += s.engagement / 2;
                score += s.initiativeBoost;
                score += s.affection / 8;
                score -= s.tension / 5;
                if (now - s.lastSpokeAt < 45_000L) score -= 45;
                if (s.lastAddressedPersonaId == lastSpeakerId && lastSpeakerId != 0) score += 18;
            }
            if (p.id == addressedPersonaId && addressedPersonaId != 0) score += 90;
            if (p.id == lastSpeakerId) score -= userJustSpoke ? 18 : 55;
            if (userJustSpoke) score += p.warmth / 10;
            scored.add(new Scored(p, score));
        }

        scored.sort(Comparator.comparingInt((Scored x) -> x.score).reversed());
        List<PersonaEntity> out = new ArrayList<>();
        int limit = Math.max(1, Math.min(maxTurns, 3));
        for (Scored x : scored) {
            if (out.size() >= limit) break;
            if (x.score < 20 && !out.isEmpty()) break;
            out.add(x.persona);
        }
        return out;
    }

    private static final class Scored {
        final PersonaEntity persona;
        final int score;
        Scored(PersonaEntity persona, int score) {
            this.persona = persona;
            this.score = score;
        }
    }
}
