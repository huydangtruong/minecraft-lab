package io.github.huydangtruong.immortalcultivation.technique.grasp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * The `permahearts:grasped_mob` attachment on a mob held by Soul Grasp (D134): saved with the entity. bonusApplied is the
 * technique level whose health bonus the mob carries now (0 = none yet); given lists the equipment slots that hold an item
 * the owner threw (D135), so release drops only those.
 */
public record GraspedMob(UUID owner, int number, int mode, int guardX, int guardY, int guardZ, int bonusApplied, List<String> given) {
	public static final Codec<GraspedMob> CODEC = RecordCodecBuilder.create(i -> i.group(
		UUIDUtil.CODEC.fieldOf("owner").forGetter(GraspedMob::owner),
		Codec.INT.optionalFieldOf("number", 1).forGetter(GraspedMob::number),
		Codec.INT.optionalFieldOf("mode", 0).forGetter(GraspedMob::mode),
		Codec.INT.optionalFieldOf("guard_x", 0).forGetter(GraspedMob::guardX),
		Codec.INT.optionalFieldOf("guard_y", 0).forGetter(GraspedMob::guardY),
		Codec.INT.optionalFieldOf("guard_z", 0).forGetter(GraspedMob::guardZ),
		Codec.INT.optionalFieldOf("bonus_applied", 0).forGetter(GraspedMob::bonusApplied),
		Codec.STRING.listOf().optionalFieldOf("given", List.of()).forGetter(GraspedMob::given)
	).apply(i, GraspedMob::new));

	public static final AttachmentType<GraspedMob> ATTACHMENT = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "grasped_mob"), b -> b.persistent(CODEC));

	public GraspedMob withMode(int m, int x, int y, int z) {
		return new GraspedMob(owner, number, m, x, y, z, bonusApplied, given);
	}

	public GraspedMob withBonus(int level) {
		return new GraspedMob(owner, number, mode, guardX, guardY, guardZ, level, given);
	}

	public GraspedMob withGiven(List<String> g) {
		return new GraspedMob(owner, number, mode, guardX, guardY, guardZ, bonusApplied, g);
	}
}
