package com.boydti.rededit.config;

import com.boydti.fawe.config.Config;
import java.io.File;

public abstract class LocalConfig extends Config {
    @Config.Ignore
    private final File file;

    public LocalConfig(File file) {
        this.file = file;
        reload();
    }

    public void reload() {
        load(file);
        save(file);
    }

    public void save() {
        save(file);
    }

    public boolean load() {
        return load(file);
    }
}
