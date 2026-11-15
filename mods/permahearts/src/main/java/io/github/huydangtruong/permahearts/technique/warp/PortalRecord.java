package io.github.huydangtruong.permahearts.technique.warp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * One portal of the network (D75), as stored in the world. Pure data: the id is also the UUID of the portal entity.
 *
 * @param id        portal id (= entity UUID)
 * @param owner     UUID of the player who placed it
 * @param dimension dimension id, e.g. "minecraft:overworld"
 * @param x         exact position of the entity (centre of the floor)
 * @param yaw       the way the portal faces
 * @param name      auto name, unique per owner
 * @param created   overworld game time at creation, for ordering (oldest first)
 */
public record PortalRecord(UUID id, UUID owner, String dimension, double x, double y, double z, float yaw, String name, long created) {
	public static final Codec<PortalRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
		UUIDUtil.CODEC.fieldOf("id").forGetter(PortalRecord::id),
		UUIDUtil.CODEC.fieldOf("owner").forGetter(PortalRecord::owner),
		Codec.STRING.fieldOf("dimension").forGetter(PortalRecord::dimension),
		Codec.DOUBLE.fieldOf("x").forGetter(PortalRecord::x),
		Codec.DOUBLE.fieldOf("y").forGetter(PortalRecord::y),
		Codec.DOUBLE.fieldOf("z").forGetter(PortalRecord::z),
		Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(PortalRecord::yaw),
		Codec.STRING.fieldOf("name").forGetter(PortalRecord::name),
		Codec.LONG.optionalFieldOf("created", 0L).forGetter(PortalRecord::created)
	).apply(i, PortalRecord::new));

	/** Every portal of the world. */
	public record All(List<PortalRecord> portals) {
		public static final All EMPTY = new All(List.of());
		public static final Codec<All> CODEC = PortalRecord.CODEC.listOf().xmap(All::new, All::portals);

		public All {
			portals = List.copyOf(portals);
		}

		/** The portals of one owner, oldest first. */
		public List<PortalRecord> of(UUID owner) {
			List<PortalRecord> out = new ArrayList<>();
			for (PortalRecord p : portals) {
				if (p.owner().equals(owner)) {
					out.add(p);
				}
			}
			out.sort(Comparator.comparingLong(PortalRecord::created));
			return out;
		}

		public PortalRecord find(UUID id) {
			for (PortalRecord p : portals) {
				if (p.id().equals(id)) {
					return p;
				}
			}
			return null;
		}

		public All with(PortalRecord p) {
			List<PortalRecord> l = new ArrayList<>(portals);
			l.add(p);
			return new All(l);
		}

		public All without(UUID id) {
			List<PortalRecord> l = new ArrayList<>(portals);
			l.removeIf(p -> p.id().equals(id));
			return new All(l);
		}
	}
}
