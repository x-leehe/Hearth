package org.awp0rtuh1ty.hearth;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Direct copy of Carpet's {@code Messenger} utility.
 * Format: {@code "<style> <text>"}, then {@code "!/cmd"}, {@code "?/cmd"}, {@code "^hover"}.
 */
public final class HearthMessenger {
    private static final Pattern COLOR_EXTRACT = Pattern.compile("#([0-9a-fA-F]{6})");

    enum Fmt {
        ITALIC      ('i', (s, f) -> s.withItalic(true)),
        STRIKE      ('s', (s, f) -> s.applyFormat(ChatFormatting.STRIKETHROUGH)),
        UNDERLINE   ('u', (s, f) -> s.applyFormat(ChatFormatting.UNDERLINE)),
        BOLD        ('b', (s, f) -> s.withBold(true)),
        OBFUSCATE   ('o', (s, f) -> s.applyFormat(ChatFormatting.OBFUSCATED)),

        WHITE       ('w', (s, f) -> s.withColor(ChatFormatting.WHITE)),
        YELLOW      ('y', (s, f) -> s.withColor(ChatFormatting.YELLOW)),
        LIGHT_PURPLE('m', (s, f) -> s.withColor(ChatFormatting.LIGHT_PURPLE)),
        RED         ('r', (s, f) -> s.withColor(ChatFormatting.RED)),
        AQUA        ('c', (s, f) -> s.withColor(ChatFormatting.AQUA)),
        GREEN       ('l', (s, f) -> s.withColor(ChatFormatting.GREEN)),
        BLUE        ('t', (s, f) -> s.withColor(ChatFormatting.BLUE)),
        DARK_GRAY   ('f', (s, f) -> s.withColor(ChatFormatting.DARK_GRAY)),
        GRAY        ('g', (s, f) -> s.withColor(ChatFormatting.GRAY)),
        GOLD        ('d', (s, f) -> s.withColor(ChatFormatting.GOLD)),
        DARK_PURPLE ('p', (s, f) -> s.withColor(ChatFormatting.DARK_PURPLE)),
        DARK_RED    ('n', (s, f) -> s.withColor(ChatFormatting.DARK_RED)),
        DARK_AQUA   ('q', (s, f) -> s.withColor(ChatFormatting.DARK_AQUA)),
        DARK_GREEN  ('e', (s, f) -> s.withColor(ChatFormatting.DARK_GREEN)),
        DARK_BLUE   ('v', (s, f) -> s.withColor(ChatFormatting.DARK_BLUE)),
        BLACK       ('k', (s, f) -> s.withColor(ChatFormatting.BLACK)),

        COLOR       ('#', (s, f) -> {
            try { return s.withColor(TextColor.parseColor("#" + f).getOrThrow(RuntimeException::new)); }
            catch (RuntimeException ex) { return s; }
        }, s -> { Matcher m = COLOR_EXTRACT.matcher(s); return m.find() ? m.group(1) : null; }),
        ;
        public final char code;
        public final BiFunction<Style, String, Style> applier;
        public final Function<String, String> container;
        Fmt(char code, BiFunction<Style, String, Style> applier) {
            this(code, applier, s -> s.indexOf(code) >= 0 ? Character.toString(code) : null);
        }
        Fmt(char code, BiFunction<Style, String, Style> applier, Function<String, String> container) {
            this.code = code; this.applier = applier; this.container = container;
        }
        public Style apply(String format, Style previous) {
            String fmt = container.apply(format);
            if (fmt != null) return applier.apply(previous, fmt);
            return previous;
        }
    }

    public static Style parseStyle(String style) {
        Style myStyle = Style.EMPTY.withColor(ChatFormatting.WHITE);
        for (Fmt cf : Fmt.values()) myStyle = cf.apply(style, myStyle);
        return myStyle;
    }

    /** Copied from Carpet: builds a component from a description string + previous component context. */
    private static MutableComponent getChatComponentFromDesc(String message, MutableComponent prev) {
        if (message.isEmpty()) return Component.literal("");
        if (Character.isWhitespace(message.charAt(0))) message = "w" + message;
        int limit = message.indexOf(' ');
        String desc = message, str = "";
        if (limit >= 0) { desc = message.substring(0, limit); str = message.substring(limit + 1); }
        if (prev == null) {
            MutableComponent text = Component.literal(str);
            text.setStyle(parseStyle(desc));
            return text;
        }
        Style prevStyle = prev.getStyle();
        MutableComponent ret = prev;
        prev.setStyle(switch (desc.charAt(0)) {
            case '?' -> prevStyle.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, message.substring(1)));
            case '!' -> prevStyle.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, message.substring(1)));
            case '^' -> prevStyle.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, c(message.substring(1))));
            case '@' -> prevStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message.substring(1)));
            default -> { ret = Component.literal(str); ret.setStyle(parseStyle(desc)); yield prevStyle; }
        });
        return ret;
    }

    /** Composes multiple objects into a single chat component. Carpet's {@code c()} method. */
    public static Component c(Object... fields) {
        MutableComponent message = Component.literal("");
        MutableComponent prev = null;
        for (Object o : fields) {
            if (o instanceof MutableComponent mc) { message.append(mc); prev = mc; continue; }
            if (o instanceof Component c) {
                MutableComponent mc = c.copy();
                message.append(mc); prev = mc; continue;
            }
            String txt = o.toString();
            MutableComponent comp = getChatComponentFromDesc(txt, prev);
            if (comp != prev) message.append(comp);
            prev = comp;
        }
        return message;
    }

    /** Simple styled text. */
    public static Component s(String text) { return s(text, ""); }
    public static Component s(String text, String style) {
        MutableComponent m = Component.literal(text);
        m.setStyle(parseStyle(style));
        return m;
    }

    /** Sends a message to a CommandSourceStack. */
    public static void m(CommandSourceStack source, Object... fields) {
        if (source != null) source.sendSuccess(() -> c(fields), false);
    }

    /** Sends to a player. */
    public static void m(Player player, Object... fields) {
        player.sendSystemMessage(c(fields));
    }

    /** Sends a collection of messages. */
    public static void send(CommandSourceStack source, Collection<Component> lines) {
        lines.forEach(s -> source.sendSuccess(() -> s, false));
    }
}
