package dev.rennivik.rennmute;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

public class Rennmute implements ClientModInitializer {
	private final Map<SoundCategory, Double> previousVolumes = new EnumMap<>(SoundCategory.class);
	@Override
	public void onInitializeClient() {
		KeyBinding muteKey = KeyBindingHelper.registerKeyBinding(
				new KeyBinding(
						"key.rennmute.toggle_mute",
						InputUtil.Type.KEYSYM,
						GLFW.GLFW_KEY_M,
						KeyBinding.Category.MISC
				));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			GameOptions options = client.options;
			SoundManager soundManager = client.getSoundManager();

			if (muteKey.wasPressed()) {
				double masterVolume = options.getSoundVolumeOption(SoundCategory.MASTER).getValue();
				boolean willMute = masterVolume != 0.0;

				if (willMute) {
					for (SoundCategory category : SoundCategory.values()) {
						previousVolumes.put(category, options.getSoundVolumeOption(category).getValue());
						options.getSoundVolumeOption(category).setValue(0.0);
						soundManager.refreshSoundVolumes(category);
					}
				} else {
					for (SoundCategory category : SoundCategory.values()) {
						double previous = previousVolumes.getOrDefault(category, 1.0);
						options.getSoundVolumeOption(category).setValue(previous);
						soundManager.refreshSoundVolumes(category);
					}
					previousVolumes.clear();
				}

				Text message = Text.literal(willMute ? "Muted the game!" : "Un-muted the game!")
						.formatted(willMute ? Formatting.LIGHT_PURPLE : Formatting.GRAY);
				client.player.sendMessage(message, false);
			}
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			if (client.player == null) return;

			GameOptions options = client.options;
			SoundManager soundManager = client.getSoundManager();

			if (!previousVolumes.isEmpty()) {
				for (SoundCategory category : SoundCategory.values()) {
					double previous = previousVolumes.getOrDefault(category, 1.0);
					options.getSoundVolumeOption(category).setValue(previous);
					soundManager.refreshSoundVolumes(category);
				}
				previousVolumes.clear();
			}
		});
	}
}