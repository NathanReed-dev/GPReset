package org.plutonix.gPReset.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Msg {
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final String PREFIX = "<gray>[<yellow>GP Reset</yellow>]</gray> ";

    public static Component raw(String msg) {
        return mm.deserialize(PREFIX + msg);
    }
    public static Component error (String msg) { return mm.deserialize(PREFIX + "<red>" + msg + "</red>"); }
    public static Component hError(String msg) {
        return mm.deserialize(PREFIX + "<red><bold>" + msg + "</bold></red>");
    }
    public static Component warn(String msg) {
        return mm.deserialize(PREFIX + "<yellow>" + msg + "</yellow>");
    }
    public static Component hWarn(String msg) { return mm.deserialize(PREFIX + "<yellow><bold>" + msg + "</bold></yellow>"); }
    public static Component info(String msg) {
        return mm.deserialize(PREFIX + "<gray>" + msg + "</gray>");
    }
    public  static Component hInfo(String msg) { return mm.deserialize(PREFIX + "<gray><bold>" + msg + "</bold><gray>"); }
}
