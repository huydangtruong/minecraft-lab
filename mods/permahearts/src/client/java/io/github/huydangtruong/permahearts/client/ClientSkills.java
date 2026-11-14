package io.github.huydangtruong.permahearts.client;

import io.github.huydangtruong.permahearts.HeartsAttachment;
import io.github.huydangtruong.permahearts.technique.SkillBarLogic;
import io.github.huydangtruong.permahearts.technique.Technique;
import io.github.huydangtruong.permahearts.technique.TechniqueAttachment;
import io.github.huydangtruong.permahearts.technique.TechniqueData;
import io.github.huydangtruong.permahearts.technique.TechniqueNet;
import io.github.huydangtruong.permahearts.technique.TechniqueRules;
import io.github.huydangtruong.permahearts.technique.TechniqueSelection;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Client side of the skill bar (P-011b): the selected entry, the skill key (tap = next, hold = wheel), charging with
 * right click, casting. The server validates every cast again; this only asks.
 */
public final class ClientSkills {
	public static final KeyMapping KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.permahearts.technique", 82 /* GLFW_KEY_R */, KeyMapping.Category.register(Identifier.fromNamespaceAndPath("permahearts", "main"))));

	/** Selected entry of the local player: -1 = the cross, 0..3 = a slot. */
	private static int selected = -1;
	private static LocalPlayer lastPlayer;
	private static int lastHotbar = -1;
	private static boolean keyTracking;
	private static int keyHeld;
	private static boolean charging;
	private static int chargeTicks;
	private static boolean useWasDown;

	private ClientSkills() {}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ClientSkills::tick);
	}

	// ---- reads ----

	public static TechniqueData data(LocalPlayer p) {
		return p.getAttachedOrElse(TechniqueAttachment.TECHNIQUES, TechniqueData.DEFAULT);
	}

	/** Techniques are usable: world switch on, not creative, not spectator. (Same conditions as the HeartXP bar.) */
	public static boolean available(Minecraft mc) {
		LocalPlayer p = mc.player;
		return p != null && mc.level != null && !p.isCreative() && !p.isSpectator()
			&& p.getAttachedOrElse(HeartsAttachment.ACTIVE, false);
	}

	public static int selected() {
		return selected;
	}

	/** The technique in the selected slot, or null (cross, empty slot, level 0, not available). */
	public static Technique selectedTechnique(Minecraft mc) {
		if (selected < 0 || !available(mc)) {
			return null;
		}
		TechniqueData d = data(mc.player);
		Technique t = selected < d.slotsOpen() ? d.inSlot(selected) : null;
		return t != null && d.level(t) >= 1 ? t : null;
	}

	/** Technique shown in a player's hand (focus item), or null. The local player uses the local selection. */
	public static Technique focusOf(net.minecraft.world.entity.player.Player p) {
		Minecraft mc = Minecraft.getInstance();
		if (p == mc.player) {
			return selectedTechnique(mc);
		}
		return Technique.byId(p.getAttachedOrElse(TechniqueSelection.SELECTED, TechniqueSelection.Selection.NONE).technique()).orElse(null);
	}

	public static boolean charging() {
		return charging;
	}

	public static float chargeFraction() {
		return SkillBarLogic.charge(chargeTicks);
	}

	// ---- changes ----

	public static void select(int index) {
		selected = index;
		charging = false;
		if (Minecraft.getInstance().player != null) {
			ClientPlayNetworking.send(new TechniqueNet.SkillAction(TechniqueNet.SELECT, index, 0f));
		}
	}

	/** Next entry like the skill key tap. */
	public static void next(Minecraft mc) {
		if (!available(mc)) {
			return;
		}
		select(TechniqueRules.nextSelection(selected, data(mc.player).filledSlots()));
	}

	/** Casts the technique in a slot (asks the server). Returns false when it is on cooldown (message shown). */
	public static boolean cast(Minecraft mc, int slot, float charge) {
		TechniqueData d = data(mc.player);
		Technique t = d.inSlot(slot);
		if (t == null) {
			return false;
		}
		long left = d.cooldownLeft(t, mc.level.getGameTime());
		if (left > 0) {
			mc.player.sendOverlayMessage(Component.literal("On cooldown: " + SkillBarLogic.secondsLeft(left) + " s"));
			return false;
		}
		ClientPlayNetworking.send(new TechniqueNet.SkillAction(TechniqueNet.CAST, slot, charge));
		return true;
	}

	// ---- input hooks (MinecraftInputMixin) ----

	/** True while a technique is selected: normal item use and attacks are cancelled. */
	public static boolean intercepting(Minecraft mc) {
		return selectedTechnique(mc) != null;
	}

	/** Left click: throws at once with full charge. */
	public static void onAttackClick(Minecraft mc) {
		if (mc.gui.screen() == null && intercepting(mc)) {
			cast(mc, selected, 1f);
		}
	}

	// ---- tick ----

	private static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			reset();
			lastPlayer = null;
			return;
		}
		if (p != lastPlayer) { // joined, respawned or changed dimension: start on the cross
			lastPlayer = p;
			reset();
		}
		if (selected >= 0 && selectedTechnique(mc) == null) {
			select(-1);
		}
		int hotbar = p.getInventory().getSelectedSlot();
		if (lastHotbar != -1 && hotbar != lastHotbar && selected >= 0) {
			select(-1); // scrolling the hotbar or pressing 1-9 goes back to the real item
		}
		lastHotbar = hotbar;

		boolean inGame = mc.gui.screen() == null && available(mc);
		tickKey(mc, inGame);
		tickUse(mc, inGame);
	}

	private static void reset() {
		selected = -1;
		charging = false;
		keyTracking = false;
		lastHotbar = -1;
		useWasDown = false;
	}

	/** Skill key: tap (shorter than TAP_TICKS) = next entry, hold = the radial wheel. */
	private static void tickKey(Minecraft mc, boolean inGame) {
		boolean click = false;
		while (KEY.consumeClick()) {
			click = true;
		}
		if (!inGame) {
			keyTracking = false;
			return;
		}
		if (click && !keyTracking) {
			keyTracking = true;
			keyHeld = 0;
		}
		if (!keyTracking) {
			return;
		}
		if (KEY.isDown()) {
			keyHeld++;
			if (keyHeld >= SkillBarLogic.TAP_TICKS) {
				keyTracking = false;
				mc.setScreenAndShow(new SkillWheelScreen());
			}
		} else {
			keyTracking = false;
			next(mc);
		}
	}

	/** Right click: charge while held, cast on release (instant techniques cast on press). */
	private static void tickUse(Minecraft mc, boolean inGame) {
		Technique t = inGame ? selectedTechnique(mc) : null;
		boolean down = mc.options.keyUse.isDown();
		if (t == null) {
			charging = false;
			useWasDown = down;
			return;
		}
		if (down && !useWasDown) {
			if (t.charged()) {
				TechniqueData d = data(mc.player);
				long left = d.cooldownLeft(t, mc.level.getGameTime());
				if (left > 0) {
					cast(mc, selected, 1f); // only shows the cooldown message
				} else {
					charging = true;
					chargeTicks = 0;
				}
			} else {
				cast(mc, selected, 1f);
			}
		} else if (down && charging) {
			chargeTicks++;
		} else if (!down && charging) {
			charging = false;
			cast(mc, selected, SkillBarLogic.charge(chargeTicks));
		}
		useWasDown = down;
	}
}
