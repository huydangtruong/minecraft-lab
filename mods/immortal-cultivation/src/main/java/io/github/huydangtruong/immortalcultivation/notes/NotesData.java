package io.github.huydangtruong.immortalcultivation.notes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.huydangtruong.immortalcultivation.notes.NoteRules.Note;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** P-042 (D117): per-world notes: one shared list (newest first) and one private list per player UUID. */
public class NotesData extends SavedData {
	private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

	private static final Codec<Note> NOTE_CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.LONG.fieldOf("id").forGetter(Note::id),
		UUID_CODEC.fieldOf("author").forGetter(Note::author),
		Codec.STRING.fieldOf("name").forGetter(Note::authorName),
		Codec.STRING.fieldOf("text").forGetter(Note::text),
		Codec.LONG.optionalFieldOf("time", 0L).forGetter(Note::time)
	).apply(i, Note::new));

	private static final Codec<NotesData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.LONG.optionalFieldOf("next_id", 1L).forGetter(d -> d.nextId),
		NOTE_CODEC.listOf().optionalFieldOf("shared", List.of()).forGetter(d -> d.shared),
		Codec.unboundedMap(Codec.STRING, NOTE_CODEC.listOf()).optionalFieldOf("private", Map.of()).forGetter(d -> {
			Map<String, List<Note>> m = new HashMap<>();
			d.privateNotes.forEach((k, v) -> m.put(k.toString(), v));
			return m;
		})
	).apply(i, NotesData::new));

	public static final SavedDataType<NotesData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath("permahearts", "notes"), NotesData::new, CODEC, null);

	private long nextId = 1;
	private List<Note> shared = new ArrayList<>();
	private final Map<UUID, List<Note>> privateNotes = new HashMap<>();

	public NotesData() {}

	private NotesData(long nextId, List<Note> shared, Map<String, List<Note>> priv) {
		this.nextId = nextId;
		this.shared = new ArrayList<>(shared);
		priv.forEach((k, v) -> privateNotes.put(UUID.fromString(k), new ArrayList<>(v)));
	}

	public static NotesData of(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Note> shared() {
		return shared;
	}

	public List<Note> privateOf(UUID player) {
		return privateNotes.getOrDefault(player, List.of());
	}

	/** Adds a note for a player. Returns null when added, else the reason. */
	public String add(boolean sharedTab, UUID author, String name, String text, long time) {
		List<Note> list = sharedTab ? shared : privateOf(author);
		NoteRules.AddResult r = NoteRules.add(list, sharedTab, author, name, text, nextId, time);
		if (!r.ok()) {
			return r.error();
		}
		nextId++;
		if (sharedTab) {
			shared = new ArrayList<>(r.list());
		} else {
			privateNotes.put(author, new ArrayList<>(r.list()));
		}
		setDirty();
		return null;
	}

	/** Deletes the player's own note. Returns true when something was removed. */
	public boolean delete(boolean sharedTab, UUID who, long id) {
		List<Note> list = sharedTab ? shared : privateOf(who);
		List<Note> out = NoteRules.delete(list, id, who);
		if (out.size() == list.size()) {
			return false;
		}
		if (sharedTab) {
			shared = new ArrayList<>(out);
		} else {
			privateNotes.put(who, new ArrayList<>(out));
		}
		setDirty();
		return true;
	}
}
