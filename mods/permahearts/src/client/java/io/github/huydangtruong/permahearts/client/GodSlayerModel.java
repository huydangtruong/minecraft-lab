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
		Map.entry("arm", new int[] {0, 75}),
		Map.entry("belt", new int[] {36, 148}),
		Map.entry("blade", new int[] {86, 42}),
		Map.entry("bone_inner", new int[] {24, 0}),
		Map.entry("bone_outer", new int[] {44, 0}),
		Map.entry("brow", new int[] {26, 211}),
		Map.entry("chest", new int[] {0, 42}),
		Map.entry("collar", new int[] {0, 103}),
		Map.entry("fang", new int[] {104, 199}),
		Map.entry("feather_dark", new int[] {0, 0}),
		Map.entry("feather_red", new int[] {12, 0}),
		Map.entry("feather_tip", new int[] {72, 218}),
		Map.entry("fist", new int[] {62, 184}),
		Map.entry("foot", new int[] {0, 184}),
		Map.entry("forearm", new int[] {0, 126}),
		Map.entry("guard", new int[] {0, 211}),
		Map.entry("hair_slice", new int[] {0, 218}),
		Map.entry("hair_top", new int[] {0, 168}),
		Map.entry("handle", new int[] {114, 168}),
		Map.entry("head", new int[] {36, 75}),
		Map.entry("horn1", new int[] {62, 168}),
		Map.entry("horn2", new int[] {90, 168}),
		Map.entry("horn3", new int[] {42, 184}),
		Map.entry("horn4", new int[] {56, 199}),
		Map.entry("joint_elbow", new int[] {90, 184}),
		Map.entry("joint_tip", new int[] {68, 199}),
		Map.entry("knot", new int[] {88, 199}),
		Map.entry("shin", new int[] {0, 148}),
		Map.entry("spike", new int[] {44, 199}),
		Map.entry("tasset_front", new int[] {0, 199}),
		Map.entry("tasset_side", new int[] {90, 126}),
		Map.entry("thigh", new int[] {76, 103}),
		Map.entry("waist", new int[] {32, 126}));

	/** Resting pose angles: arms open outwards, forearms folded up so the katanas stand straight. */
	private static final float ARM_OUT = 1.15f;
	private static final float ARM_FORWARD = 1.05f;
	private static final float FOREARM_UP = 1.5f;
	private static final float HEAD_SCALE = 1.6f; // as in P-035 (depth and height); the width is 95% of that, see HEAD_WIDTH;
	/** Forward lean of the body at full speed in the air: 45 degrees. */
	private static final float MAX_LEAN = 0.785f;
	/** The head pivot (its chin) sits 1 px into the collar so no gap shows. */
	private static final float HEAD_WIDTH = 1.37f;
	private static final float HEAD_Y = -20f;
	private static final float HEAD_YAW_FACTOR = 0.6f;
	private static final float HAIR_TOP = -37f;
	/** The mane is fixed (no sway); its bottom leans back by this angle (back is +z, a negative xRot swings the bottom to +z). */
	private static final float HAIR_TILT = 10f * Mth.DEG_TO_RAD;
	private static final float HAIR_Z = 15f;
	private static final int HAIR_SLICES = 12;
	private static final float HAIR_STEP = 4.4f;
	private static final float HAIR_W_TOP = 26f;
	private static final float HAIR_W_BOTTOM = 33f;
	private static final float WING_ROOT_X = 4f;
	private static final float WING_ROOT_Y = -11f;
	private static final float WING_ROOT_Z = 8.5f;
	private static final float WING_UP = 29f * Mth.DEG_TO_RAD;
	private static final float WING_DOWN = 7f * Mth.DEG_TO_RAD;
	private static final float INNER_LEN = 34f;
	private static final float OUTER_LEN = 33f;
	/** Feather lengths, innermost to outermost: 6 on the inner bone, 5 on the outer one. */
	private static final float[] FEATHER_LEN = {17f, 18.5f, 20f, 21.5f, 28f, 29.5f, 37f, 42f, 45f, 47f, 52f};
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

	private static CubeListBuilder box(String name, float x, float y, float z, float w, float h, float d) {
		int[] uv = UV.get(name);
		return CubeListBuilder.create().texOffs(uv[0], uv[1]).addBox(x, y, z, w, h, d);
	}

	private static float[] unit(float x, float y, float z) {
		float l = Mth.sqrt(x * x + y * y + z * z);
		return new float[] {x / l, y / l, z / l};
	}

	/** A gold bone: a square box hanging along +y from its pivot, turned (z then x) so that +y points along dir. */
	private static void bone(PartDefinition parent, String name, String boxName, float thick, float length, float[] from, float[] dir) {
		float xRot = (float) Math.asin(dir[2]);
		float zRot = (float) Math.atan2(-dir[0], dir[1]);
		parent.addOrReplaceChild(name, box(boxName, -thick / 2, 0, -thick / 2, thick, length, thick),
			PartPose.offsetAndRotation(from[0], from[1], from[2], xRot, 0, zRot));
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
		// one white piece from the top back of the head down to mid-back, a trapezoid: 7 stacked slices, wider at the bottom
		PartDefinition mane = chest.addOrReplaceChild("hair_back", CubeListBuilder.create(), PartPose.offset(0, HAIR_TOP, HAIR_Z));
		for (int i = 0; i < HAIR_SLICES; i++) {
			float w = HAIR_W_TOP + (HAIR_W_BOTTOM - HAIR_W_TOP) * i / (HAIR_SLICES - 1);
			mane.addOrReplaceChild("slice_" + i, box("hair_slice", -17, 0, 0, 34, 4, 2),
				PartPose.offset(0, i * HAIR_STEP / (HAIR_STEP / 4f), 0).scaled(w / 34f, HAIR_STEP / 4f, 1f));
		}

		// ---- head: face, brow, tusks, horns, mane ----
		PartDefinition head = chest.addOrReplaceChild("head", box("head", -8, -10, -7, 16, 10, 13), PartPose.offset(0, HEAD_Y / HEAD_SCALE, 0).scaled(HEAD_WIDTH, HEAD_SCALE, HEAD_SCALE));
		chest.addOrReplaceChild("collar", box("collar", -10, -4, -9, 20, 5, 18), PartPose.offset(0, -17, 0));
		head.addOrReplaceChild("brow", box("brow", -8, -10, -8.5f, 16, 3, 3), PartPose.ZERO);
		// tusks grow out of the lower jaw, point forward and a little up (pitch -0.35), clear of the eyes
		head.addOrReplaceChild("fang_right", box("fang", -1, -1, -6, 2, 2, 6), PartPose.offsetAndRotation(-6.5f, -1.5f, -7f, -0.4f, 0, 0));
		head.addOrReplaceChild("fang_left", box("fang", -1, -1, -6, 2, 2, 6), PartPose.offsetAndRotation(6.5f, -1.5f, -7f, -0.4f, 0, 0));
		head.addOrReplaceChild("hair_top", box("hair_top", -8.5f, -12, -7.5f, 17, 2, 14), PartPose.ZERO);
		head.addOrReplaceChild("spike_1", box("spike", -1.5f, -8, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(-5, -12, -3, -0.2f, 0, -0.45f));
		head.addOrReplaceChild("spike_2", box("spike", -1.5f, -9, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(0, -12, -5, -0.7f, 0, 0));
		head.addOrReplaceChild("spike_3", box("spike", -1.5f, -8, -1.5f, 3, 8, 3), PartPose.offsetAndRotation(5, -12, -3, -0.2f, 0, 0.45f));
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			// a horn is three cubes stepping outwards, then up, then curling back in
			PartDefinition h1 = head.addOrReplaceChild("horn_" + n, box("horn1", -3.325f, -8, -3.325f, 6.65f, 8, 6.65f),
				PartPose.offsetAndRotation(side * 7.5f, -6, 1.5f, 0, 0, side * 1.35f).scaled(0.88f));
			PartDefinition h2 = h1.addOrReplaceChild("horn2", box("horn2", -2.85f, -9, -2.85f, 5.7f, 9, 5.7f),
				PartPose.offsetAndRotation(0, -7.5f, 0, 0, 0, side * -0.5f));
			PartDefinition h3 = h2.addOrReplaceChild("horn3", box("horn3", -2.375f, -9, -2.375f, 4.75f, 9, 4.75f),
				PartPose.offsetAndRotation(0, -8.5f, 0, 0, 0, side * -0.3f));
			h3.addOrReplaceChild("horn4", box("horn4", -1.425f, -8, -1.425f, 2.85f, 8, 2.85f),
				PartPose.offsetAndRotation(0, -8.5f, 0, 0, 0, side * -0.3f));
		}

		// ---- arms: raised forward-out (the shoulder yaws them forward), forearm folded up, a straight katana in each fist ----
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			PartDefinition shoulder = chest.addOrReplaceChild(n + "_shoulder", CubeListBuilder.create(),
				PartPose.offsetAndRotation(side * 17, -14, 0, 0, side * ARM_FORWARD, 0));
			PartDefinition arm = shoulder.addOrReplaceChild(n + "_arm", box("arm", -4.5f, -2, -4.5f, 9, 19, 9),
				PartPose.offsetAndRotation(0, 0, 0, 0, 0, side * -ARM_OUT));
			PartDefinition fore = arm.addOrReplaceChild("forearm", box("forearm", -4, 0, -4, 8, 14, 8),
				PartPose.offsetAndRotation(0, 16, 0, 0, 0, side * -FOREARM_UP));
			PartDefinition fist = fore.addOrReplaceChild("fist", box("fist", -3.5f, -1, -3.5f, 7, 6, 7), PartPose.offset(0, 13, 0));
			PartDefinition katana = fist.addOrReplaceChild("katana", box("handle", -1.5f, -5, -1.5f, 3, 12, 3),
				// turned about its own axis so that the cutting edge faces forward and outward, the spine back and inward (D105)
				PartPose.offsetAndRotation(0, 2, 0, 0, side < 0 ? Mth.PI : 0, 0));
			katana.addOrReplaceChild("guard", box("guard", -4, 7, -2.5f, 8, 2, 5), PartPose.ZERO);
			// one straight blade, no taper; the tip (far end) is cut on a slant in the texture (edge side shorter)
			katana.addOrReplaceChild("blade", box("blade", -2, 9, -1, 4, 28, 2), PartPose.ZERO);
		}

		// ---- wings (D105, the approved mockup): shown only while flying; per side a gold inner bone going out and up 29 degrees,
		// a gold elbow, a gold outer bone going out and down 7 degrees, and 11 separate feathers hanging behind the bones ----
		for (int side = -1; side <= 1; side += 2) {
			String n = side < 0 ? "right" : "left";
			PartDefinition wing = chest.addOrReplaceChild(n + "_wing", CubeListBuilder.create(),
				PartPose.offset(side * WING_ROOT_X, WING_ROOT_Y, WING_ROOT_Z));
			// bone directions in model axes (y down, the back is +z); the mockup has y up and the back at -z
			float[] d1 = unit(side * Mth.cos(WING_UP), -Mth.sin(WING_UP), 0.22f);
			float[] d2 = unit(side * Mth.cos(WING_DOWN), Mth.sin(WING_DOWN), 0.12f);
			float[] elbow = {d1[0] * INNER_LEN, d1[1] * INNER_LEN, d1[2] * INNER_LEN};
			float[] tip = {elbow[0] + d2[0] * OUTER_LEN, elbow[1] + d2[1] * OUTER_LEN, elbow[2] + d2[2] * OUTER_LEN};
			bone(wing, "bone_inner", "bone_inner", 4.5f, INNER_LEN, new float[] {0, 0, 0}, d1);
			bone(wing, "bone_outer", "bone_outer", 4.5f, OUTER_LEN, elbow, d2);
			wing.addOrReplaceChild("elbow", box("joint_elbow", -3, -3, -3, 6, 6, 6), PartPose.offset(elbow[0], elbow[1], elbow[2]));
			wing.addOrReplaceChild("wing_tip", box("joint_tip", -2.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.offset(tip[0], tip[1], tip[2]));
			for (int k = 0; k < FEATHER_LEN.length; k++) {
				float[] p;
				if (k < 6) {
					float t = 0.1f + 0.8f * k / 5f;
					p = new float[] {d1[0] * INNER_LEN * t, d1[1] * INNER_LEN * t, d1[2] * INNER_LEN * t};
				} else {
					float t = 0.06f + 0.92f * (k - 6) / 4f;
					p = new float[] {elbow[0] + d2[0] * OUTER_LEN * t, elbow[1] + d2[1] * OUTER_LEN * t, elbow[2] + d2[2] * OUTER_LEN * t};
				}
				float angle = (4f + 6.9f * k) * Mth.DEG_TO_RAD;
				float len = FEATHER_LEN[k];
				// the root sits 2 px below the bone centre line and a little behind it; the feather hangs in the wing plane
				PartDefinition f = wing.addOrReplaceChild("feather_" + k, box(k % 2 == 0 ? "feather_dark" : "feather_red", -2, 0, -0.6f, 4, 40, 1.2f),
					PartPose.offsetAndRotation(p[0], p[1] + 2, p[2] + 1.8f, 0, 0, -side * angle).scaled(1, len / 40f, 1));
				// the black tip is the last 3 px; the feather part is scaled along y, so the tip box is stretched back to 3 px
				float inv = 40f / len;
				f.addOrReplaceChild("tip", box("feather_tip", -2.2f, 0, -0.75f, 4.4f, 3 * inv, 1.5f), PartPose.offset(0, 40 - 3 * inv, 0));
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
		head.yRot = Mth.clamp(state.yRot, -50f, 50f) * Mth.DEG_TO_RAD * HEAD_YAW_FACTOR;
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
		hairBack.xRot = HAIR_TILT;
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
