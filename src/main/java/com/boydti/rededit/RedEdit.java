package com.boydti.rededit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.boydti.rededit.command.teleport.RedEditCommand;
import com.boydti.rededit.command.teleport.TeleportCommands;
import com.boydti.rededit.config.Settings;
import com.boydti.rededit.config.UserConf;
import com.boydti.rededit.config.WarpConf;
import com.boydti.rededit.listener.Network;
import com.boydti.rededit.listener.PlayerListener;
import com.boydti.rededit.listener.RedEditPubSub;
import com.boydti.rededit.remote.Channel;
import com.boydti.rededit.remote.RemoteCall;
import com.boydti.rededit.remote.Server;
import com.boydti.rededit.serializer.UUIDSerializer;
import com.boydti.rededit.serializer.VoidSerializer;
import com.boydti.rededit.test.BasicTaskMan;
import com.boydti.rededit.test.NullPlugin;
import com.boydti.rededit.util.M;
import com.boydti.rededit.util.MapUtil;
import com.boydti.rededit.util.TeleportUtil;
import com.boydti.rededit.util.plot.PlotLoader;
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


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uses Redis to run code on multiple server instances with minimal effort
 */
public class RedEdit {

    private static volatile RedEdit INSTANCE;
    private PlotLoader loader;

    private IRedEditPlugin IMP = new NullPlugin();
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

    private static RemoteCall<Object, UUID> unloadUser;

    private RedEdit(IRedEditPlugin plugin) throws URISyntaxException, IOException {
        if (TaskManager.IMP == null) TaskManager.IMP = new BasicTaskMan(5);
        INSTANCE = this;
        INSTANCE.IMP = plugin;
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
        if (Settings.IMP.PLOTSQUARED_SYNC) {
            try {
                loader = new PlotLoader();
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
        }
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
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                close();
            }
        }));
        if (unloadUser == null) {
            unloadUser = new RemoteCall<Object, UUID>() {
                @Override
                public Object run(Server sender, UUID arg) {
                    unloadUser(arg, true);
                    return null;
                }
            }.setSerializer(new VoidSerializer(), new UUIDSerializer());
        }
    }

    public static void debug(Object o) {
        if (Settings.IMP.DEBUG) {
            Fawe.debugPlain(M.getPrefix() + o);
        }
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

    public void unloadUser(UUID uuid, boolean local) {
        if (local) {
            users.invalidate(uuid);
        } else {
            unloadUser.call(0, 0, uuid);
        }
    }

    public UserConf getUserConf(UUID user) {
        UserConf conf = users.getIfPresent(user);
        if (conf == null) {
            conf = new UserConf(MainUtil.getFile(DIR, Settings.IMP.PATHS.USERS), user);
            users.put(user, conf);
        }
        return conf;
    }

    public PlotLoader getPlotLoader() {
        return loader;
    }

    public static RedEdit get() {
        RedEdit tmp = INSTANCE;
        if (tmp == null) {
            try {
                INSTANCE = tmp = new RedEdit(new NullPlugin());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tmp;
    }

    public static IRedEditPlugin imp() {
        return get().IMP;
    }

    public boolean isInitialized() {
        return !(IMP instanceof NullPlugin);
    }

    public static void init(IRedEditPlugin plugin) {
        checkNotNull(plugin);
        RedEdit tmp = INSTANCE;
        if (tmp == null) {
            try {
                INSTANCE = tmp = new RedEdit(plugin);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            tmp.IMP = plugin;
        }
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

    public synchronized void close() {
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
        RUNNING.clear();
        if (JEDIS != null) {
            try {
                POOL.returnBrokenResource(JEDIS);
                POOL.returnResource(JEDIS);
            } catch (JedisException ignore) {}
            JEDIS = null;
        }
    }

    private void setupCommands() {
        CommandManager.getInstance().registerCommands(new TeleportCommands(util, getNetwork()));
        CommandManager.getInstance().registerCommands(new RedEditCommand(), "rededit");
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

    public void setupConfig() {
        unloadWarps();
        Settings.IMP.load(SETTINGS_FILE);
        Settings.IMP.save(SETTINGS_FILE);
    }
}
