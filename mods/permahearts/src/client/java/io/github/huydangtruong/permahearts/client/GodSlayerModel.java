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
 * The God Slayer model (P-034, D98): an oni 5 blocks tall in a wide fighting stance, arms open with a big katana held
 * upright in each fist, two curved horns, a thick white mane and bulky shoulder plates. Built in code, one unit = one
 * pixel (16 per block); the feet stand on y = 24 like every vanilla entity model. Texture 128x256, drawn by
 * tools/make_god_slayer_texture.py (the box sizes there must match the ones here).
 * The entity's right arm is the model's -x side; it is the one that slashes.
 */
public class GodSlayerModel extends EntityModel<GodSlayerRenderState> {
	/** Texture offsets of every box, printed by `make_god_slayer_texture.py --uv`. */
	private static final Map<String, int[]> UV = Map.ofEntries(
		Map.entry("arm", new int[] {88, 66}),
		Map.entry("belt", new int[] {0, 176}),
		Map.entry("blade", new int[] {0, 66}),
		Map.entry("blade_tip", new int[] {26, 221}),
		Map.entry("brow", new int[] {36, 221}),
		Map.entry("chest", new int[] {32, 0}),
		Map.entry("collar", new int[] {12, 66}),
		Map.entry("fang", new int[] {112, 208}),
		Map.entry("fist", new int[] {0, 208}),
		Map.entry("foot", new int[] {64, 193}),
		Map.entry("forearm", new int[] {0, 113}),
		Map.entry("guard", new int[] {0, 221}),
		Map.entry("hair_back", new int[] {0, 33}),
		Map.entry("hair_lock", new int[] {94, 90}),
		Map.entry("hair_side", new int[] {0, 0}),
		Map.entry("hair_top", new int[] {32, 113}),
		Map.entry("hair_up", new int[] {44, 90}),
		Map.entry("handle", new int[] {52, 193}),
		Map.entry("head", new int[] {58, 33}),
		Map.entry("horn1", new int[] {0, 193}),
		Map.entry("horn2", new int[] {28, 193}),
		Map.entry("horn3", new int[] {106, 193}),
		Map.entry("horn4", new int[] {84, 208}),
		Map.entry("knot", new int[] {96, 208}),
		Map.entry("pauldron_low", new int[] {66, 176}),
		Map.entry("pauldron_top", new int[] {0, 156}),
		Map.entry("shin", new int[] {64, 156}),
		Map.entry("spike", new int[] {72, 208}),
		Map.entry("tasset_front", new int[] {28, 208}),
		Map.entry("tasset_side", new int[] {58, 135}),
		Map.entry("thigh", new int[] {0, 90}),
		Map.entry("waist", new int[] {0, 135}));

	/** Resting pose angles: arms open outwards, forearms folded up so the katanas stand straight. */
	private static final float ARM_OUT = 0.8f;
	private static final float FOREARM_UP = 2.0f;
	private static final float HIP_OUT = 0.28f;
	private static final float THIGH_FWD = -0.3f;
	private static final float SHIN_BACK = 0.55f;

	private final ModelPart body;
	private final ModelPart chest;
	private final ModelPart head;
	private final ModelPart hairBack;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightForearm;
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
		rightArm = chest.getChild("right_arm");
		leftArm = chest.getChild("left_arm");
		rightForearm = rightArm.getChild("forearm");
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

		// ---- chest, mane down the back, shoulder plates ----
		PartDefinition chest = body.addOrReplaceChild("chest", box("chest", -14, -18, -7.5f, 28, 18, 15), PartPose.offset(0, -10, 0));
		chest.addOrReplaceChild("hair_back", box("hair_back", -12, -17, 7.5f, 24, 28, 5), PartPose.ZERO);
		chest.addOrReplaceChild("hair_lock_right", box("hair_lock", -13, -17, -9, 5, 18, 4), PartPose.rotation(0, 0, 0.05f));
		chest.addOrReplaceChild("hair_lock_left", box("hair_lock", 8, -17, -9, 5, 18, 4), PartPose.rotation(0, 0, -0.05f));
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			PartDefinition plate = chest.addOrReplaceChild("pauldron_" + n, box("pauldron_top", -8, -4, -8, 16, 4, 16),
				PartPose.offsetAndRotation(side * 17, -17, 0, 0, 0, side * 0.22f));
			plate.addOrReplaceChild("low", box("pauldron_low", -7, 0, -7, 14, 3, 14), PartPose.offset(0, -1, 0));
		}

		// ---- head: face, brow, tusks, horns, mane ----
		PartDefinition head = chest.addOrReplaceChild("head", box("head", -8, -14, -7.5f, 16, 14, 15), PartPose.offset(0, -17, 0));
		chest.addOrReplaceChild("collar", box("collar", -10, -4, -9, 20, 5, 18), PartPose.offset(0, -17, 0));
		head.addOrReplaceChild("brow", box("brow", -8, -13, -9, 16, 3, 3), PartPose.ZERO);
		head.addOrReplaceChild("fang_right", box("fang", -6, -9, -9, 3, 6, 2), PartPose.ZERO);
		head.addOrReplaceChild("fang_left", box("fang", 3, -9, -9, 3, 6, 2), PartPose.ZERO);
		head.addOrReplaceChild("hair_top", box("hair_top", -8.5f, -17, -8.5f, 17, 4, 17), PartPose.ZERO);
		head.addOrReplaceChild("hair_up", box("hair_up", -10, -16, 6.5f, 20, 17, 5), PartPose.ZERO);
		head.addOrReplaceChild("hair_side_right", box("hair_side", -11, -13, -6.5f, 3, 20, 13), PartPose.rotation(0, 0, 0.08f));
		head.addOrReplaceChild("hair_side_left", box("hair_side", 8, -13, -6.5f, 3, 20, 13), PartPose.rotation(0, 0, -0.08f));
		head.addOrReplaceChild("spike_1", box("spike", -1.5f, -8, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(-5, -16, -3, -0.2f, 0, -0.45f));
		head.addOrReplaceChild("spike_2", box("spike", -1.5f, -9, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(0, -16, -5, -0.7f, 0, 0));
		head.addOrReplaceChild("spike_3", box("spike", -1.5f, -8, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(5, -16, -3, -0.2f, 0, 0.45f));
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			// a horn is three cubes stepping outwards, then up, then curling back in
			PartDefinition h1 = head.addOrReplaceChild("horn_" + n, box("horn1", -3.5f, -8, -3.5f, 7, 8, 7),
				PartPose.offsetAndRotation(side * 7.5f, -8, -1, 0, 0, side * 1.25f));
			PartDefinition h2 = h1.addOrReplaceChild("horn2", box("horn2", -3, -9, -3, 6, 9, 6),
				PartPose.offsetAndRotation(0, -7.5f, 0, 0, 0, side * -0.8f));
			PartDefinition h3 = h2.addOrReplaceChild("horn3", box("horn3", -2.5f, -9, -2.5f, 5, 9, 5),
				PartPose.offsetAndRotation(0, -8.5f, 0, 0, 0, side * -0.5f));
			h3.addOrReplaceChild("horn4", box("horn4", -1.5f, -8, -1.5f, 3, 8, 3),
				PartPose.offsetAndRotation(0, -8.5f, 0, 0, 0, side * -0.55f));
		}

		// ---- arms: open, forearm folded up, a katana standing in each fist ----
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			PartDefinition arm = chest.addOrReplaceChild(n + "_arm", box("arm", -4.5f, -2, -4.5f, 9, 14, 9),
				PartPose.offsetAndRotation(side * 17, -14, 0, 0, 0, side * -ARM_OUT));
			PartDefinition fore = arm.addOrReplaceChild("forearm", box("forearm", -4, 0, -4, 8, 14, 8),
				PartPose.offsetAndRotation(0, 11, 0, 0, 0, side * -FOREARM_UP));
			PartDefinition fist = fore.addOrReplaceChild("fist", box("fist", -3.5f, -1, -3.5f, 7, 6, 7), PartPose.offset(0, 13, 0));
			PartDefinition katana = fist.addOrReplaceChild("katana", box("handle", -1.5f, -5, -1.5f, 3, 12, 3), PartPose.offset(0, 2, 0));
			katana.addOrReplaceChild("guard", box("guard", -4, 7, -2.5f, 8, 2, 5), PartPose.ZERO);
			PartDefinition blade = katana.addOrReplaceChild("blade", box("blade", -2, 9, -1, 4, 22, 2), PartPose.ZERO);
			blade.addOrReplaceChild("tip", box("blade_tip", -1.5f, 31, -1, 3, 5, 2), PartPose.ZERO);
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
		return LayerDefinition.create(mesh, 128, 256);
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
			rightArm.xRot = Mth.lerp(w, rightArm.xRot, x);
			rightArm.zRot = Mth.lerp(w, rightArm.zRot, 0.12f);
			rightForearm.zRot = Mth.lerp(w, rightForearm.zRot, 0f);
			chest.yRot = w * -0.4f * Mth.sin(p * Mth.PI);
		}
	}
}
