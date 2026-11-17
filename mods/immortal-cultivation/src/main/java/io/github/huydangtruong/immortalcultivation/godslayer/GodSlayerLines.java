package io.github.huydangtruong.immortalcultivation.godslayer;

import java.util.List;
import java.util.random.RandomGenerator;

/** What God Slayer says (D113): 30 random lines each for winning, losing and scorning weak players. Pure Java. */
public final class GodSlayerLines {
	private GodSlayerLines() {}

	public static final List<String> WIN = List.of(
		"Pathetic. I will be back.",
		"Your hearts were paper. My blade was not.",
		"Rest now. The world is mine again.",
		"Is this all your cultivation could buy?",
		"I expected a battle. I got a warm-up.",
		"Kneel next time. It hurts less.",
		"Your ancestors weep at this display.",
		"Another hunt, another silence.",
		"Train a thousand days more. Then call me.",
		"The weak fall. That is the only law.",
		"I barely drew my second sword.",
		"Your blood will feed the soil. At least it will be useful.",
		"You fought like children with sticks.",
		"Remember this fear. It will keep you humble.",
		"Even the wind fought harder than you.",
		"I came for gods. I found only mortals.",
		"Your elixirs did nothing. Your courage did less.",
		"Count your hearts again. Then count your mistakes.",
		"The God Slayer does not lose to prey.",
		"Run home and tell them what you saw.",
		"I will remember none of your names.",
		"You were brave. Bravery is not enough.",
		"Blades sing when they cut the arrogant.",
		"This land bows to me once more.",
		"Grow stronger. I want a worthy kill next time.",
		"Your guardians fled before you did.",
		"Ten minutes was too generous.",
		"Sleep, little cultivators. Dream of me.",
		"The hunt is over. You were never the hunters.",
		"Come back with more hearts. And better friends."
	);

	public static final List<String> LOSE = List.of(
		"Impossible... I will return stronger.",
		"You have earned this victory. Enjoy it while it lasts.",
		"My blades... broken by mortals?",
		"This is not the end. I am never the end.",
		"Remember this day. I certainly will.",
		"You fight well. Next time, I fight better.",
		"I fall today so I may rise twice as strong.",
		"Well struck. Your hearts are worthy after all.",
		"Celebrate now. Fear me later.",
		"The Oni do not die. We wait.",
		"You have woken something you cannot stop.",
		"Hmph. Lucky. Very, very lucky.",
		"I underestimated you. That will not happen again.",
		"Take your treasure. It will not save you next time.",
		"My blood stains your blade... a fine trophy.",
		"Laugh while you can. I am already sharpening my swords.",
		"So this is the strength of your cultivation.",
		"Even gods bleed. Even I.",
		"You have bought yourselves a little peace. Nothing more.",
		"I will haunt your nights until we meet again.",
		"A worthy fight. A worthy defeat. A worthy return.",
		"Your names are carved in my memory now.",
		"Next time, I bring my full fury.",
		"I yield... for now.",
		"The hunter becomes the hunted. How amusing.",
		"Each defeat makes my blade hungrier.",
		"You win the battle. The war is far from over.",
		"Well done. Truly. Now prepare yourselves.",
		"I will return when you least expect it.",
		"The God Slayer bows... only this once."
	);

	public static final List<String> SCORN = List.of(
		"Weaklings. Come back when your hearts can bear my blade.",
		"Not one of you is worth unsheathing a sword for.",
		"I smell no warriors here. Only farmers.",
		"Thirty hearts. Is that too much to ask?",
		"I will not waste my blades on children.",
		"Grow stronger. Then I will grow interested.",
		"Your hearts are too few to fill my cup.",
		"Call me when one of you is worth hunting.",
		"I came all this way for nothing. Disappointing.",
		"Even my shadow would bore of you.",
		"Feast, train, cultivate. Then pray I come.",
		"The God Slayer does not hunt rabbits.",
		"Not today, little ones. You are beneath me.",
		"I will wait. Time is on my side, not yours.",
		"Your strength is a candle. I am the storm.",
		"Cultivate harder. My patience is long, but not endless.",
		"I see no gods here to slay. Only mortals.",
		"How sad. I sharpened both swords for this.",
		"Return to your fields. I will return to my mountain.",
		"Your hearts beat too softly for my ears.",
		"Find thirty hearts first. Then find courage.",
		"Is this the best this world can offer?",
		"I refuse to fight prey that cannot run.",
		"Drink your elixirs. All of them. Then call me.",
		"The weak are not worth the walk.",
		"I will come back when the hunt is fair.",
		"Even the Crimson Master is ashamed of you.",
		"Your blades are dull. Your hearts are duller.",
		"Train. Bleed. Grow. Then face me.",
		"Pitiful. Not even a warm-up."
	);

	public static String win(RandomGenerator random) {
		return pick(WIN, random);
	}

	public static String lose(RandomGenerator random) {
		return pick(LOSE, random);
	}

	public static String scorn(RandomGenerator random) {
		return pick(SCORN, random);
	}

	private static String pick(List<String> lines, RandomGenerator random) {
		return lines.get(random.nextInt(lines.size()));
	}
}
