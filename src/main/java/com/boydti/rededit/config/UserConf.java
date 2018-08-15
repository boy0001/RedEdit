package com.boydti.rededit.config;

import com.boydti.rededit.RedEdit;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UserConf extends LocalConfig {
    private final UUID user;

    public UserConf(File folder, UUID user) {
        super(new File(folder, user + ".yml"));
        this.user = user;
    }

    @Create // This value will be generated automatically
    public ConfigBlock<UserConf.HOMES> HOMES;

    @Comment({"The user's home locations"})
    @BlockName({}) // The name for the default block
    public static class HOMES extends ConfigBlock {
        public int SERVER = Integer.MIN_VALUE;
        public int GROUP = Integer.MIN_VALUE;
        public String WORLD = "world";
        public double X = 0;
        public double Y = 0;
        public double Z = 0;
    }

    public UserConf.HOMES getHome(String name) {
        return HOMES.get(name);
    }

    public UserConf.HOMES deleteHome(String name) {
        return HOMES.remove(name);
    }

    public Collection<UserConf.HOMES> getHomes() {
        return Collections.unmodifiableCollection(HOMES.getInstances());
    }

    public void addHome(String name, UserConf.HOMES home) {
        HOMES.put(name, home);
    }

    @Override
    public void save() {
        super.save();
        RedEdit.get().unloadUser(this.user, false);
    }
}
