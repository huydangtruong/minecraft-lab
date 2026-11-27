package io.github.huydangtruong.immortalcultivation.client;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import io.github.huydangtruong.immortalcultivation.ImmortalCultivation;
import io.github.huydangtruong.immortalcultivation.client.mixin.PostChainAccess;
import io.github.huydangtruong.immortalcultivation.client.mixin.PostPassAccess;
import java.nio.ByteBuffer;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/**
 * D268: the grey of a Time Rewind stop. A post effect (post_effect/chrono.json) greys every pixel whose view ray passes
 * through a stopped box before it hits something; this class feeds it the camera and the boxes each frame.
 */
public final class ChronoGrey {
	public static final Identifier ID = Identifier.fromNamespaceAndPath(ImmortalCultivation.NAMESPACE, "chrono");
	private static final String BLOCK = "ChronoConfig";
	/** mat4 + Params + 8 vec4. */
	private static final int SIZE = 64 + 16 + 8 * 16;
	private static final int MAX_BOXES = 4;

	private static PostPass fedPass;
	private static GpuBuffer buffer;
	private static ByteBuffer scratch;

	private ChronoGrey() {}

	/** True while a stop is reported near the player. */
	public static boolean wanted() {
		return ClientChrono.zones().iterator().hasNext();
	}

	public static void feed(PostChain chain, Camera camera) {
		var passes = ((PostChainAccess) chain).permahearts$passes();
		if (passes.isEmpty()) {
			return;
		}
		PostPass pass = passes.get(0);
		Map<String, GpuBuffer> uniforms = ((PostPassAccess) pass).permahearts$customUniforms();
		if (pass != fedPass || buffer == null || uniforms.get(BLOCK) != buffer) {
			// the JSON buffer cannot be written to: swap in one that can (once per loaded effect)
			GpuBuffer old = uniforms.get(BLOCK);
			buffer = RenderSystem.getDevice().createBuffer(() -> "permahearts chrono", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, SIZE);
			uniforms.put(BLOCK, buffer);
			if (old != null) {
				old.close();
			}
			fedPass = pass;
		}
		if (scratch == null) {
			scratch = MemoryUtil.memAlloc(SIZE);
		}
		Matrix4f vp = camera.getViewRotationProjectionMatrix(new Matrix4f());
		Matrix4f inv = new Matrix4f(vp).invert();
		// which depth the empty sky has and the NDC z of the near plane (26.3: reversed depth, NDC z -1..1 stored as 0..1)
		float near = ndcZ(vp, 0.06f);
		float far = ndcZ(vp, 1.0e6f);
		float sky = near > far ? 0f : 1f;
		Vec3 cam = camera.position();
		scratch.clear();
		Std140Builder b = Std140Builder.intoBuffer(scratch);
		b.putMat4f(inv);
		int n = 0;
		AABB[] boxes = new AABB[MAX_BOXES];
		for (ClientChrono.Zone z : ClientChrono.zones()) {
			if (n < MAX_BOXES) {
				boxes[n++] = z.box();
			}
		}
		b.putVec4(n, sky, near, 0f);
		for (int i = 0; i < MAX_BOXES; i++) {
			AABB a = boxes[i];
			if (a == null) {
				b.putVec4(0, 0, 0, 0);
				b.putVec4(0, 0, 0, 0);
			} else {
				b.putVec4((float) (a.minX - cam.x), (float) (a.minY - cam.y), (float) (a.minZ - cam.z), 0f);
				b.putVec4((float) (a.maxX - cam.x), (float) (a.maxY - cam.y), (float) (a.maxZ - cam.z), 0f);
			}
		}
		ByteBuffer data = b.get();
		RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data);
	}

	/** Depth (NDC z) of a point straight ahead of the camera at a distance. */
	private static float ndcZ(Matrix4f vp, float distance) {
		Matrix4f inv = new Matrix4f(vp).invert();
		Vector4f mid = inv.transform(new Vector4f(0f, 0f, 0.5f, 1f)); // a point on the centre ray, in front of the camera
		Vec3 dir = new Vec3(mid.x / mid.w, mid.y / mid.w, mid.z / mid.w).normalize();
		Vector4f p = vp.transform(new Vector4f((float) (dir.x * distance), (float) (dir.y * distance), (float) (dir.z * distance), 1f));
		return p.z / p.w;
	}
}
