package com.boydti.rededit.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.configuration.MemorySection;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.RunnableVal3;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public enum M {
    PREFIX("&8(&4&lEDIT&8)&r&7", "Info"),

    TOGGLE_TELEPORT("&7Set option disable-teleport to %s0", "Info"),
    TOGGLE_TELEPORT_SPECIFIC("&7Set option disable-teleport:%s0 to %s1", "Info"),
    TPA_REQUEST("&7%s0 wants to teleport to you. To accept use:\n&8 - &c/tpaccept %s0", "Info"),
    TPAHERE_REQUEST("&7%s0 wants you to teleport to them. To accept use &c/tpaccept %s0", "Info"),

    TPA_DENIED("&7%s0 has teleportation disabled", "Info"),
    TPA_DUPLICATE("&7%s0 already has a request from you", "Info"),
    TPA_ALLOWED("&7%s0 has been sent your teleport request", "Info"),
    TPA_ACCEPTED("&7Accepted teleport request from %s0", "Info"),
    TPA_REJECTED("&7Rejected teleport request from %s0", "Info"),
    TPA_ACCEPTED_SENDER("&7%s0 has accepted your teleport request", "Info"),

    NO_REQUEST_FOUND("&7No teleport request was found.", "Info"),
    SERVER_NOT_FOUND("&7Server not found: %s0", "Info"),
    HOME_NOT_FOUND("&7Your homes: %s0", "Info"),
    WARP_NOT_FOUND("&7Warp not found: %s0", "Info"),
    HOME_DELETED("&7Removed home: %s0", "Info"),
    WARP_DELETED("&7Removed warp: %s0", "Info"),
    HOME_SET("Created home %s0.", "Info"),
    WARP_SET("Created warp %s0.", "Info"),
    WARP_ALREADY_SET("There is already a warp called %s0.", "Info"),


    TELEPORTING("Teleporting to %s0", "Info"),
    NO_BACK("No previous position", "Info"),

    ;


    private static final HashMap<String, String> replacements = new HashMap<>();
    /**
     * Translated
     */
    private String s;
    /**
     * Default
     */
    private String d;
    /**
     * What locale category should this translation fall under
     */
    private String cat;
    /**
     * Should the string be prefixed?
     */
    private boolean prefix;

    /**
     * Constructor for custom strings.
     */
    M() {
        /*
         * use setCustomString();
         */
    }

    /**
     * Constructor
     *
     * @param d default
     * @param prefix use prefix
     */
    M(final String d, final boolean prefix, final String cat) {
        this.d = d;
        if (this.s == null) {
            this.s = d;
        }
        this.prefix = prefix;
        this.cat = cat.toLowerCase();
    }

    /**
     * Constructor
     *
     * @param d default
     */
    M(final String d, final String cat) {
        this(d, true, cat.toLowerCase());
    }

    public String f(final Object... args) {
        return format(args);
    }

    public String format(final Object... args) {
        String m = this.s;
        for (int i = args.length - 1; i >= 0; i--) {
            if (args[i] == null) {
                continue;
            }
            m = m.replaceAll("%s" + i, args[i].toString());
        }
        if (args.length > 0) {
            m = m.replaceAll("%s", args[0].toString());
        }
        return m;
    }

    public static void load(final File file) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            final YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            final Set<String> keys = yml.getKeys(true);
            final EnumSet<M> all = EnumSet.allOf(M.class);
            final HashSet<String> allNames = new HashSet<>();
            final HashSet<String> allCats = new HashSet<>();
            final HashSet<String> toRemove = new HashSet<>();
            for (final M c : all) {
                allNames.add(c.name());
                allCats.add(c.cat.toLowerCase());
            }
            final HashSet<M> captions = new HashSet<>();
            boolean changed = false;
            for (final String key : keys) {
                final Object value = yml.get(key);
                if (value instanceof MemorySection) {
                    continue;
                }
                final String[] split = key.split("\\.");
                final String node = split[split.length - 1].toUpperCase();
                final M caption = allNames.contains(node) ? valueOf(node) : null;
                if (caption != null) {
                    if (!split[0].equalsIgnoreCase(caption.cat)) {
                        changed = true;
                        yml.set(key, null);
                        yml.set(caption.cat + "." + caption.name().toLowerCase(), value);
                    }
                    captions.add(caption);
                    caption.s = (String) value;
                } else {
                    toRemove.add(key);
                }
            }
            for (final String remove : toRemove) {
                changed = true;
                yml.set(remove, null);
            }
            replacements.clear();
            for (final char letter : "1234567890abcdefklmnor".toCharArray()) {
                replacements.put("&" + letter, "\u00a7" + letter);
            }
            replacements.put("\\\\n", "\n");
            replacements.put("\\n", "\n");
            replacements.put("&-", "\n");
            for (final M caption : all) {
                if (!captions.contains(caption)) {
                    changed = true;
                    yml.set(caption.cat + "." + caption.name().toLowerCase(), caption.d);
                }
                caption.s = StringMan.replaceFromMap(caption.s, replacements);
            }
            if (changed) {
                yml.save(file);
            }
        } catch (final Exception e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public String toString() {
        return s();
    }

    public boolean isEmpty() {
        return length() == 0;
    }

    public int length() {
        return toString().length();
    }

    public static String color(String string) {
        return StringMan.replaceFromMap(string, replacements);
    }

    public String s() {
        return this.s;
    }

    public String original() {
        return d;
    }

    public boolean usePrefix() {
        return this.prefix;
    }

    public String getCat() {
        return this.cat;
    }

    public M or(M... others) {
        int index = PseudoRandom.random.nextInt(others.length + 1);
        return index == 0 ? this : others[index - 1];
    }

    public static String getPrefix() {
        return (PREFIX.isEmpty() ? "" : color(PREFIX.s()) + " ");
    }

    public void send(Object actor, final Object... args) {
        if (isEmpty()) {
            return;
        }
        if (actor == null) {
            Fawe.debug(this.format(args));
        } else {
            try {
                Method method = actor.getClass().getMethod("print", String.class);
                method.setAccessible(true);
                method.invoke(actor, (PREFIX.isEmpty() ? "" : PREFIX.s() + " ") + this.format(args));
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(final FawePlayer<?> player, final Object... args) {
        if (isEmpty()) {
            return;
        }
        if (player == null) {
            Fawe.debug(this.format(args));
        } else {
            player.sendMessage((PREFIX.isEmpty() ? "" : PREFIX.s() + " ") + this.format(args));
        }
    }

    public static String getColorName(char code) {
        switch (code) {
            case '0': return "BLACK";
            case '1': return "DARK_BLUE";
            case '2': return "DARK_GREEN";
            case '3': return "DARK_AQUA";
            case '4': return "DARK_RED";
            case '5': return "DARK_PURPLE";
            case '6': return "GOLD";
            default:
            case '7': return "GRAY";
            case '8': return "DARK_GRAY";
            case '9': return "BLUE";
            case 'a': return "GREEN";
            case 'b': return "AQUA";
            case 'c': return "RED";
            case 'd': return "LIGHT_PURPLE";
            case 'e': return "YELLOW";
            case 'f': return "WHITE";
            case 'k': return "OBFUSCATED";
            case 'l': return "BOLD";
            case 'm': return "STRIKETHROUGH";
            case 'n': return "UNDERLINE";
            case 'o': return "ITALIC";
            case 'r': return "RESET";
        }
    }

    /**
     *
     * @param m
     * @param runPart Part, Color, NewLine
     */
    public static void splitMessage(String m, RunnableVal3<String, String, Boolean> runPart) {
        m = color(m);
        String color = "GRAY";
        boolean newline = false;
        for (String line : m.split("\n")) {
            boolean hasColor = line.charAt(0) == '\u00A7';
            String[] splitColor = line.split("\u00A7");
            for (String part : splitColor) {
                if (hasColor) {
                    color = getColorName(part.charAt(0));
                    part = part.substring(1);
                }
                runPart.run(part, color, newline);
                hasColor = true;
            }
            newline = true;
        }
    }
}
