package it.blackowlzz.bItemFilter.gui;

import it.blackowlzz.bItemFilter.BItemFilter;
import it.blackowlzz.bItemFilter.FilterManager;
import it.blackowlzz.bItemFilter.FilterMode;
import it.blackowlzz.bItemFilter.PlayerFilter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public final class FilterGui implements Listener {

    // Layout (54-slot inventory, 6 rows):
    // Rows 1-4 (slots  0-35): filtered items  [PAGE_SIZE = 36]
    // Row  5   (slots 36-44): separator
    // Row  6   (slots 45-53): controls
    private static final int PAGE_SIZE   = 36;
    private static final int SEP_START   = 36;
    private static final int SEP_END     = 44;
    private static final int SLOT_PREV   = 45;
    private static final int SLOT_MODE   = 47;
    private static final int SLOT_TOGGLE = 49;
    private static final int SLOT_CLEAR  = 51;
    private static final int SLOT_NEXT   = 53;

    private final BItemFilter plugin;
    private final FilterManager manager;

    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Set<UUID> open      = new HashSet<>();
    private final Set<UUID> rendering = new HashSet<>();

    public FilterGui(BItemFilter plugin, FilterManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        playerPage.putIfAbsent(uuid, 0);
        open.add(uuid);
        render(player);
    }

    private void render(Player player) {
        UUID uuid   = player.getUniqueId();
        int page    = playerPage.getOrDefault(uuid, 0);
        PlayerFilter filter = manager.getOrCreate(player.getUniqueId());

        List<String> allMaterials = new ArrayList<>(filter.getMaterials());
        int totalPages = Math.max(1, (int) Math.ceil(allMaterials.size() / (double) PAGE_SIZE));
        if (page >= totalPages) { page = totalPages - 1; playerPage.put(uuid, page); }

        String rawTitle = plugin.getConfig().getString("gui.title", "&b&lItem Filter");
        String modeTag  = filter.getMode() == FilterMode.BLACKLIST ? " &8[&cBL&8]" : " &8[&aWL&8]";
        String enTag    = filter.isEnabled() ? " &8[&aON&8]" : " &8[&cOFF&8]";
        String title    = plugin.color(plugin.sc(rawTitle + modeTag + enTag)
                + plugin.color(" &8- &7") + (page + 1) + "/" + totalPages);

        @SuppressWarnings("deprecation")
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack filler    = buildItem(getMaterial("gui.filler-material",    "GRAY_STAINED_GLASS_PANE"), " ", null);
        ItemStack separator = buildItem(getMaterial("gui.separator-material", "BLACK_STAINED_GLASS_PANE"), " ", null);

        // Content rows (0-35): filtered item icons
        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, allMaterials.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildFilterEntry(allMaterials.get(i)));
        }
        for (int i = end - start; i < PAGE_SIZE; i++) inv.setItem(i, filler);

        // Empty placeholder at slot 0
        if (allMaterials.isEmpty()) {
            String emptyTitle = plugin.sc(plugin.getConfig().getString("gui.empty-title", "&7Filter is empty"));
            inv.setItem(0, buildItem(Material.BARRIER,
                    emptyTitle,
                    colorList(plugin.getConfig().getStringList("gui.empty-lore"))));
        }

        // Separator row
        for (int i = SEP_START; i <= SEP_END; i++) inv.setItem(i, separator);

        // Navigation row base
        for (int i = 45; i <= 53; i++) inv.setItem(i, filler);

        // Prev
        if (page > 0) {
            String name = plugin.sc(plugin.getConfig().getString("gui.prev-name", "&7Previous Page"));
            inv.setItem(SLOT_PREV, buildItem(getMaterial("gui.prev-material", "ARROW"), name, null));
        }

        // Mode toggle
        if (filter.getMode() == FilterMode.BLACKLIST) {
            String name = plugin.sc(plugin.getConfig().getString("gui.mode-blacklist-name", "&c&lBlacklist Mode"));
            inv.setItem(SLOT_MODE, buildItem(
                    getMaterial("gui.mode-blacklist-material", "RED_STAINED_GLASS_PANE"),
                    name,
                    colorList(plugin.getConfig().getStringList("gui.mode-blacklist-lore"))));
        } else {
            String name = plugin.sc(plugin.getConfig().getString("gui.mode-whitelist-name", "&a&lWhitelist Mode"));
            inv.setItem(SLOT_MODE, buildItem(
                    getMaterial("gui.mode-whitelist-material", "LIME_STAINED_GLASS_PANE"),
                    name,
                    colorList(plugin.getConfig().getStringList("gui.mode-whitelist-lore"))));
        }

        // Filter enabled toggle
        if (filter.isEnabled()) {
            String name = plugin.sc(plugin.getConfig().getString("gui.toggle-filter-name-on", "&a&lFiltering: ON"));
            inv.setItem(SLOT_TOGGLE, buildItem(
                    getMaterial("gui.toggle-filter-material-on", "LIME_WOOL"),
                    name,
                    colorList(plugin.getConfig().getStringList("gui.toggle-filter-lore-on"))));
        } else {
            String name = plugin.sc(plugin.getConfig().getString("gui.toggle-filter-name-off", "&c&lFiltering: OFF"));
            inv.setItem(SLOT_TOGGLE, buildItem(
                    getMaterial("gui.toggle-filter-material-off", "RED_WOOL"),
                    name,
                    colorList(plugin.getConfig().getStringList("gui.toggle-filter-lore-off"))));
        }

        // Clear
        String clearName = plugin.sc(plugin.getConfig().getString("gui.clear-name", "&c&lClear All"));
        inv.setItem(SLOT_CLEAR, buildItem(
                getMaterial("gui.clear-material", "BARRIER"),
                clearName,
                colorList(plugin.getConfig().getStringList("gui.clear-lore"))));

        // Next
        if (page < totalPages - 1) {
            String name = plugin.sc(plugin.getConfig().getString("gui.next-name", "&7Next Page"));
            inv.setItem(SLOT_NEXT, buildItem(getMaterial("gui.next-material", "ARROW"), name, null));
        }

        rendering.add(uuid);
        player.openInventory(inv);
        rendering.remove(uuid);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!open.contains(uuid)) return;

        Inventory clicked = event.getClickedInventory();

        // Click in player's own inventory (bottom half): toggle item in filter
        if (clicked != null && clicked.equals(player.getInventory())) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            String material = item.getType().name();
            PlayerFilter filter = manager.getOrCreate(uuid);

            if (filter.hasMaterial(material)) {
                filter.removeMaterial(material);
                manager.savePlayer(filter);
                player.sendMessage(plugin.msg("messages.item-removed",
                        "%prefix% &7Removed &b%material% &7from your filter.")
                        .replace("%material%", formatMaterial(material)));
            } else {
                int max = manager.getMaxItems();
                if (max > 0 && filter.getItemCount() >= max) {
                    player.sendMessage(plugin.msg("messages.max-items-reached",
                            "%prefix% &7Maximum filter size reached &8(%max% items)&7.")
                            .replace("%max%", String.valueOf(max)));
                    return;
                }
                filter.addMaterial(material);
                manager.savePlayer(filter);
                player.sendMessage(plugin.msg("messages.item-added",
                        "%prefix% &7Added &b%material% &7to your filter.")
                        .replace("%material%", formatMaterial(material)));
            }
            render(player);
            return;
        }

        int slot = event.getRawSlot();
        if (slot >= 0 && slot < 54) event.setCancelled(true);
        if (slot < 0 || slot >= 54) return;

        PlayerFilter filter = manager.getOrCreate(uuid);

        // Content area: click removes that material from filter
        if (slot < PAGE_SIZE) {
            int idx = playerPage.getOrDefault(uuid, 0) * PAGE_SIZE + slot;
            List<String> mats = new ArrayList<>(filter.getMaterials());
            if (idx < mats.size()) {
                String mat = mats.get(idx);
                filter.removeMaterial(mat);
                manager.savePlayer(filter);
                player.sendMessage(plugin.msg("messages.item-removed",
                        "%prefix% &7Removed &b%material% &7from your filter.")
                        .replace("%material%", formatMaterial(mat)));
                if (playerPage.getOrDefault(uuid, 0) >= Math.max(1, (int) Math.ceil(filter.getItemCount() / (double) PAGE_SIZE))) {
                    playerPage.put(uuid, Math.max(0, playerPage.getOrDefault(uuid, 0) - 1));
                }
                render(player);
            }
            return;
        }

        // Separator: ignore
        if (slot >= SEP_START && slot <= SEP_END) return;

        // Navigation row
        if (slot == SLOT_PREV) {
            int p = playerPage.getOrDefault(uuid, 0);
            if (p > 0) { playerPage.put(uuid, p - 1); render(player); }

        } else if (slot == SLOT_NEXT) {
            int p = playerPage.getOrDefault(uuid, 0);
            int total = Math.max(1, (int) Math.ceil(filter.getItemCount() / (double) PAGE_SIZE));
            if (p < total - 1) { playerPage.put(uuid, p + 1); render(player); }

        } else if (slot == SLOT_MODE) {
            filter.toggleMode();
            manager.savePlayer(filter);
            String msgKey = filter.getMode() == FilterMode.BLACKLIST
                    ? "messages.mode-blacklist" : "messages.mode-whitelist";
            String def = filter.getMode() == FilterMode.BLACKLIST
                    ? "%prefix% &7Mode set to &cBlacklist&7."
                    : "%prefix% &7Mode set to &aWhitelist&7.";
            player.sendMessage(plugin.msg(msgKey, def));
            render(player);

        } else if (slot == SLOT_TOGGLE) {
            filter.toggleEnabled();
            manager.savePlayer(filter);
            String msgKey = filter.isEnabled() ? "messages.filter-enabled" : "messages.filter-disabled";
            String def = filter.isEnabled()
                    ? "%prefix% &7Item filtering &aenabled&7."
                    : "%prefix% &7Item filtering &cdisabled&7.";
            player.sendMessage(plugin.msg(msgKey, def));
            render(player);

        } else if (slot == SLOT_CLEAR) {
            if (event.isShiftClick()) {
                filter.clear();
                manager.savePlayer(filter);
                playerPage.put(uuid, 0);
                player.sendMessage(plugin.msg("messages.filter-cleared", "%prefix% &7Your filter has been cleared."));
                render(player);
            } else {
                player.sendMessage(plugin.msg("messages.clear-confirm", "%prefix% &cShift-click the barrier to confirm clear."));
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (rendering.contains(uuid)) return;
        open.remove(uuid);
        playerPage.remove(uuid);
    }

    private ItemStack buildFilterEntry(String material) {
        Material mat = Material.AIR;
        try { mat = Material.valueOf(material); } catch (IllegalArgumentException ignored) {}
        if (mat == Material.AIR) mat = Material.STONE;

        List<String> loreTemplate = plugin.getConfig().getStringList("gui.filtered-item-lore");
        if (loreTemplate.isEmpty()) loreTemplate = List.of("&8Click to remove from filter.");
        List<String> lore = loreTemplate.stream().map(plugin::color).collect(Collectors.toList());

        return buildItem(mat, plugin.sc("&f" + formatMaterial(material)), lore);
    }

    private Material getMaterial(String configPath, String defaultName) {
        String name = plugin.getConfig().getString(configPath, defaultName);
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException ignored) {
            try { return Material.valueOf(defaultName); }
            catch (IllegalArgumentException ignored2) { return Material.STONE; }
        }
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color(name));
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> colorList(List<String> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        return raw.stream().map(plugin::color).collect(Collectors.toList());
    }

    public static String formatMaterial(String material) {
        String[] words = material.toLowerCase().replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
