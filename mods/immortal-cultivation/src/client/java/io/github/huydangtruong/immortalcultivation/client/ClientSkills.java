package io.github.huydangtruong.immortalcultivation.client;

import io.github.huydangtruong.immortalcultivation.HeartsAttachment;
import io.github.huydangtruong.immortalcultivation.technique.SeismRules;
import io.github.huydangtruong.immortalcultivation.technique.SkillBarLogic;
import io.github.huydangtruong.immortalcultivation.technique.Technique;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueAttachment;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueData;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueNet;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueRules;
import io.github.huydangtruong.immortalcultivation.technique.TechniqueSelection;
import io.github.huydangtruong.immortalcultivation.technique.WheelLogic;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.resources.Identifier;

/**
 * Client side of the skill bar (P-045, D125): four slot keys (Z X C V) select a technique slot, the mouse wheel runs over
 * the hotbar and then the filled technique slots, charging with right click, casting. The server validates every cast
 * again; this only asks. While a technique is selected the real inventory slot stays where it was.
 */
public final class ClientSkills {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("permahearts", "main"));
	/** Slot keys 1..4; SDL scancodes (not GLFW): Z = 29, X = 27, C = 6, V = 25. */
	public static final KeyMapping[] KEYS = {
		slotKey(1, com.mojang.blaze3d.platform.InputConstants.KEY_Z),
		slotKey(2, com.mojang.blaze3d.platform.InputConstants.KEY_X),
		slotKey(3, com.mojang.blaze3d.platform.InputConstants.KEY_C),
		slotKey(4, com.mojang.blaze3d.platform.InputConstants.KEY_V)
	};

	/** D188: hold to open the technique wheel while a technique slot is selected. */
	public static final KeyMapping WHEEL = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.permahearts.wheel", com.mojang.blaze3d.platform.InputConstants.KEY_R, CATEGORY));

	/** Selected technique slot of the local player: -1 = the hotbar (a real item), 0..3 = a technique slot. */
	private static int selected = -1;
	private static LocalPlayer lastPlayer;
	private static boolean charging;
	private static int chargeTicks;
	private static boolean useWasDown;
	private static final String MAYHEM_HOLD_HINT = "Hold right click for 1 s to start Light Strike Reflection";

	private ClientSkills() {}

	private static KeyMapping slotKey(int n, int key) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.permahearts.slot_" + n, key, CATEGORY));
	}

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

	/** Client game tests cannot press a real key: they set this to keep the wheel open. */
	public static boolean testWheelHeld;

	/** True while the wheel key is physically held (read from the window state: an open screen swallows key events). */
	public static boolean wheelKeyDown() {
		if (testWheelHeld) {
			return true;
		}
		var key = com.mojang.blaze3d.platform.InputConstants.getKey(WHEEL.saveString());
		return key.getType() != com.mojang.blaze3d.platform.InputConstants.Type.MOUSE && key.getValue() >= 0
			&& com.mojang.blaze3d.platform.InputConstants.isKeyDown(key.getValue());
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
		return (charging && !seismLocked(Minecraft.getInstance())) || ClientGrasp.tethering();
	}

	/** D142: below Lv 3 Seism has no Wave, so no charge bar or zoom is shown while right click is held. */
	private static boolean seismLocked(Minecraft mc) {
		Technique t = selectedTechnique(mc);
		return t == Technique.SEISM && data(mc.player).level(t) < SeismRules.WAVE_MIN_LEVEL;
	}

	public static float chargeFraction() {
		return ClientGrasp.tethering() ? ClientGrasp.fraction() : currentCharge();
	}

	private static float currentCharge() {
		Technique t = selectedTechnique(Minecraft.getInstance());
		return t == null ? SkillBarLogic.charge(chargeTicks) : SkillBarLogic.charge(chargeTicks, TechniqueRules.fullChargeSeconds(t));
	}

	// ---- changes ----

	public static void select(int index) {
		boolean changed = selected != index;
		selected = index;
		if (changed) {
			charging = false;
		}
		if (changed && Minecraft.getInstance().player != null) {
			ClientPlayNetworking.send(new TechniqueNet.SkillAction(TechniqueNet.SELECT, index, 0f));
		}
	}

	/** Slot numbers of the filled, usable techniques in slot order (the technique part of the scroll ring). */
	public static int[] usableSlots(Minecraft mc) {
		return SkillBarLogic.filledSlots(usableFlags(mc));
	}

	private static boolean[] usableFlags(Minecraft mc) {
		TechniqueData d = data(mc.player);
		boolean[] f = d.filledSlots();
		for (int i = 0; i < f.length; i++) {
			if (f[i]) {
				f[i] = d.level(d.inSlot(i)) >= 1;
			}
		}
		return f;
	}

	/** A slot key: selects that technique slot when it holds a usable technique, else nothing. */
	private static void pressSlotKey(Minecraft mc, int keyIndex) {
		int slot = SkillBarLogic.slotForKey(keyIndex, usableFlags(mc));
		if (slot >= 0) {
			select(slot);
		}
	}

	/**
	 * Mouse wheel (MouseScrollMixin): moves along the ring hotbar 0..8, then the filled techniques. Returns the inventory slot
	 * to set (a technique position leaves the real slot unchanged). {@code wheel} has vanilla's sign (negative = down).
	 */
	public static int scroll(Minecraft mc, double wheel, int hotbarSlot) {
		if (ClientChrono.scroll(mc, wheel)) {
			return hotbarSlot; // D268: while time is stopped the wheel scrubs time
		}
		int delta = -(int) Math.signum(wheel);
		int[] slots = available(mc) ? usableSlots(mc) : new int[0];
		if (slots.length == 0) {
			return net.minecraft.client.ScrollWheelHandler.getNextScrollWheelSelection(wheel, hotbarSlot, Inventory.getSelectionSize());
		}
		int pos = SkillBarLogic.ringPosition(hotbarSlot, selected, slots);
		int next = SkillBarLogic.scrollRing(pos, delta, slots.length);
		select(SkillBarLogic.techniqueAtRing(next, slots));
		return next < SkillBarLogic.HOTBAR ? next : hotbarSlot;
	}

	/** D193: slot the wheel opens on: the selected one (empty is fine), or from the cross the lowest ready slot; -1 = none open. */
	public static int wheelStartSlot(Minecraft mc) {
		TechniqueData d = data(mc.player);
		int open = d.slotsOpen();
		if (selected >= 0 && selected < open) {
			return selected;
		}
		long now = mc.level.getGameTime();
		boolean[] usable = usableFlags(mc);
		boolean[] cooling = new boolean[usable.length];
		for (int i = 0; i < cooling.length; i++) {
			cooling[i] = d.slotOnCooldown(i, now);
		}
		return WheelLogic.defaultSlot(open, usable, cooling);
	}

	/** Ticks an empty selected slot is kept after the wheel assigned into it (the server data needs a moment to come back). */
	private static int fillGrace;

	/** Forgets a charge and the fill grace of the old world (B5): called when the client disconnects. */
	static void resetOnDisconnect() {
		charging = false;
		chargeTicks = 0;
		useWasDown = false;
		fillGrace = 0;
	}

	/** Called by the wheel after it asked the server to fill the selected slot. */
	static void keepSlotForFill() {
		fillGrace = 20;
	}

	/** A number key or other hotbar change: back to the hotbar. */
	public static void backToHotbar() {
		select(-1);
	}

	/** Casts the technique in a slot (asks the server). Returns false when it is on cooldown (message shown). */
	public static boolean cast(Minecraft mc, int slot, float charge) {
		TechniqueData d = data(mc.player);
		Technique t = d.inSlot(slot);
		if (t == null) {
			return false;
		}
		long left = t == Technique.WARP || t == Technique.MAYHEM ? 0 : d.cooldownLeft(t, mc.level.getGameTime()); // D75: placing a portal has no cooldown; D174: Mayhem keeps its own state (the server answers)
		if (t == Technique.CHRONO && ClientChrono.own(mc) != null) {
			left = 0; // D268: the recast lets time run again
		}
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
			if (selectedTechnique(mc) == Technique.LANCE) {
				if (cast(mc, selected, 1f)) {
					ClientLance.started(1); // D159: the beam lasts while this button stays held
				}
			} else if (selectedTechnique(mc) == Technique.GRASP) {
				ClientGrasp.openList(); // D134: left click opens the list of grasped mobs
			} else if (selectedTechnique(mc) == Technique.WARP) {
				ClientPlayNetworking.send(new io.github.huydangtruong.immortalcultivation.technique.warp.Warp.Request(io.github.huydangtruong.immortalcultivation.technique.warp.Warp.OPEN, "")); // D75: left click opens the warp menu
			} else if (selectedTechnique(mc) == Technique.SEISM) {
				cast(mc, selected, 0f); // D142: left click is always the boulder (a full charge would start the Wave)
			} else if (selectedTechnique(mc) == Technique.MAYHEM) {
				cast(mc, selected, 0f); // D174: left click = one shard (a full charge would be the channel)
			} else if (selectedTechnique(mc) == Technique.CRYSTAL) {
				cast(mc, selected, 0f); // D143: left click cages what you aim at (right click sends 1 = cage around you)
			} else {
				cast(mc, selected, 1f);
			}
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
		boolean wheelOpen = mc.gui.screen() instanceof TechniqueWheelScreen;
		if (fillGrace > 0) {
			fillGrace--;
		}
		boolean slotGone = !wheelOpen && fillGrace == 0 && SkillBarLogic.slotForKey(selected, usableFlags(mc)) < 0; // D193: an empty slot stays selected while the wheel fills it
		if (selected >= 0 && ((mc.gui.screen() != null && !wheelOpen) || !available(mc) || slotGone)) {
			select(-1); // a screen opened, world switch off, creative, or the slot was cleared
		}

		FocusParticles.tick(mc);
		ClientGrasp.tick(mc, selectedTechnique(mc) == Technique.GRASP);
		boolean inGame = mc.gui.screen() == null && available(mc);
		tickKey(mc, inGame);
		tickUse(mc, inGame);
	}

	private static void reset() {
		selected = -1;
		charging = false;
		useWasDown = false;
	}

	private static void tickKey(Minecraft mc, boolean inGame) {
		boolean wheel = false;
		while (WHEEL.consumeClick()) {
			wheel = true;
		}
		if (wheel && inGame) {
			int slot = wheelStartSlot(mc);
			if (slot >= 0) {
				select(slot); // D193: from the cross the wheel picks the lowest ready slot and really selects it
				mc.gui.setScreen(new TechniqueWheelScreen(slot)); // D188
			}
		}
		for (int i = 0; i < KEYS.length; i++) {
			boolean click = false;
			while (KEYS[i].consumeClick()) {
				click = true;
			}
			if (click && inGame) {
				pressSlotKey(mc, i);
			}
		}
	}

	/** Right click: charge while held, cast on release (instant techniques cast on press). */
	private static void tickUse(Minecraft mc, boolean inGame) {
		Technique t = inGame ? selectedTechnique(mc) : null;
		boolean down = mc.options.keyUse.isDown();
		if (t != Technique.GRASP) {
			ClientGrasp.cancelIfRunning();
		} else {
			ClientGrasp.tickUse(mc, down, useWasDown);
			charging = false;
			useWasDown = down;
			return;
		}
		if (t == null) {
			charging = false;
			useWasDown = down;
			return;
		}
		if (down && !useWasDown) {
			if (t.charged()) {
				TechniqueData d = data(mc.player);
				long left = t == Technique.WARP || t == Technique.MAYHEM ? 0 : d.cooldownLeft(t, mc.level.getGameTime()); // D75: placing a portal has no cooldown
				if (left > 0) {
					cast(mc, selected, 1f); // only shows the cooldown message
				} else {
					charging = true;
					chargeTicks = 0;
				}
			} else if (cast(mc, selected, 1f) && t == Technique.LANCE) {
				ClientLance.started(2); // D159: the beam runs while the right button stays down
			}
		} else if (down && charging) {
			chargeTicks++;
		} else if (!down && charging) {
			float c = currentCharge();
			charging = false;
			if (t == Technique.MAYHEM && c < 1f) {
				mc.player.sendOverlayMessage(Component.literal(MAYHEM_HOLD_HINT)); // D174: let go too early, nothing happens
			} else if (t == Technique.WARP && !TechniqueRules.warpPlaceReady(c)) {
				mc.player.sendOverlayMessage(Component.literal(TechniqueNet.WARP_HOLD_HINT)); // D79: let go too early, no portal
			} else {
				if (t == Technique.SEISM && c >= 1f && data(mc.player).level(t) < SeismRules.WAVE_MIN_LEVEL) {
					mc.player.sendOverlayMessage(Component.literal(SeismRules.WAVE_LOCKED_HINT));
				}
				cast(mc, selected, c);
			}
		}
		useWasDown = down;
	}
}
