package dev.donutpay;

import dev.donutpay.PaymentTracker.Payment;
import dev.donutpay.PaymentTracker.State;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Draws the panel and handles clicks on its buttons. */
public final class HudRenderer {

	// Panel position on screen (top-left). Change these to move it.
	public static int originX = 6;
	public static int originY = 6;

	private static final int WIDTH = 196;
	private static final int PAD = 5;
	private static final int LINE = 11;
	private static final int BTN_H = 16;
	private static final int GAP = 3;

	private static final int BG = 0xC0101018;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFAAAAAA;
	private static final int LIGHT = 0xFFCCCCCC;
	private static final int GREEN = 0xFF55FF55;
	private static final int RED = 0xFFFF5555;
	private static final int YELLOW = 0xFFFFFF55;
	private static final int GOLD = 0xFFFFAA00;
	private static final int AQUA = 0xFF55FFFF;

	private record Rect(int x, int y, int w, int h) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private static final Rect EMPTY = new Rect(0, 0, 0, 0);
	private static Rect panel = EMPTY;
	private static Rect winBtn = EMPTY;
	private static Rect loseBtn = EMPTY;
	private static Rect skipBtn = EMPTY;
	private static Rect undoBtn = EMPTY;

	private HudRenderer() {}

	public static void render(DrawContext ctx, int mouseX, int mouseY, boolean interactive) {
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		List<Payment> list = PaymentTracker.entries();
		int size = list.size();
		int start = PaymentTracker.windowStart();
		int end = PaymentTracker.windowEnd();
		Payment sel = PaymentTracker.selected();

		boolean hiddenAbove = start > 0;
		boolean hiddenBelow = end < size;
		int rows = Math.max(1, end - start);

		int height = PAD + LINE + LINE
			+ (hiddenAbove ? LINE : 0)
			+ rows * LINE
			+ (hiddenBelow ? LINE : 0)
			+ (sel != null ? LINE + 2 : 0)
			+ 4 + BTN_H + GAP + BTN_H + GAP + BTN_H + PAD;

		int x = originX;
		int y = originY;
		panel = new Rect(x, y, WIDTH, height);
		ctx.fill(x, y, x + WIDTH, y + height, BG);
		ctx.drawBorder(x, y, WIDTH, height, 0x40FFFFFF);

		int cy = y + PAD;

		// header
		ctx.drawTextWithShadow(tr, "»", x + PAD, cy, GOLD);
		ctx.drawTextWithShadow(tr, "Gambling HUD", x + PAD + 10, cy, WHITE);
		cy += LINE;
		ctx.drawTextWithShadow(tr, "(press T/Enter to interact)", x + PAD, cy, GRAY);
		cy += LINE;

		if (hiddenAbove) {
			ctx.drawTextWithShadow(tr, "▲ scroll up for more (" + start + ")", x + PAD, cy, GRAY);
			cy += LINE;
		}

		// rows
		if (size == 0) {
			ctx.drawTextWithShadow(tr, "waiting for payments...", x + PAD, cy, GRAY);
			cy += LINE;
		} else {
			for (int i = start; i < end; i++) {
				Payment p = list.get(i);
				boolean isSel = p == sel;
				if (isSel) {
					ctx.fill(x + 2, cy - 1, x + WIDTH - 2, cy + LINE - 1, 0x66FFAA00);
				}

				String icon;
				int iconColor;
				switch (p.state) {
					case WIN -> { icon = "✔"; iconColor = GREEN; }
					case LOSE -> { icon = "✘"; iconColor = RED; }
					case SKIP -> { icon = "–"; iconColor = GRAY; }
					default -> {
						icon = isSel ? "▶" : "•";
						iconColor = isSel ? YELLOW : GRAY;
					}
				}
				ctx.drawTextWithShadow(tr, icon, x + PAD, cy, iconColor);
				ctx.drawTextWithShadow(tr, p.name, x + PAD + 12, cy, AQUA);

				String amount = PaymentTracker.format(p.amount);
				int right = x + WIDTH - PAD;
				if (isSel) {
					String arrow = " → ";
					String payout = PaymentTracker.format(PaymentTracker.payout(p));
					int total = tr.getWidth(amount) + tr.getWidth(arrow) + tr.getWidth(payout);
					int px = right - total;
					ctx.drawTextWithShadow(tr, amount, px, cy, LIGHT);
					px += tr.getWidth(amount);
					ctx.drawTextWithShadow(tr, arrow, px, cy, WHITE);
					px += tr.getWidth(arrow);
					ctx.drawTextWithShadow(tr, payout, px, cy, GREEN);
				} else {
					ctx.drawTextWithShadow(tr, amount, right - tr.getWidth(amount), cy, LIGHT);
				}
				cy += LINE;
			}
		}

		if (hiddenBelow) {
			ctx.drawTextWithShadow(tr, "▼ scroll down (" + (size - end) + ")", x + PAD, cy, GRAY);
			cy += LINE;
		}

		// "x = 50.2M"
		if (sel != null) {
			cy += 2;
			int px = x + PAD;
			ctx.drawTextWithShadow(tr, "x", px, cy, GREEN);
			px += tr.getWidth("x");
			ctx.drawTextWithShadow(tr, " = ", px, cy, GRAY);
			px += tr.getWidth(" = ");
			ctx.drawTextWithShadow(tr, PaymentTracker.format(PaymentTracker.payout(sel)), px, cy, AQUA);
			cy += LINE;
		}

		cy += 4;
		int full = WIDTH - 2 * PAD;
		int half = (full - GAP) / 2;
		boolean hasPending = sel != null;

		winBtn = new Rect(x + PAD, cy, full, BTN_H);
		cy += BTN_H + GAP;
		loseBtn = new Rect(x + PAD, cy, half, BTN_H);
		skipBtn = new Rect(x + PAD + half + GAP, cy, full - half - GAP, BTN_H);
		cy += BTN_H + GAP;
		undoBtn = new Rect(x + PAD, cy, full, BTN_H);

		button(ctx, tr, winBtn, "✔ WIN", 0xFF1F6F1F, 0xFF2B8F2B, GREEN, hasPending, interactive, mouseX, mouseY);
		button(ctx, tr, loseBtn, "✘ LOSE", 0xFF6F1F1F, 0xFF8F2B2B, RED, hasPending, interactive, mouseX, mouseY);
		button(ctx, tr, skipBtn, "→ SKIP", 0xFF3A3A3A, 0xFF505050, LIGHT, hasPending, interactive, mouseX, mouseY);
		button(ctx, tr, undoBtn, "↶ Undo last", 0xFF4A4010, 0xFF645617, YELLOW, PaymentTracker.canUndo(), interactive, mouseX, mouseY);
	}

	private static void button(DrawContext ctx, TextRenderer tr, Rect r, String label, int bg, int hoverBg,
			int textColor, boolean enabled, boolean interactive, int mouseX, int mouseY) {
		boolean hover = interactive && enabled && r.contains(mouseX, mouseY);
		int fill = !enabled ? 0xFF262626 : hover ? hoverBg : bg;
		ctx.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), fill);
		ctx.drawBorder(r.x(), r.y(), r.w(), r.h(), 0x80000000);
		ctx.drawCenteredTextWithShadow(tr, label, r.x() + r.w() / 2, r.y() + (r.h() - 8) / 2,
			enabled ? textColor : 0xFF666666);
	}

	/** Returns true if the click landed on the panel (so the chat screen should ignore it). */
	public static boolean click(double mx, double my, int mouseButton) {
		if (!panel.contains(mx, my)) return false;
		if (mouseButton != 0) return true;

		boolean hasPending = PaymentTracker.selected() != null;
		if (hasPending && winBtn.contains(mx, my)) PaymentTracker.mark(State.WIN);
		else if (hasPending && loseBtn.contains(mx, my)) PaymentTracker.mark(State.LOSE);
		else if (hasPending && skipBtn.contains(mx, my)) PaymentTracker.mark(State.SKIP);
		else if (undoBtn.contains(mx, my)) PaymentTracker.undo();
		return true;
	}

	/** Returns true if the scroll happened over the panel. */
	public static boolean scroll(double mx, double my, double vertical) {
		if (!panel.contains(mx, my)) return false;
		PaymentTracker.scrollBy(vertical > 0 ? 1 : -1);
		return true;
	}
}
