package com.boydti.rededit.config;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class WarpConf extends LocalConfig {
    public WarpConf(File file) {
        super(file);
    }

    @Create // This value will be generated automatically
    public ConfigBlock<WARP> WARP;

    @BlockName({}) // The name for the default block
    public static class WARP extends ConfigBlock {
        public String world = "world";
        public int server = 0;
        public int x = 0;
        public int y = 0;
        public int z = 0;
    }

    public WARP deleteWarp(String warp) {
        return WARP.remove(warp);
    }

    public WARP getWarp(String name) {
        return WARP.get(name);
    }

    public Collection<WARP> getWarp() {
        return Collections.unmodifiableCollection(WARP.getInstances());
    }

    public void addWarp(String name, WARP home) {
        WARP.put(name, home);
    }
}
