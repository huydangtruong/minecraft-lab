package io.github.huydangtruong.permahearts.client;

import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The God Slayer model (P-034, P-035, D98, D101): an oni 5 blocks tall in a wide fighting stance, arms angled forward
 * with a straight katana in each fist, two curved horns, a long white mane, a big face with slanted angry eyes, tusks,
 * and two big wings that only show while it flies (the body then leans forward). Built in code, one unit = one
 * pixel (16 per block); the feet stand on y = 24 like every vanilla entity model. Texture 128x320, drawn by
 * tools/make_god_slayer_texture.py (the box sizes there must match the ones here).
 * The entity's right arm is the model's -x side; it is the one that slashes.
 */
public class GodSlayerModel extends EntityModel<GodSlayerRenderState> {
	/** Texture offsets of every box, printed by `make_god_slayer_texture.py --uv`. */
	private static final Map<String, int[]> UV = Map.ofEntries(
		Map.entry("arm", new int[] {76, 145}),
		Map.entry("belt", new int[] {62, 212}),
		Map.entry("blade", new int[] {68, 86}),
		Map.entry("brow", new int[] {0, 276}),
		Map.entry("chest", new int[] {0, 53}),
		Map.entry("collar", new int[] {0, 145}),
		Map.entry("fang", new int[] {84, 264}),
		Map.entry("feather", new int[] {0, 0}),
		Map.entry("feather_short", new int[] {86, 53}),
		Map.entry("fist", new int[] {62, 249}),
		Map.entry("foot", new int[] {0, 249}),
		Map.entry("forearm", new int[] {62, 168}),
		Map.entry("guard", new int[] {100, 264}),
		Map.entry("hair_back", new int[] {0, 86}),
		Map.entry("hair_lock", new int[] {44, 168}),
		Map.entry("hair_low", new int[] {62, 116}),
		Map.entry("hair_side", new int[] {20, 0}),
		Map.entry("hair_top", new int[] {0, 191}),
		Map.entry("hair_up", new int[] {0, 233}),
		Map.entry("handle", new int[] {88, 233}),
		Map.entry("head", new int[] {0, 116}),
		Map.entry("horn1", new int[] {36, 233}),
		Map.entry("horn2", new int[] {64, 233}),
		Map.entry("horn3", new int[] {42, 249}),
		Map.entry("horn4", new int[] {56, 264}),
		Map.entry("knot", new int[] {68, 264}),
		Map.entry("shin", new int[] {26, 212}),
		Map.entry("spike", new int[] {44, 264}),
		Map.entry("tasset_front", new int[] {0, 264}),
		Map.entry("tasset_side", new int[] {0, 212}),
		Map.entry("thigh", new int[] {0, 168}),
		Map.entry("waist", new int[] {68, 191}),
		Map.entry("wing_bone", new int[] {38, 276}));

	/** Resting pose angles: arms open outwards, forearms folded up so the katanas stand straight. */
	private static final float ARM_OUT = 1.15f;
	private static final float ARM_FORWARD = 0.9f;
	private static final float FOREARM_UP = 1.5f;
	private static final float HEAD_SCALE = 1.6f;
	/** Forward lean of the body at full speed in the air: 45 degrees. */
	private static final float MAX_LEAN = 0.785f;
	private static final float HIP_OUT = 0.28f;
	private static final float THIGH_FWD = -0.3f;
	private static final float SHIN_BACK = 0.55f;

	private final ModelPart body;
	private final ModelPart chest;
	private final ModelPart head;
	private final ModelPart hairBack;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightShoulder;
	private final ModelPart leftShoulder;
	private final ModelPart rightForearm;
	private final ModelPart leftForearm;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightShin;
	private final ModelPart leftShin;
	private final ModelPart rightFoot;
	private final ModelPart leftFoot;

	public GodSlayerModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		chest = body.getChild("chest");
		head = chest.getChild("head");
		hairBack = chest.getChild("hair_back");
		rightShoulder = chest.getChild("right_shoulder");
		leftShoulder = chest.getChild("left_shoulder");
		rightArm = rightShoulder.getChild("right_arm");
		leftArm = leftShoulder.getChild("left_arm");
		rightForearm = rightArm.getChild("forearm");
		leftForearm = leftArm.getChild("forearm");
		rightWing = chest.getChild("right_wing");
		leftWing = chest.getChild("left_wing");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
		rightShin = rightLeg.getChild("shin");
		leftShin = leftLeg.getChild("shin");
		rightFoot = rightShin.getChild("foot");
		leftFoot = leftShin.getChild("foot");
	}

	private static CubeListBuilder box(String name, float x, float y, float z, int w, int h, int d) {
		int[] uv = UV.get(name);
		return CubeListBuilder.create().texOffs(uv[0], uv[1]).addBox(x, y, z, w, h, d);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// ---- waist, belt, hip armour ----
		PartDefinition body = root.addOrReplaceChild("body", box("waist", -9, -10, -5.5f, 18, 10, 11), PartPose.offset(0, -2, 0));
		body.addOrReplaceChild("belt", box("belt", -10, -6, -6.5f, 20, 4, 13), PartPose.ZERO);
		body.addOrReplaceChild("knot", box("knot", -2.5f, -7, -9.5f, 5, 6, 3), PartPose.ZERO);
		body.addOrReplaceChild("tasset_front", box("tasset_front", -10, -2, -7.5f, 20, 10, 2), PartPose.rotation(-0.12f, 0, 0));
		body.addOrReplaceChild("tasset_back", box("tasset_front", -10, -2, 5.5f, 20, 10, 2), PartPose.rotation(0.12f, 0, 0));
		body.addOrReplaceChild("tasset_right", box("tasset_side", -11, -2, -5.5f, 2, 10, 11), PartPose.rotation(0, 0, 0.12f));
		body.addOrReplaceChild("tasset_left", box("tasset_side", 9, -2, -5.5f, 2, 10, 11), PartPose.rotation(0, 0, -0.12f));

		// ---- chest, mane down the back (no shoulder or armpit plates, D101) ----
		PartDefinition chest = body.addOrReplaceChild("chest", box("chest", -14, -18, -7.5f, 28, 18, 15), PartPose.offset(0, -10, 0));
		PartDefinition mane = chest.addOrReplaceChild("hair_back", box("hair_back", -14, -22, 7.5f, 28, 24, 6), PartPose.ZERO);
		mane.addOrReplaceChild("hair_low", box("hair_low", -11, 0, -2, 22, 24, 5), PartPose.offsetAndRotation(0, 2, 10, 0.18f, 0, 0));
		chest.addOrReplaceChild("hair_lock_right", box("hair_lock", -13, -17, -9, 5, 18, 4), PartPose.rotation(0, 0, 0.05f));
		chest.addOrReplaceChild("hair_lock_left", box("hair_lock", 8, -17, -9, 5, 18, 4), PartPose.rotation(0, 0, -0.05f));

		// ---- head: face, brow, tusks, horns, mane ----
		PartDefinition head = chest.addOrReplaceChild("head", box("head", -8, -14, -7.5f, 16, 14, 15), PartPose.offset(0, -17, 0).scaled(HEAD_SCALE));
		chest.addOrReplaceChild("collar", box("collar", -10, -4, -9, 20, 5, 18), PartPose.offset(0, -17, 0));
		head.addOrReplaceChild("brow", box("brow", -8, -13, -9, 16, 3, 3), PartPose.ZERO);
		// tusks grow out of the lower jaw, point forward and a little up (pitch -0.35), clear of the eyes
		head.addOrReplaceChild("fang_right", box("fang", -1, -1, -6, 2, 2, 6), PartPose.offsetAndRotation(-6.5f, -1.5f, -7.5f, -0.4f, 0, 0));
		head.addOrReplaceChild("fang_left", box("fang", -1, -1, -6, 2, 2, 6), PartPose.offsetAndRotation(6.5f, -1.5f, -7.5f, -0.4f, 0, 0));
		head.addOrReplaceChild("hair_top", box("hair_top", -8.5f, -17, -8.5f, 17, 4, 17), PartPose.ZERO);
		head.addOrReplaceChild("hair_up", box("hair_up", -7, -15, 7.5f, 14, 12, 4), PartPose.ZERO);
		head.addOrReplaceChild("hair_side_right", box("hair_side", -11, -13, -6.5f, 3, 20, 13), PartPose.rotation(0, 0, 0.08f));
		head.addOrReplaceChild("hair_side_left", box("hair_side", 8, -13, -6.5f, 3, 20, 13), PartPose.rotation(0, 0, -0.08f));
		head.addOrReplaceChild("spike_1", box("spike", -1.5f, -8, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(-5, -16, -3, -0.2f, 0, -0.45f));
		head.addOrReplaceChild("spike_2", box("spike", -1.5f, -9, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(0, -16, -5, -0.7f, 0, 0));
		head.addOrReplaceChild("spike_3", box("spike", -1.5f, -8, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(5, -16, -3, -0.2f, 0, 0.45f));
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			// a horn is three cubes stepping outwards, then up, then curling back in
			PartDefinition h1 = head.addOrReplaceChild("horn_" + n, box("horn1", -3.5f, -8, -3.5f, 7, 8, 7),
				PartPose.offsetAndRotation(side * 7.5f, -9, 2, 0, 0, side * 1.1f).scaled(0.8f));
			PartDefinition h2 = h1.addOrReplaceChild("horn2", box("horn2", -3, -9, -3, 6, 9, 6),
				PartPose.offsetAndRotation(0, -7.5f, 0, 0, 0, side * -0.8f));
			PartDefinition h3 = h2.addOrReplaceChild("horn3", box("horn3", -2.5f, -9, -2.5f, 5, 9, 5),
				PartPose.offsetAndRotation(0, -8.5f, 0, 0, 0, side * -0.5f));
			h3.addOrReplaceChild("horn4", box("horn4", -1.5f, -8, -1.5f, 3, 8, 3),
				PartPose.offsetAndRotation(0, -8.5f, 0, 0, 0, side * -0.55f));
		}

		// ---- arms: raised forward-out (the shoulder yaws them forward), forearm folded up, a straight katana in each fist ----
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			PartDefinition shoulder = chest.addOrReplaceChild(n + "_shoulder", CubeListBuilder.create(),
				PartPose.offsetAndRotation(side * 17, -14, 0, 0, side * ARM_FORWARD, 0));
			PartDefinition arm = shoulder.addOrReplaceChild(n + "_arm", box("arm", -4.5f, -2, -4.5f, 9, 14, 9),
				PartPose.offsetAndRotation(0, 0, 0, 0, 0, side * -ARM_OUT));
			PartDefinition fore = arm.addOrReplaceChild("forearm", box("forearm", -4, 0, -4, 8, 14, 8),
				PartPose.offsetAndRotation(0, 11, 0, 0, 0, side * -FOREARM_UP));
			PartDefinition fist = fore.addOrReplaceChild("fist", box("fist", -3.5f, -1, -3.5f, 7, 6, 7), PartPose.offset(0, 13, 0));
			PartDefinition katana = fist.addOrReplaceChild("katana", box("handle", -1.5f, -5, -1.5f, 3, 12, 3), PartPose.offset(0, 2, 0));
			katana.addOrReplaceChild("guard", box("guard", -4, 7, -2.5f, 8, 2, 5), PartPose.ZERO);
			// one straight blade, no taper; the tip is cut on a slant in the texture (edge side shorter)
			katana.addOrReplaceChild("blade", box("blade", -2, 9, -1, 4, 28, 2), PartPose.ZERO);
		}

		// ---- wings: shown only while flying; a gold arm bone, a fan of membranes at its end and a short fan at the shoulder ----
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			PartDefinition wing = chest.addOrReplaceChild(n + "_wing", CubeListBuilder.create(),
				PartPose.offsetAndRotation(side * 8, -15, 9, 0, -side * 0.15f, 0));
			float x0 = side < 0 ? -40 : 0;
			PartDefinition bone = wing.addOrReplaceChild("bone", box("wing_bone", x0, -1.5f, -1.5f, 40, 3, 3),
				PartPose.rotation(0, 0, -side * 0.5f));
			PartDefinition wrist = bone.addOrReplaceChild("wrist", CubeListBuilder.create(), PartPose.offset(side * 40, 0, 0));
			float[] reach = {0.2f, 0.6f, 1.0f, 1.4f, 1.8f, 2.2f, 2.6f};
			float[] length = {0.8f, 0.92f, 1f, 1f, 0.92f, 0.8f, 0.65f};
			for (int i = 0; i < reach.length; i++) {
				wrist.addOrReplaceChild("feather_" + i, box("feather", -4.5f, 0, -0.5f, 9, 52, 1),
					PartPose.offsetAndRotation(0, 0, i * 0.02f, 0, 0, -side * (Mth.PI - reach[i])).scaled(1, length[i], 1));
			}
			for (int i = 0; i < 3; i++) {
				wing.addOrReplaceChild("inner_" + i, box("feather_short", -4.5f, 0, -0.5f, 9, 32, 1),
					PartPose.offsetAndRotation(0, 0, i * 0.02f, 0, 0, -side * (Mth.PI - (2.2f + i * 0.4f))));
			}
		}

		// ---- legs: wide stance, knees bent ----
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			PartDefinition leg = root.addOrReplaceChild(n + "_leg", box("thigh", -5.5f, 0, -5.5f, 11, 12, 11),
				PartPose.offsetAndRotation(side * 8, -1, 0, THIGH_FWD, 0, side * -HIP_OUT));
			PartDefinition shin = leg.addOrReplaceChild("shin", box("shin", -4.5f, 0, -4.5f, 9, 11, 9),
				PartPose.offsetAndRotation(0, 11.5f, 0, SHIN_BACK, 0, side * HIP_OUT));
			shin.addOrReplaceChild("foot", box("foot", -4.5f, 0, -8, 9, 3, 12), PartPose.offset(0, 10.5f, 0));
		}
		return LayerDefinition.create(mesh, 128, 320);
	}

	@Override
	public void setupAnim(GodSlayerRenderState state) {
		resetPose();
		float speed = Math.min(1f, state.walkAnimationSpeed);
		float swing = Mth.cos(state.walkAnimationPos * 0.6662f) * speed;
		float time = state.ageInTicks;

		// the head follows where the boss looks, a little
		head.yRot = Mth.clamp(state.yRot, -50f, 50f) * Mth.DEG_TO_RAD * 0.6f;
		head.xRot = Mth.clamp(state.xRot, -30f, 30f) * Mth.DEG_TO_RAD * 0.6f;

		// legs: the stance stays, the thighs swing forward and back, the shin folds when the leg goes back, feet stay flat
		rightLeg.xRot = THIGH_FWD + swing * 0.9f;
		leftLeg.xRot = THIGH_FWD - swing * 0.9f;
		rightShin.xRot = SHIN_BACK + Math.max(0f, -swing) * 0.8f;
		leftShin.xRot = SHIN_BACK + Math.max(0f, swing) * 0.8f;
		rightFoot.xRot = -(rightLeg.xRot + rightShin.xRot);
		leftFoot.xRot = -(leftLeg.xRot + leftShin.xRot);

		// flying (D101): wings out and flapping; upright when hovering, leaning forward up to 45 degrees with the horizontal speed
		rightWing.visible = state.flying;
		leftWing.visible = state.flying;
		if (state.flying) {
			float lean = MAX_LEAN * (state.forwardLean ? 1f : Mth.clamp(speed, 0f, 1f));
			body.xRot = lean;
			head.xRot -= lean * 0.7f;
			rightLeg.xRot += lean * 0.7f;
			leftLeg.xRot += lean * 0.7f;
			float flap = Mth.sin(time * 0.25f) * 0.3f;
			rightWing.zRot = flap;
			leftWing.zRot = -flap;
			rightWing.xRot = lean * -0.3f;
			leftWing.xRot = lean * -0.3f;
		}

		// a slow breathing sway of the arms and the mane
		float sway = Mth.sin(time * 0.07f) * 0.03f;
		rightArm.zRot += sway;
		leftArm.zRot -= sway;
		hairBack.xRot = 0.04f + Mth.sin(time * 0.09f) * 0.04f + speed * 0.25f;
		body.yRot = swing * 0.12f;

		// slash: the right arm rises over the head, then swings down; blended in and out so it never jumps
		float p = state.swing;
		if (p > 0f) {
			float w = p < 0.12f ? p / 0.12f : (p > 0.85f ? (1f - p) / 0.15f : 1f);
			float x;
			if (p < 0.3f) {
				float t = p / 0.3f;
				x = -2.9f * (1f - (1f - t) * (1f - t));
			} else {
				float u = (p - 0.3f) / 0.7f;
				x = -2.9f + 2.5f * u * u;
			}
			rightShoulder.yRot = Mth.lerp(w, rightShoulder.yRot, 0f);
			rightArm.xRot = Mth.lerp(w, rightArm.xRot, x);
			rightArm.zRot = Mth.lerp(w, rightArm.zRot, 0.12f);
			rightForearm.zRot = Mth.lerp(w, rightForearm.zRot, 0f);
			chest.yRot = w * -0.4f * Mth.sin(p * Mth.PI);
		}
	}
}
