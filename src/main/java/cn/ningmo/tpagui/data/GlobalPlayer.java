package cn.ningmo.tpagui.data;

import java.util.UUID;

public class GlobalPlayer {
    private final String name;
    private final UUID uuid;
    private final String server;

    public GlobalPlayer(String name, UUID uuid, String server) {
        this.name = name;
        this.uuid = uuid;
        this.server = server;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getServer() {
        return server;
    }
}
