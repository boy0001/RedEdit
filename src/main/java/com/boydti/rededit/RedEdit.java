package com.boydti.rededit;

import com.boydti.fawe.util.MainUtil;
import com.boydti.rededit.command.teleport.TeleportCommands;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.config.UserConf;
import com.boydti.rededit.config.WarpConf;
import com.boydti.rededit.listener.PlayerListener;
import com.boydti.rededit.listener.RedEditPubSub;
import com.boydti.rededit.listener.Network;
import com.boydti.rededit.remote.Channel;
import com.boydti.rededit.util.M;
import com.boydti.rededit.util.MapUtil;
import com.boydti.rededit.util.plot.PlotLoader;
import com.boydti.rededit.util.TeleportUtil;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.extension.platform.CommandManager;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
    private final TeleportUtil util;

    private volatile WarpConf warps;
    private final LoadingCache<UUID, UserConf> users;

    private List<Thread> RUNNING = new ArrayList<>();
    private Jedis JEDIS;
    private PlayerListener playerListener;

    public RedEdit(IRedEditPlugin imp) throws URISyntaxException, IOException {
        INSTANCE = this;
        IMP = imp;
        URL url = RedEdit.class.getProtectionDomain().getCodeSource().getLocation();
        FILE = new File(new URL(url.toURI().toString().split("\\!")[0].replaceAll("jar:file", "file")).toURI().getPath());
        DIR = new File(FILE.getParentFile(), "RedEdit");
        SETTINGS_FILE = new File(DIR, "config.yml");
        M.load(new File(DIR, "messages.yml"));
        setupConfig();
        if (Settings.IMP.PASSWORD.isEmpty()) {
            POOL = new JedisPool(new JedisPoolConfig(), Settings.IMP.IP, Settings.IMP.PORT, 0);
        } else {
            POOL = new JedisPool(new JedisPoolConfig(), Settings.IMP.IP, Settings.IMP.PORT, 0, Settings.IMP.PASSWORD);
        }
        LISTENER = new RedEditPubSub(POOL);
        PlotLoader loader = null;
        try {
            loader = new PlotLoader();
        } catch (Throwable ignore) {}
        this.util = new TeleportUtil(loader);
        // Subscribe
        // 0000 - Any + Any
        // GG00 - Group + Any
        // GGII - Group + Server id
        // 00II - Any + Server id
        subscribe();
        setupEvents();
        setupCommands();
        users = MapUtil.getExpiringMap(60, TimeUnit.SECONDS);
    }

    public WarpConf getWarpConfig() {
        WarpConf tmp = warps;
        if (tmp == null) {
            warps = tmp = new WarpConf(MainUtil.getFile(DIR, Settings.IMP.PATHS.WARPS + File.separator + "warps.yml"));
            tmp.load();
        }
        return tmp;
    }

    public void unloadWarps() {
        warps = null;
    }

    public UserConf getUserConf(UUID user) {
        UserConf conf = users.getIfPresent(user);
        if (conf == null) {
            conf = new UserConf(MainUtil.getFile(DIR, Settings.IMP.PATHS.USERS), user);
            users.put(user, conf);
        }
        return conf;
    }

    public static RedEdit get() {
        return INSTANCE;
    }

    public static IRedEditPlugin imp() {
        return INSTANCE.IMP;
    }

    public TeleportUtil getTeleportUtil() {
        return util;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public RedEditPubSub getScheduler() {
        return LISTENER;
    }

    public Network getNetwork() {
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
        CommandManager.getInstance().registerCommands(new TeleportCommands(util, getNetwork()));
    }

    private void setupEvents() {
        IMP.registerEvents();
        this.playerListener = new PlayerListener(getNetwork());
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
