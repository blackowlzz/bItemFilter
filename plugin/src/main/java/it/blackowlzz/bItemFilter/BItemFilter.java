package it.blackowlzz.bItemFilter;

import it.blackowlzz.bItemFilter.gui.FilterGui;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class BItemFilter extends JavaPlugin {

    public static String PLUGIN_VERSION = "UNKNOWN";

    private FilterManager filterManager;
    private FilterGui filterGui;
    private SQLiteDataStore dataStore;
    private UpdateChecker updateChecker;
    private String prefix;
    private boolean smallCaps;

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        try { PLUGIN_VERSION = getDescription().getVersion(); } catch (Throwable ignored) {}

        smallCaps = VersionUtil.SMALL_CAPS_SUPPORTED;
        prefix    = smallCaps ? "&b[ʙɪᴛᴇᴍꜰɪʟᴛᴇʀ]" : "&b[bItemFilter]";

        FilterMode defaultMode;
        try {
            defaultMode = FilterMode.valueOf(
                    getConfig().getString("general.default-mode", "BLACKLIST").toUpperCase());
        } catch (IllegalArgumentException e) {
            defaultMode = FilterMode.BLACKLIST;
        }
        boolean defaultEnabled = getConfig().getBoolean("general.default-enabled", true);
        int maxItems = getConfig().getInt("general.max-items", 0);

        dataStore = new SQLiteDataStore(getDataFolder(), getLogger(), defaultEnabled, defaultMode);
        if (!dataStore.initialize()) {
            getLogger().severe("SQLite failed to initialize - disabling bItemFilter.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        filterManager = new FilterManager(dataStore);
        filterManager.setDefaults(defaultEnabled, defaultMode, maxItems);
        filterManager.load();

        filterGui = new FilterGui(this, filterManager);
        getServer().getPluginManager().registerEvents(filterGui, this);

        if (VersionUtil.ENTITY_PICKUP_SUPPORTED) {
            getServer().getPluginManager().registerEvents(new FilterListener(filterManager), this);
            getLogger().info("Pickup listener: EntityPickupItemEvent (1.12+)");
        } else {
            getServer().getPluginManager().registerEvents(new LegacyFilterListener(filterManager), this);
            getLogger().info("Pickup listener: PlayerPickupItemEvent (pre-1.12 legacy)");
        }

        FilterCommand cmd = new FilterCommand(this, filterManager, filterGui);
        for (String alias : List.of("ifilter", "bitemfilter", "itemfilter", "filter")) {
            PluginCommand pc = getCommand(alias);
            if (pc != null) { pc.setExecutor(cmd); pc.setTabCompleter(cmd); }
        }

        updateChecker = new UpdateChecker(this);
        updateChecker.start();

        getLogger().info("bItemFilter enabled. Version: " + PLUGIN_VERSION);
    }

    @Override
    public void onDisable() {
        if (updateChecker != null) updateChecker.stop();
        if (filterManager != null) filterManager.save();
        if (dataStore != null) dataStore.close();
    }

    public FilterManager getFilterManager() { return filterManager; }
    public FilterGui getFilterGui() { return filterGui; }
    public String getPrefix() { return prefix; }
    public boolean isSmallCaps() { return smallCaps; }

    // only for GUI titles and messages, not for config keys or anything else, might fucking destroy everything on older versions since old mc didn't like fucking fonts but hey, small caps -- P.S fuck it im disabling this on versions older than 1.20.5 so no strange issue spawns
    public String sc(String text) {
        return smallCaps ? VersionUtil.toSmallCaps(text) : text;
    }

    public String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public String msg(String path, String def) {
        return color(getConfig().getString(path, def).replace("%prefix%", prefix));
    }
}
