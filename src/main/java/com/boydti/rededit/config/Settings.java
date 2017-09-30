package com.boydti.rededit.config;

import com.boydti.fawe.config.Config;

public class Settings extends Config {
    @Ignore
    public static Settings IMP = new Settings();

    @Comment("Redis Service IP")
    public String IP = "127.0.0.1";
    @Comment("Redis Service Port")
    public int PORT = 6379;
    @Comment("Redis Password")
    public String PASSWORD = "";
    @Comment("The group this server belongs to")
    public int SERVER_GROUP = 1;
    @Comment({
            "This server's unique id",
            " - Must be in the range: [0,65535]"
    })
    public int SERVER_ID = 1;

    public boolean DEBUG = true;
    public boolean PLOTSQUARED_SYNC = true;

    @Create
    public PATHS PATHS;

    @Comment("Paths for various directories")
    public static final class PATHS {
        public String USERS = "users";
        public String WARPS = "warps";
    }
}
