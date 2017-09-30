package com.boydti.rededit.config;

import com.boydti.fawe.config.Config;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
        try {
            File tempFile = File.createTempFile("warp.yml", ".yml");
            save(tempFile);
            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean load() {
        return load(file);
    }
}