package io.github.huydangtruong.permahearts;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermaHearts implements ModInitializer {
	public static final String MOD_ID = "permahearts";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("PermaHearts loaded");
	}
}
