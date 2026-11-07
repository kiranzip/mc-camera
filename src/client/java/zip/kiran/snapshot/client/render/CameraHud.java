package zip.kiran.snapshot.client.render;

import zip.kiran.snapshot.CameraConstants;
import zip.kiran.snapshot.client.CameraClient;
import zip.kiran.snapshot.client.ViewfinderState;
import zip.kiran.snapshot.client.photo.ClientPhotoCache;
import zip.kiran.snapshot.component.Components;
import zip.kiran.snapshot.component.CameraSettings;
import zip.kiran.snapshot.component.PhotoRef;
import zip.kiran.snapshot.item.Items;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CameraHud {
    private CameraHud() {
    }

    public static void configure(int mask, int bracket, int readout, int readoutSelected, int readoutDim, int card) {
        MASK = mask;
        BRACKET = bracket;
        READOUT = readout;
        READOUT_SELECTED = readoutSelected;
        READOUT_DIM = readoutDim;
        CARD = card;
    }

    private static int MASK = 0xC8060606;
    private static int BRACKET = 0xFFEFEBE2;
    private static int READOUT = 0xFFE8E4DA;
    private static int READOUT_SELECTED = 0xFFFFC46B;
    private static int READOUT_DIM = 0xFF8A857B;
    private static int CARD = 0xFFF4F2EB;

    private static final int HOTBAR_HEIGHT = 22;
    private static final int INFO_ROW_OFFSET = 30;
    private static final int DIAL_ROW_OFFSET = 14;
    private static final int TEXT_GAP = 8;
    private static final int TEXT_INSET = 14;
    private static final int CORNER_DIVISOR = 11;
    private static final int HINT_TOP_OFFSET = 12;

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.options.hudHidden) {
            return;
        }

        if (CameraClient.state().isActive()) {
            drawViewfinder(context, client);
        } else if (player.isUsingItem() && player.getActiveItem().isOf(Items.PHOTOGRAPH)) {
            drawHeldPrint(context, client, player.getActiveItem());
        }
    }

    private static void drawViewfinder(DrawContext context, MinecraftClient client) {
        ViewfinderState state = CameraClient.state();
        CameraSettings settings = state.settings();
        TextRenderer text = client.textRenderer;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        int side = Math.min(width, height);
        int left = (width - side) / 2;
        int top = (height - side) / 2;
        int right = left + side;
        int bottom = top + side;

        context.fill(0, 0, width, top, MASK);
        context.fill(0, bottom, width, height, MASK);
        context.fill(0, top, left, bottom, MASK);
        context.fill(right, top, width, bottom, MASK);

        drawCorners(context, left, top, right, bottom, side / CORNER_DIVISOR);
        drawReticle(context, (left + right) / 2, (top + bottom) / 2);

        int centreX = width / 2;
        int hotbarTop = height - HOTBAR_HEIGHT;
        int rowY = hotbarTop - INFO_ROW_OFFSET;
        int barY = hotbarTop - DIAL_ROW_OFFSET;

        String aperture = state.apertureString();
        String lens = state.focalString();
        String focus = state.focusString();
        String iso = state.isoString();

        context.drawTextWithShadow(text, aperture, left + TEXT_INSET, rowY,
                tint(state.dial() == ViewfinderState.Dial.APERTURE));
        context.drawTextWithShadow(text, lens, left + TEXT_INSET + text.getWidth(aperture) + TEXT_GAP, rowY,
                tint(state.dial() == ViewfinderState.Dial.FOCAL_LENGTH));

        int isoX = right - TEXT_INSET - text.getWidth(iso);
        context.drawTextWithShadow(text, iso, isoX, rowY,
                tint(state.dial() == ViewfinderState.Dial.ISO));
        context.drawTextWithShadow(text, focus, isoX - TEXT_GAP - text.getWidth(focus), rowY,
                tint(state.dial() == ViewfinderState.Dial.FOCUS));

        String name = Text.translatable(state.dial().translationKey).getString();
        context.drawTextWithShadow(text, name, centreX - text.getWidth(name) / 2, rowY, READOUT_SELECTED);

        drawDial(context, centreX, barY, state);

        Text hint = Text.translatable("hud.kiranzip.viewfinder.hint");
        context.drawTextWithShadow(text, hint, centreX - text.getWidth(hint) / 2, top + HINT_TOP_OFFSET, READOUT_DIM);
    }

    private static int tint(boolean selected) {
        return selected ? READOUT_SELECTED : READOUT;
    }

    private static void drawCorners(DrawContext context, int left, int top, int right, int bottom, int length) {
        int t = 2;
        context.fill(left, top, left + length, top + t, BRACKET);
        context.fill(left, top, left + t, top + length, BRACKET);
        context.fill(right - length, top, right, top + t, BRACKET);
        context.fill(right - t, top, right, top + length, BRACKET);
        context.fill(left, bottom - t, left + length, bottom, BRACKET);
        context.fill(left, bottom - length, left + t, bottom, BRACKET);
        context.fill(right - length, bottom - t, right, bottom, BRACKET);
        context.fill(right - t, bottom - length, right, bottom, BRACKET);
    }

    private static void drawReticle(DrawContext context, int cx, int cy) {
        int reach = 9;
        int tick = 4;
        context.fill(cx - reach, cy - reach, cx - reach + tick, cy - reach + 1, BRACKET);
        context.fill(cx - reach, cy - reach, cx - reach + 1, cy - reach + tick, BRACKET);
        context.fill(cx + reach - tick, cy - reach, cx + reach, cy - reach + 1, BRACKET);
        context.fill(cx + reach - 1, cy - reach, cx + reach, cy - reach + tick, BRACKET);
        context.fill(cx - reach, cy + reach - 1, cx - reach + tick, cy + reach, BRACKET);
        context.fill(cx - reach, cy + reach - tick, cx - reach + 1, cy + reach, BRACKET);
        context.fill(cx + reach - tick, cy + reach - 1, cx + reach, cy + reach, BRACKET);
        context.fill(cx + reach - 1, cy + reach - tick, cx + reach, cy + reach, BRACKET);
    }

    private static void drawDial(DrawContext context, int centreX, int y, ViewfinderState state) {
        int half = 66;
        int barLeft = centreX - half;
        int barRight = centreX + half;

        context.fill(barLeft, y, barRight, y + 1, 0x66FFFFFF);

        int stops = state.dialStops();
        if (stops > 1) {
            for (int i = 0; i < stops; i++) {
                int x = barLeft + Math.round((float) i / (stops - 1) * (barRight - barLeft));
                context.fill(x, y - 2, x + 1, y + 3, 0x55FFFFFF);
            }
        }

        int knob = barLeft + Math.round(state.dialFraction() * (barRight - barLeft));
        context.fill(knob - 1, y - 4, knob + 2, y + 5, READOUT_SELECTED);
    }

    private static void drawHeldPrint(DrawContext context, MinecraftClient client, ItemStack stack) {
        PhotoRef ref = PhotoRef.fromNbt(Components.PHOTO.raw(stack));
        if (ref == null) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        context.fill(0, 0, width, height, 0xB8000000);

        int cardHeight = Math.max(96, (int) (height * 0.76F));
        int cardWidth = Math.round(cardHeight * 0.82F);
        int x = (width - cardWidth) / 2;
        int y = (height - cardHeight) / 2;

        context.fill(x + 5, y + 5, x + cardWidth + 5, y + cardHeight + 5, 0x55000000);
        context.fill(x, y, x + cardWidth, y + cardHeight, CARD);

        int margin = Math.max(4, Math.round(cardWidth * 0.075F));
        int imageSize = cardWidth - margin * 2;
        int imageX = x + margin;
        int imageY = y + margin;

        Identifier texture = ClientPhotoCache.texture(ref.photoId());
        if (texture != null) {
            context.drawTexture(texture, imageX, imageY, imageSize, imageSize,
                    0.0F, 0.0F, CameraConstants.PHOTO_SIZE, CameraConstants.PHOTO_SIZE,
                    CameraConstants.PHOTO_SIZE, CameraConstants.PHOTO_SIZE);
        } else {
            context.fill(imageX, imageY, imageX + imageSize, imageY + imageSize,
                    ClientPhotoCache.isMissing(ref.photoId()) ? 0xFF1B1B1B : 0xFF2A2A2A);
        }

        float developed = ref.developProgress(System.currentTimeMillis());
        if (developed < 1.0F) {
            int alpha = Math.round(255.0F * (1.0F - developed));
            context.fill(imageX, imageY, imageX + imageSize, imageY + imageSize, (alpha << 24) | 0x00EFEBE0);
        }

        TextRenderer text = client.textRenderer;
        String dials = ref.settings().formatAperture() + "  \u00b7  " + ref.settings().formatFocalLength()
                + "  \u00b7  " + ref.settings().formatIso();

        int lineHeight = 11;
        int bandTop = imageY + imageSize;
        int bandHeight = (y + cardHeight) - bandTop;
        int lineY = bandTop + Math.max(4, (bandHeight - lineHeight * 3) / 2);

        if (!ref.location().isEmpty()) {
            context.drawText(text, ref.location(), imageX, lineY, 0xFF463F38, false);
        }
        if (!ref.author().isEmpty()) {
            context.drawText(text, ref.author(), imageX + imageSize - text.getWidth(ref.author()), lineY,
                    0xFF463F38, false);
        }

        context.drawText(text, dials, imageX, lineY + lineHeight * 2, 0xFF8A8078, false);
    }
}
