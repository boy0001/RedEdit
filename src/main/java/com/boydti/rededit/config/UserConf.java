package com.boydti.rededit.config;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UserConf extends LocalConfig {
    public UserConf(File folder, UUID user) {
        super(new File(folder, user + ".yml"));
    }

    @Create // This value will be generated automatically
    public ConfigBlock<HOME> HOME;

    @Comment({"The user's home locations"})
    @BlockName({}) // The name for the default block
    public static class HOME extends ConfigBlock {
        public int server = 0;
        public String world = "world";
        public int x = 0;
        public int y = 0;
        public int z = 0;
    }

    public HOME getHome(String name) {
        return HOME.get(name);
    }

    public HOME deleteHome(String name) {
        return HOME.remove(name);
    }

    public Collection<HOME> getHomes() {
        return Collections.unmodifiableCollection(HOME.getInstances());
    }

    public void addHome(String name, HOME home) {
        HOME.put(name, home);
    }
}
