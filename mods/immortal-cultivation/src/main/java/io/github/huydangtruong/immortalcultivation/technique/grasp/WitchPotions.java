package io.github.huydangtruong.immortalcultivation.technique.grasp;

import io.github.huydangtruong.immortalcultivation.technique.OwnedSummon;
import io.github.huydangtruong.immortalcultivation.technique.WitchRules;
import io.github.huydangtruong.immortalcultivation.wanderer.WandererNpc;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;

/**
 * D167: what a grasped Witch throws and whom the splash may touch. Vanilla witches are untouched: only a witch that
 * carries the grasped-mob attachment goes through here. The thrown potion carries a tag with its {@link WitchRules.Intent};
 * the splash filter (ThrownSplashPotionMixin) asks {@link #allows} for every entity in range.
 */
public final class WitchPotions {
	private static final String TAG = "permahearts_witch_";

	private WitchPotions() {}

	public static boolean isGrasped(Entity e) {
		return e instanceof Witch && Grasp.isGrasped(e);
	}

	/** Owner, other mobs grasped by the same owner, and the owner's summons (clones, Violet Guardian, angels). */
	static boolean isAlly(UUID owner, Entity e) {
		return e.getUUID().equals(owner) || Grasp.isGraspedBy(e, owner) || e instanceof OwnedSummon s && owner.equals(s.owner());
	}

	private static UUID ownerOf(Entity witch) {
		GraspedMob ga = witch.getAttached(GraspedMob.ATTACHMENT);
		return ga == null ? null : ga.owner();
	}

	/** Splash filter: may the potion thrown by a grasped witch affect this entity? True for any other potion. */
	public static boolean allows(AbstractThrownPotion potion, LivingEntity target) {
		if (!(potion.getOwner() instanceof Witch w) || !isGrasped(w)) {
			return true;
		}
		WitchRules.Intent intent = null;
		for (WitchRules.Intent i : WitchRules.Intent.values()) {
			if (potion.entityTags().contains(TAG + i.name())) {
				intent = i;
			}
		}
		UUID owner = ownerOf(w);
		if (intent == null || owner == null) {
			return true;
		}
		return WitchRules.affects(intent, isAlly(owner, target), target.isInvertedHealAndHarm(), bystander(w, owner, target));
	}

	/** A player who is not the owner's Death Match opponent, or a wanderer: neither ally nor enemy. */
	private static boolean bystander(Witch w, UUID owner, LivingEntity target) {
		if (WandererNpc.isWanderer(target)) {
			return true;
		}
		if (!(target instanceof Player p) || p.getUUID().equals(owner)) {
			return false;
		}
		ServerPlayer o = w.level() instanceof ServerLevel sl ? sl.getServer().getPlayerList().getPlayer(owner) : null;
		return o == null || !Grasp.isMatchOpponent(o, p);
	}

	/** Replaces Witch#performRangedAttack for a grasped witch: a harmful potion for an enemy (Healing for an undead one). */
	public static void throwAtEnemy(Witch w, LivingEntity target) {
		if (w.isDrinkingPotion()) {
			return;
		}
		double dx = target.getX() - w.getX(), dz = target.getZ() - w.getZ();
		WitchRules.Potion p = WitchRules.enemyPotion(target.isInvertedHealAndHarm(), target.hasEffect(MobEffects.SLOWNESS),
			target.hasEffect(MobEffects.WEAKNESS), target.hasEffect(MobEffects.POISON), target.getHealth(),
			Math.sqrt(dx * dx + dz * dz), w.getRandom().nextFloat());
		shoot(w, target, holder(p), WitchRules.Intent.ENEMY);
	}

	/** Throws Healing or Regeneration at a hurt ally. */
	static void throwAtAlly(Witch w, LivingEntity ally) {
		float fraction = ally.getHealth() / ally.getMaxHealth();
		WitchRules.Potion p = WitchRules.allyPotion(fraction, ally.isInvertedHealAndHarm());
		shoot(w, ally, holder(p), WitchRules.allyIntent(p));
	}

	/** The hurt ally (lowest health fraction) within range, or null. */
	static LivingEntity hurtAlly(Witch w, UUID owner) {
		LivingEntity best = null;
		float bestFraction = 2;
		double r = WitchRules.ALLY_RANGE;
		for (LivingEntity e : w.level().getEntitiesOfClass(LivingEntity.class, w.getBoundingBox().inflate(r), e -> e != w && e.isAlive())) {
			float f = e.getHealth() / e.getMaxHealth();
			if (isAlly(owner, e) && e.distanceToSqr(w) <= r * r && WitchRules.needsHealing(f) && f < bestFraction && w.hasLineOfSight(e)) {
				best = e;
				bestFraction = f;
			}
		}
		return best;
	}

	private static Holder<net.minecraft.world.item.alchemy.Potion> holder(WitchRules.Potion p) {
		return switch (p) {
			case HEALING -> Potions.HEALING;
			case REGENERATION -> Potions.REGENERATION;
			case SLOWNESS -> Potions.SLOWNESS;
			case WEAKNESS -> Potions.WEAKNESS;
			case POISON -> Potions.POISON;
			case HARMING -> Potions.HARMING;
		};
	}

	/** Same aim as vanilla Witch#performRangedAttack, plus the intent tag. */
	private static void shoot(Witch w, LivingEntity target, Holder<net.minecraft.world.item.alchemy.Potion> potion, WitchRules.Intent intent) {
		if (!(w.level() instanceof ServerLevel sl)) {
			return;
		}
		Vec3 v = target.getDeltaMovement();
		double x = target.getX() + v.x - w.getX();
		double y = target.getEyeY() - 1.1 - w.getY();
		double z = target.getZ() + v.z - w.getZ();
		double d = Math.sqrt(x * x + z * z);
		ItemStack stack = PotionContents.createItemStack(Items.SPLASH_POTION, potion);
		Projectile proj = Projectile.spawnProjectileUsingShoot(ThrownSplashPotion::new, sl, stack, w, x, y + d * 0.2, z,
			d <= 2 ? 0.45f : 0.75f, w.rangedAttackUncertainty(sl));
		proj.addTag(TAG + intent.name());
		if (!w.isSilent()) {
			sl.playSound(null, w.getX(), w.getY(), w.getZ(), SoundEvents.WITCH_THROW, w.getSoundSource(), 1.0f, 0.8f + w.getRandom().nextFloat() * 0.4f);
		}
	}
}
