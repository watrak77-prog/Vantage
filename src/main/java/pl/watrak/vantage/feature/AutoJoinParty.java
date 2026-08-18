package pl.watrak.vantage.feature;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import pl.watrak.vantage.VantageClient;
import pl.watrak.vantage.config.ConfigManager;

/**
 * Joins public party invitations on PvPClub the moment they appear.
 *
 * <p>The invitation ends in a clickable "Join" button, and the command behind
 * that button is the thing this runs. Reading the command out of the message
 * rather than guessing at it means nothing here depends on the wording, the
 * player's name, or the party syntax — if the server changes any of those, the
 * button still carries whatever the correct command now is.
 */
public final class AutoJoinParty {

	/** The one phrase the invitation is recognised by. */
	private static final String INVITATION = "is hosting a public party!";

	/**
	 * The same message can arrive through both the chat and system events. This
	 * window drops the second copy so the command is not sent twice.
	 */
	private static final long DUPLICATE_WINDOW_MS = 500L;

	/** Floor on how often a join may fire, in case a server spams invitations. */
	private static final long COOLDOWN_MS = 3000L;

	private static String lastMessage = "";
	private static long lastMessageAt;
	private static long lastJoinAt;

	private AutoJoinParty() {
	}

	public static void register() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				handle(message);
			}
		});
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, timestamp) -> handle(message));
	}

	private static void handle(Component message) {
		if (!ConfigManager.get().autoJoinParty) {
			return;
		}

		String text = message.getString();
		if (!text.contains(INVITATION)) {
			return;
		}

		long now = System.currentTimeMillis();
		if (text.equals(lastMessage) && now - lastMessageAt < DUPLICATE_WINDOW_MS) {
			return;
		}
		lastMessage = text;
		lastMessageAt = now;

		if (now - lastJoinAt < COOLDOWN_MS) {
			return;
		}

		String command = commandOf(message);
		if (command == null) {
			return;
		}

		lastJoinAt = now;
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> {
			ClientPacketListener connection = minecraft.getConnection();
			if (connection != null) {
				VantageClient.LOGGER.info("Auto-joining party: /{}", command);
				connection.sendCommand(command);
			}
		});
	}

	/**
	 * Finds the command attached to the message's clickable part.
	 *
	 * <p>Walks the whole component tree, because the button is a sibling deep
	 * inside a formatted line rather than the message itself.
	 */
	private static @Nullable String commandOf(Component message) {
		ClickEvent event = message.getStyle().getClickEvent();

		String command = switch (event) {
			case ClickEvent.RunCommand run -> run.command();
			case ClickEvent.SuggestCommand suggest -> suggest.command();
			case null, default -> null;
		};

		if (command != null) {
			// sendCommand adds the slash itself; leaving ours on would send "//party".
			return command.startsWith("/") ? command.substring(1) : command;
		}

		for (Component sibling : message.getSiblings()) {
			String found = commandOf(sibling);
			if (found != null) {
				return found;
			}
		}
		return null;
	}
}
