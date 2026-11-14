package io.github.huydangtruong.permahearts.technique;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-player technique state (D43-D46). Pure Java, immutable: every "with" returns a new record.
 *
 * @param levels        technique id to level (missing = 0)
 * @param spentPoints   technique id to the XP points paid for it (the reset refunds half)
 * @param emeralds      emeralds deposited toward the slots, 0..MAX_EMERALDS
 * @param slots         4 entries, technique id or "" for an empty slot
 * @param cooldownUntil technique id to the overworld game time (ticks) at which it is ready again
 */
public record TechniqueData(Map<String, Integer> levels, Map<String, Integer> spentPoints, int emeralds,
	List<String> slots, Map<String, Long> cooldownUntil) {

	public static final TechniqueData DEFAULT = new TechniqueData(Map.of(), Map.of(), 0, emptySlots(), Map.of());

	public static final Codec<TechniqueData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(TechniqueData::levels),
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("spent_points", Map.of()).forGetter(TechniqueData::spentPoints),
		Codec.INT.optionalFieldOf("emeralds", 0).forGetter(TechniqueData::emeralds),
		Codec.STRING.listOf().optionalFieldOf("slots", emptySlots()).forGetter(TechniqueData::slots),
		Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldown_until", Map.of()).forGetter(TechniqueData::cooldownUntil)
	).apply(i, TechniqueData::new));

	/** Normalises: slots always has exactly 4 entries, maps are copied (the record is immutable). */
	public TechniqueData {
		levels = Map.copyOf(levels);
		spentPoints = Map.copyOf(spentPoints);
		cooldownUntil = Map.copyOf(cooldownUntil);
		List<String> s = new ArrayList<>(slots);
		while (s.size() < TechniqueRules.MAX_SLOTS) {
			s.add("");
		}
		slots = List.copyOf(s.subList(0, TechniqueRules.MAX_SLOTS));
		emeralds = Math.max(0, Math.min(TechniqueRules.MAX_EMERALDS, emeralds));
	}

	private static List<String> emptySlots() {
		return List.of("", "", "", "");
	}

	// ---- reads ----

	public int level(Technique t) {
		return levels.getOrDefault(t.id(), 0);
	}

	public int spent(Technique t) {
		return spentPoints.getOrDefault(t.id(), 0);
	}

	public int slotsOpen() {
		return TechniqueRules.slotsFor(emeralds);
	}

	/** Technique in a slot, or null when the slot is empty or does not exist. */
	public Technique inSlot(int slot) {
		if (slot < 0 || slot >= slots.size()) {
			return null;
		}
		return Technique.byId(slots.get(slot)).orElse(null);
	}

	/** Slot (0..3) the technique sits in, or -1. */
	public int slotOf(Technique t) {
		return slots.indexOf(t.id());
	}

	public long cooldownEnd(Technique t) {
		return cooldownUntil.getOrDefault(t.id(), 0L);
	}

	public boolean onCooldown(Technique t, long now) {
		return cooldownEnd(t) > now;
	}

	/** Ticks left (0 when ready). */
	public long cooldownLeft(Technique t, long now) {
		return Math.max(0, cooldownEnd(t) - now);
	}

	/** True when the technique in this slot is on cooldown (an empty slot never is). */
	public boolean slotOnCooldown(int slot, long now) {
		Technique t = inSlot(slot);
		return t != null && onCooldown(t, now);
	}

	// ---- changes ----

	public TechniqueData withLevel(Technique t, int level, int spent) {
		Map<String, Integer> l = new HashMap<>(levels);
		Map<String, Integer> s = new HashMap<>(spentPoints);
		if (level <= 0) {
			l.remove(t.id());
			s.remove(t.id());
		} else {
			l.put(t.id(), Math.min(TechniqueRules.MAX_LEVEL, level));
			s.put(t.id(), Math.max(0, spent));
		}
		return new TechniqueData(l, s, emeralds, slots, cooldownUntil);
	}

	public TechniqueData withEmeralds(int n) {
		return new TechniqueData(levels, spentPoints, n, slots, cooldownUntil);
	}

	/** Puts the technique in the slot (an empty id clears it). A technique sits in at most one slot, so it leaves any other. */
	public TechniqueData withSlot(int slot, Technique t) {
		List<String> s = new ArrayList<>(slots);
		if (t != null) {
			for (int i = 0; i < s.size(); i++) {
				if (s.get(i).equals(t.id())) {
					s.set(i, "");
				}
			}
		}
		s.set(slot, t == null ? "" : t.id());
		return new TechniqueData(levels, spentPoints, emeralds, s, cooldownUntil);
	}

	public TechniqueData withoutFromSlots(Technique t) {
		List<String> s = new ArrayList<>(slots);
		s.replaceAll(id -> id.equals(t.id()) ? "" : id);
		return new TechniqueData(levels, spentPoints, emeralds, s, cooldownUntil);
	}

	public TechniqueData withCooldown(Technique t, long untilTick) {
		Map<String, Long> c = new HashMap<>(cooldownUntil);
		c.put(t.id(), untilTick);
		return new TechniqueData(levels, spentPoints, emeralds, slots, c);
	}

	public TechniqueData withoutCooldowns() {
		return new TechniqueData(levels, spentPoints, emeralds, slots, Map.of());
	}

	/** Which slots hold a technique (for the skill key cycle). */
	public boolean[] filledSlots() {
		int open = slotsOpen();
		boolean[] f = new boolean[TechniqueRules.MAX_SLOTS];
		for (int i = 0; i < f.length; i++) {
			f[i] = i < open && inSlot(i) != null;
		}
		return f;
	}
}
