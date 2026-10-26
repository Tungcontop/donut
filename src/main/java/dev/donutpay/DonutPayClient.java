package dev.donutpay;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;

public class DonutPayClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// 1) Watch chat for "<name> paid you $<amount>"
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) return; // action-bar copy of the same payment, ignore to avoid double counting
			PaymentTracker.Payment p = PaymentTracker.parseChat(message.getString());
			if (p != null) PaymentTracker.add(p.name, p.amount);
		});

		// 2) Passive HUD (no screen open, only once there's something to show)
		HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.options.hudHidden || mc.currentScreen != null) return;
			if (PaymentTracker.entries().isEmpty()) return;
			HudRenderer.render(ctx, 0, 0, false);
		});

		// 3) Interactive while the chat box is open (T / Enter)
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof ChatScreen)) return;
			ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, delta) ->
				HudRenderer.render(ctx, mouseX, mouseY, true));
			ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, button) ->
				!HudRenderer.click(mx, my, button));
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mx, my, horiz, vert) ->
				!HudRenderer.scroll(mx, my, vert));
		});

		// 4) /donutpay clear   and   /donutpay test <amount>   (test adds a fake payment)
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommandManager.literal("donutpay")
				.then(ClientCommandManager.literal("clear").executes(c -> {
					PaymentTracker.clear();
					c.getSource().sendFeedback(Text.literal("Payment list cleared."));
					return 1;
				}))
				.then(ClientCommandManager.literal("test")
					.then(ClientCommandManager.argument("amount", StringArgumentType.word())
						.executes(c -> {
							long amount = PaymentTracker.parseAmount(StringArgumentType.getString(c, "amount"));
							if (amount <= 0) {
								c.getSource().sendError(Text.literal("Couldn't read that amount. Try 25.1M or 500k."));
								return 0;
							}
							PaymentTracker.add("TestPlayer", amount);
							return 1;
						})))));
	}
}
