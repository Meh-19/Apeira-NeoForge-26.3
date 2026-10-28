package com.apeira.main.client.realm;

import java.util.Locale;
import java.util.Optional;


import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.realm.RealmHudPayload;
import com.apeira.main.realm.RealmVisitors;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Origin Realms' top bar, shown while the player is in a realm: pills along the top of the screen, as the server draws
 * them in its first boss bar's name (Ren's HUD capture). The left pill is the waypoint icon and where the player is
 * ("Air Balloon Platform" while they stand on the 5x5 platform round the starter balloon, only while the realm has
 * one, else the realm's name followed, in the same pill, by the player's role there as Origin Realms' role glyph:
 * the server's {@code default.json} and {@code boss_bar_1} glyphs ꑓ visitor, ꑘ resident, ꑕ trusted and ꑖ owner,
 * {@link #ROLE_GLYPHS}, drawn from the server's {@code custom_ui/role_<role>.png} labels, Apeira's
 * {@code textures/gui/role/role_<role>.png}, at ascent 3 and height 7 as {@code boss_bar_1} has them; the small
 * {@code <role>_playerlist.png} badges are the tab list's only), then the clock
 * icon and the time of day ("12:53 PM").
 *
 * <p>It is the server's own text, rebuilt glyph for glyph in the {@code apeira:top_bar} font (Origin Realms'
 * {@code boss_bar_1}: the ASCII sheet at ascent 3, {@code textures/gui/top_bar/} background pieces at ascent 6 and icons
 * at ascent 4, and its {@code minecraft:default} negative spaces), centred and drawn at y 3 with the text shadow, where
 * vanilla writes the first boss bar's name. A pill's background is an end column, centre pieces of 64, 32, ... 1
 * pixels adding up to the content's advance plus 6, and an end column; a backwards space of that width less one puts
 * the icon 4 pixels inside it; pills are 2 pixels apart.
 */
public final class RealmTopBar {
    private static final Style FONT = Style.EMPTY.withFont(new FontDescription.Resource(ApeiraIds.id("top_bar")));
    private static final int Y = 3;
    private static final int COLOR = 0xFFFFFFFF;
    private static final char END = 'ꈁ';
    /** The centre pieces A202 to A208: 1, 2, 4 ... 64 pixels wide. */
    private static final char CENTER_1 = 'ꈂ';
    private static final int[] BACK = {64, 32, 16, 8, 7, 5, 1};
    private static final char[] BACK_GLYPHS = {'', '', '', '', '', '', ''};
    private static final char CLOCK = '뀁';
    private static final char WAYPOINT = '뀂';
    /** The platform is 5x5 round the balloon: two blocks to each side. */
    private static final int PLATFORM_RADIUS = 2;
    /** Feet from a block under the platform's floor to the top of the balloon's basket count as on it. */
    private static final int PLATFORM_BELOW = 1;
    private static final int PLATFORM_ABOVE = 3;
    /** Origin Realms' role glyphs in {@link com.apeira.main.realm.RealmRole}'s order: visitor, resident, trusted, owner. */
    private static final char[] ROLE_GLYPHS = {'\uA453', '\uA458', '\uA455', '\uA456'};

    private static Optional<String> owner = Optional.empty();
    private static Optional<BlockPos> balloon = Optional.empty();
    private static int role = -1;

    private RealmTopBar() {}

    /** The client player's role in the realm they are in ({@link com.apeira.main.realm.RealmRole}'s ordinal), -1 outside realms. */
    public static int role() {
        return role;
    }

    public static void onSync(RealmHudPayload payload, IPayloadContext context) {
        owner = payload.owner();
        balloon = payload.balloon();
        role = payload.role();
        RealmVisitors.setClientVisitor(payload.visitor());
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        owner = Optional.empty();
        balloon = Optional.empty();
        role = -1;
        RealmVisitors.setClientVisitor(false);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (owner.isEmpty() || player == null || minecraft.level == null || minecraft.gui.hud.isHidden()) {
            return;
        }
        Font font = minecraft.font;
        boolean platform = onPlatform(player);
        String place = platform ? Component.translatable("hud.apeira.top_bar.balloon_platform").getString()
                : Component.translatable("hud.apeira.top_bar.realm", owner.get()).getString();
        MutableComponent content = Component.literal(WAYPOINT + " " + place).withStyle(FONT);
        if (!platform && role >= 0 && role < ROLE_GLYPHS.length) {
            // Inline after the realm's name.
            content.append(Component.literal(" " + ROLE_GLYPHS[role]).withStyle(FONT));
        }
        MutableComponent bar = Component.empty();
        pill(font, bar, content, false);
        pill(font, bar, Component.literal(CLOCK + " " + time(minecraft.level.getOverworldClockTime())).withStyle(FONT), true);
        graphics.text(font, bar, graphics.guiWidth() / 2 - font.width(bar) / 2, Y, COLOR, true);
    }

    private static boolean onPlatform(Player player) {
        BlockPos at = player.blockPosition();
        return balloon.filter(pos -> Math.abs(at.getX() - pos.getX()) <= PLATFORM_RADIUS && Math.abs(at.getZ() - pos.getZ()) <= PLATFORM_RADIUS
                && at.getY() >= pos.getY() - PLATFORM_BELOW && at.getY() <= pos.getY() + PLATFORM_ABOVE).isPresent();
    }

    /** Appends one pill: its background, then {@code content} (its icon and text, in the top bar font) drawn back over it. */
    private static void pill(Font font, MutableComponent bar, Component content, boolean last) {
        int width = font.width(content) + 6;
        StringBuilder glyphs = new StringBuilder().append(END).append('');
        int left = width;
        for (int bit = 6; bit >= 0; bit--) {
            int piece = 1 << bit;
            while (left >= piece) {
                glyphs.append((char) (CENTER_1 + bit)).append('');
                left -= piece;
            }
        }
        glyphs.append(END);
        // Back over the background to 4 pixels inside its left edge.
        int back = width - 1;
        for (int i = 0; i < BACK.length; i++) {
            while (back >= BACK[i]) {
                glyphs.append(BACK_GLYPHS[i]);
                back -= BACK[i];
            }
        }
        bar.append(Component.literal(glyphs.toString()).withStyle(FONT)).append(content)
                .append(Component.literal(last ? "" : "").withStyle(FONT));
    }

    /** The time of day as Origin Realms writes it, "12:53 PM": day time 0 is 6 AM. */
    static String time(long dayTime) {
        long ticks = Math.floorMod(dayTime, 24000L);
        int hours = (int) ((ticks / 1000 + 6) % 24);
        int minutes = (int) (ticks % 1000 * 60 / 1000);
        int hour = hours % 12 == 0 ? 12 : hours % 12;
        return String.format(Locale.ROOT, "%d:%02d %s", hour, minutes, hours < 12 ? "AM" : "PM");
    }
}
