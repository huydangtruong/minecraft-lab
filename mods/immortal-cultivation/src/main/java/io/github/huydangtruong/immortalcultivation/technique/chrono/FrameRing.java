package io.github.huydangtruong.immortalcultivation.technique.chrono;

import java.util.Arrays;

/**
 * D268: the recorded values of one thing (a mob, a projectile, the caster) for the last {@code capacity} frames. A frame is
 * a number that goes up by one every recorded tick; a frame the thing was not recorded in has no values. Pure Java.
 */
public final class FrameRing {
	private final int capacity;
	private final int width;
	private final float[] data;
	private final int[] frameAt;
	private int last = -1;

	public FrameRing(int capacity, int width) {
		this.capacity = Math.max(1, capacity);
		this.width = width;
		this.data = new float[this.capacity * width];
		this.frameAt = new int[this.capacity];
		Arrays.fill(frameAt, -1);
	}

	public void put(int frame, float[] values) {
		int i = Math.floorMod(frame, capacity);
		frameAt[i] = frame;
		System.arraycopy(values, 0, data, i * width, width);
		last = Math.max(last, frame);
	}

	/** Copies the values of a frame into {@code out}; false when that frame was not recorded (or is too old). */
	public boolean get(int frame, float[] out) {
		if (frame < 0 || frame > last || frame <= last - capacity) {
			return false;
		}
		int i = Math.floorMod(frame, capacity);
		if (frameAt[i] != frame) {
			return false;
		}
		System.arraycopy(data, i * width, out, 0, width);
		return true;
	}

	/** The oldest recorded frame still kept, or -1 when nothing is kept. */
	public int first() {
		for (int f = Math.max(0, last - capacity + 1); f <= last; f++) {
			if (frameAt[Math.floorMod(f, capacity)] == f) {
				return f;
			}
		}
		return -1;
	}

	/** The newest recorded frame, or -1. */
	public int last() {
		return last;
	}

	/** Forgets every frame after {@code frame} (time runs again from there, D268). */
	public void truncate(int frame) {
		last = -1;
		for (int i = 0; i < capacity; i++) {
			if (frameAt[i] > frame) {
				frameAt[i] = -1;
			}
			last = Math.max(last, frameAt[i]);
		}
	}
}
