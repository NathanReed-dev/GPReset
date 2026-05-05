package org.plutonix.gPReset.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Msg {
    public static final MiniMessage mm = MiniMessage.miniMessage();

    public static Component raw(String msg) {
        return mm.deserialize(msg);
    }
    public static Component error(String msg) {
        return mm.deserialize("<bold><red>" + msg + "</red></bold>");
    }
    public static Component warn(String msg) {
        return mm.deserialize("<yellow>" + msg + "</yellow>");
    }
    public static Component info(String msg) {
        return mm.deserialize("<gray>" + msg + "</gray>");
    }
}
