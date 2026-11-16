package io.github.huydangtruong.permahearts;

import io.github.huydangtruong.permahearts.notes.NoteRules;
import io.github.huydangtruong.permahearts.notes.NotesData;
import io.github.huydangtruong.permahearts.notes.NotesNet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** P-042 (D117): the server side of the Notes panel: add, limits, delete own only. */
public class NotesGameTest {
	@GameTest
	public void addLimitAndDeleteOwnOnly(GameTestHelper c) {
		ServerPlayer a = c.makeMockServerPlayerInLevel();
		ServerPlayer b = c.makeMockServerPlayerInLevel();
		NotesData data = NotesData.of(c.getLevel().getServer());
		int sharedBefore = data.shared().size();
		for (int i = 0; i < NoteRules.MAX_SHARED_PER_PLAYER; i++) {
			if (NotesNet.apply(a, NotesNet.ADD, true, "note " + i, 0) != null) {
				c.fail("add " + i + " refused");
				return;
			}
		}
		if (NotesNet.apply(a, NotesNet.ADD, true, "one more", 0) == null) {
			c.fail("the 17th shared note must be refused");
			return;
		}
		if (NotesNet.apply(b, NotesNet.ADD, true, "from b", 0) != null) {
			c.fail("the cap is per player");
			return;
		}
		if (data.shared().size() != sharedBefore + 17 || !data.shared().get(0).text().equals("from b")) {
			c.fail("newest must be on top, size " + data.shared().size());
			return;
		}
		long bId = data.shared().get(0).id();
		NotesNet.apply(a, NotesNet.DELETE, true, "", bId); // a cannot delete b's note
		if (data.shared().size() != sharedBefore + 17) {
			c.fail("deleted someone else's note");
			return;
		}
		NotesNet.apply(b, NotesNet.DELETE, true, "", bId);
		if (data.shared().size() != sharedBefore + 16) {
			c.fail("own delete failed");
			return;
		}
		NotesNet.apply(a, NotesNet.ADD, false, "secret", 0);
		if (data.privateOf(a.getUUID()).size() != 1 || !data.privateOf(b.getUUID()).isEmpty()) {
			c.fail("private notes leak or are missing");
			return;
		}
		c.succeed();
	}
}
