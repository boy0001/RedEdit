package com.boydti.rededit;

import com.boydti.rededit.command.teleport.TeleportCommands;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.listener.PlayerListener;
import com.boydti.rededit.listener.RedEditPubSub;
import com.boydti.rededit.listener.ServerController;
import com.boydti.rededit.remote.Channel;
import com.boydti.rededit.util.RedUtil;
import com.sk89q.worldedit.extension.platform.CommandManager;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Uses Redis to run code on multiple server instances with minimal effort
 */
public class RedEdit {

    private static RedEdit INSTANCE;

    private final IRedEditPlugin IMP;
    private final JedisPool POOL;
    private final RedEditPubSub LISTENER;
    private final File SETTINGS_FILE;
    private final File DIR;
    private final File FILE;
    private final RedUtil util;

    private List<Thread> RUNNING = new ArrayList<>();
    private Jedis JEDIS;
    private PlayerListener playerListener;

    public RedEdit(IRedEditPlugin imp) throws URISyntaxException, MalformedURLException {
        INSTANCE = this;
        IMP = imp;
        URL url = RedEdit.class.getProtectionDomain().getCodeSource().getLocation();
        FILE = new File(new URL(url.toURI().toString().split("\\!")[0].replaceAll("jar:file", "file")).toURI().getPath());
        DIR = new File(FILE.getParentFile(), "PlotHTTP");
        SETTINGS_FILE = new File("RedEdit/config.yml");
        setupConfig();
        if (Settings.IMP.PASSWORD.isEmpty()) {
            POOL = new JedisPool(new JedisPoolConfig(), Settings.IMP.IP, Settings.IMP.PORT, 0);
        } else {
            POOL = new JedisPool(new JedisPoolConfig(), Settings.IMP.IP, Settings.IMP.PORT, 0, Settings.IMP.PASSWORD);
        }
        LISTENER = new RedEditPubSub(POOL);
        this.util = new RedUtil();
        // Subscribe
        // 0000 - Any + Any
        // GG00 - Group + Any
        // GGII - Group + Server id
        // 00II - Any + Server id
        subscribe();
        setupEvents();
        setupCommands();
    }

    public static RedEdit get() {
        return INSTANCE;
    }

    public static IRedEditPlugin imp() {
        return INSTANCE.IMP;
    }

    public RedUtil getUtil() {
        return util;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public RedEditPubSub getScheduler() {
        return LISTENER;
    }

    public ServerController getServerController() {
        return LISTENER;
    }

    public void close() {
        if (LISTENER != null) {
            LISTENER.close();
        }
        if (POOL != null) {
            POOL.close();
        }
        for (Thread thread : RUNNING) {
            try {
                thread.interrupt();
                Thread.sleep(50);
                thread.stop();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (JEDIS != null) {
            try {
                POOL.returnBrokenResource(JEDIS);
                POOL.returnResource(JEDIS);
            } catch (JedisException ignore) {}
        }
    }

    private void setupCommands() {
        CommandManager.getInstance().registerCommands(new TeleportCommands(util, getServerController()));
    }

    private void setupEvents() {
        IMP.registerEvents();
        this.playerListener = new PlayerListener(getServerController());
    }

    private void subscribe() {
        if (this.JEDIS != null) {
            POOL.returnBrokenResource(JEDIS);
            POOL.returnResource(JEDIS);
        }
        RUNNING.clear();
        this.JEDIS = POOL.getResource();
        RUNNING.add(new Thread(new Runnable() {
            @Override
            public void run() {
                JEDIS.subscribe(LISTENER, Channel.getId(0, 0), Channel.getId(0, Settings.IMP.SERVER_ID), Channel.getId(Settings.IMP.SERVER_GROUP, Settings.IMP.SERVER_ID), Channel.getId(Settings.IMP.SERVER_GROUP, 0));
            }
        }));
        for (Thread thread : RUNNING) {
            thread.start();
        }
    }

    private void setupConfig() {
        Settings.IMP.load(SETTINGS_FILE);
        Settings.IMP.save(SETTINGS_FILE);
    }
}
