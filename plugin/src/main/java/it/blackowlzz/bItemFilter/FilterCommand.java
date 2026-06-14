package it.blackowlzz.bItemFilter;

import it.blackowlzz.bItemFilter.gui.FilterGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FilterCommand implements CommandExecutor, TabCompleter {

    private final BItemFilter plugin;
    private final FilterManager manager;
    private final FilterGui gui;

    public FilterCommand(BItemFilter plugin, FilterManager manager, FilterGui gui) {
        this.plugin  = plugin;
        this.manager = manager;
        this.gui     = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("bitemfilter.use")) {
            sender.sendMessage(plugin.msg("messages.no-permission", "%prefix% &7You don't have permission."));
            return true;
        }

        if (args.length == 0 || "open".equals(args[0].toLowerCase())) {
            if (sender instanceof Player player) gui.open(player);
            else sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help"   -> sendHelp(sender);
            case "toggle" -> handleToggle(sender);
            case "mode"   -> handleMode(sender);
            case "add"    -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list"   -> handleList(sender);
            case "clear"  -> handleClear(sender);
            case "admin"  -> handleAdmin(sender, args);
            case "reload" -> handleReload(sender);
            default       -> sendHelp(sender);
        }
        return true;
    }

    private void handleToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("messages.player-only", "%prefix% &7This command is player-only."));
            return;
        }
        PlayerFilter filter = manager.getOrCreate(player.getUniqueId());
        filter.toggleEnabled();
        manager.savePlayer(filter);
        String key = filter.isEnabled() ? "messages.filter-enabled" : "messages.filter-disabled";
        String def = filter.isEnabled()
                ? "%prefix% &7Item filtering &aenabled&7."
                : "%prefix% &7Item filtering &cdisabled&7.";
        sender.sendMessage(plugin.msg(key, def));
    }

    private void handleMode(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("messages.player-only", "%prefix% &7This command is player-only."));
            return;
        }
        PlayerFilter filter = manager.getOrCreate(player.getUniqueId());
        filter.toggleMode();
        manager.savePlayer(filter);
        String key = filter.getMode() == FilterMode.BLACKLIST ? "messages.mode-blacklist" : "messages.mode-whitelist";
        String def = filter.getMode() == FilterMode.BLACKLIST
                ? "%prefix% &7Mode set to &cBlacklist &7(listed items blocked)."
                : "%prefix% &7Mode set to &aWhitelist &7(only listed items picked up).";
        sender.sendMessage(plugin.msg(key, def));
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("messages.player-only", "%prefix% &7This command is player-only."));
            return;
        }
        PlayerFilter filter = manager.getOrCreate(player.getUniqueId());

        String materialName;
        if (args.length >= 2) {
            materialName = args[1].toUpperCase();
            try { Material.valueOf(materialName); }
            catch (IllegalArgumentException e) {
                sender.sendMessage(plugin.msg("messages.invalid-material",
                        "%prefix% &7Unknown material: &b%material%").replace("%material%", args[1]));
                return;
            }
        } else {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() == Material.AIR) {
                sender.sendMessage(plugin.msg("messages.item-in-hand",
                        "%prefix% &7Hold an item or specify a material name."));
                return;
            }
            materialName = held.getType().name();
        }

        int max = manager.getMaxItems();
        if (max > 0 && filter.getItemCount() >= max) {
            sender.sendMessage(plugin.msg("messages.max-items-reached",
                    "%prefix% &7Maximum filter size reached &8(%max% items)&7.")
                    .replace("%max%", String.valueOf(max)));
            return;
        }
        if (!filter.addMaterial(materialName)) {
            sender.sendMessage(plugin.msg("messages.item-already-in-filter",
                    "%prefix% &b%material% &7is already in your filter.")
                    .replace("%material%", FilterGui.formatMaterial(materialName)));
            return;
        }
        manager.savePlayer(filter);
        sender.sendMessage(plugin.msg("messages.item-added",
                "%prefix% &7Added &b%material% &7to your filter.")
                .replace("%material%", FilterGui.formatMaterial(materialName)));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("messages.player-only", "%prefix% &7This command is player-only."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("messages.usage-remove",
                    "%prefix% &7Usage: &b/ifilter remove <material>"));
            return;
        }

        String materialName = args[1].toUpperCase();
        PlayerFilter filter = manager.getOrCreate(player.getUniqueId());
        if (!filter.removeMaterial(materialName)) {
            sender.sendMessage(plugin.msg("messages.item-not-in-filter",
                    "%prefix% &b%material% &7is not in your filter.")
                    .replace("%material%", FilterGui.formatMaterial(materialName)));
            return;
        }
        manager.savePlayer(filter);
        sender.sendMessage(plugin.msg("messages.item-removed",
                "%prefix% &7Removed &b%material% &7from your filter.")
                .replace("%material%", FilterGui.formatMaterial(materialName)));
    }

    private void handleList(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("messages.player-only", "%prefix% &7This command is player-only."));
            return;
        }
        PlayerFilter filter = manager.getOrCreate(player.getUniqueId());
        if (filter.getItemCount() == 0) {
            sender.sendMessage(plugin.msg("messages.filter-list-empty", "%prefix% &7Your filter is empty."));
            return;
        }
        sender.sendMessage(plugin.msg("messages.filter-list-header",
                "%prefix% &7Your filtered items &8(%count%)&7:")
                .replace("%count%", String.valueOf(filter.getItemCount())));
        for (String mat : filter.getMaterials()) {
            sender.sendMessage(plugin.msg("messages.filter-list-entry",
                    "  &b+ &7%material%").replace("%material%", FilterGui.formatMaterial(mat)));
        }
    }

    private void handleClear(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("messages.player-only", "%prefix% &7This command is player-only."));
            return;
        }
        PlayerFilter filter = manager.getOrCreate(player.getUniqueId());
        filter.clear();
        manager.savePlayer(filter);
        sender.sendMessage(plugin.msg("messages.filter-cleared", "%prefix% &7Your filter has been cleared."));
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bitemfilter.admin")) {
            sender.sendMessage(plugin.msg("messages.no-permission", "%prefix% &7You don't have permission."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("messages.usage-admin",
                    "%prefix% &7Usage: &b/ifilter admin <clear|list|toggle> <player>"));
            return;
        }

        String action = args[1].toLowerCase();
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.isOnline() && !target.hasPlayedBefore()) {
            sender.sendMessage(plugin.msg("messages.player-not-found", "%prefix% &7Player not found."));
            return;
        }
        String name = target.getName() == null ? args[2] : target.getName();
        PlayerFilter filter = manager.getOrCreate(target.getUniqueId());

        switch (action) {
            case "clear" -> {
                filter.clear();
                manager.savePlayer(filter);
                sender.sendMessage(plugin.color(plugin.getPrefix() + " &7Cleared filter for &b" + name + "&7."));
            }
            case "list" -> {
                if (filter.getItemCount() == 0) {
                    sender.sendMessage(plugin.color(plugin.getPrefix() + " &b" + name + "&7's filter is empty."));
                } else {
                    sender.sendMessage(plugin.color(plugin.getPrefix() + " &b" + name
                            + "&7's filter &8(" + filter.getItemCount() + ")&7:"));
                    for (String mat : filter.getMaterials()) {
                        sender.sendMessage(plugin.color("  &b+ &7" + FilterGui.formatMaterial(mat)));
                    }
                }
            }
            case "toggle" -> {
                filter.toggleEnabled();
                manager.savePlayer(filter);
                sender.sendMessage(plugin.color(plugin.getPrefix() + " &b" + name + "&7's filter: "
                        + (filter.isEnabled() ? "&aenabled" : "&cdisabled") + "&7."));
            }
            default -> sender.sendMessage(plugin.msg("messages.usage-admin",
                    "%prefix% &7Usage: &b/ifilter admin <clear|list|toggle> <player>"));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("bitemfilter.admin")) {
            sender.sendMessage(plugin.msg("messages.no-permission", "%prefix% &7You don't have permission."));
            return;
        }
        plugin.reloadConfig();
        manager.save();
        manager.load();
        sender.sendMessage(plugin.color(plugin.getPrefix() + " &7Config reloaded."));
    }

    private void sendHelp(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String prefix  = plugin.getPrefix();

        List<String> playerLines = plugin.getConfig().getStringList("help.player");
        if (playerLines.isEmpty()) {
            playerLines = List.of(
                    "",
                    "%prefix% &7v%version%",
                    "",
                    "  &b/ifilter &8| &b/ifilter open  &7- Open the filter GUI",
                    "  &b/ifilter toggle         &7- Enable or disable your filter",
                    "  &b/ifilter mode           &7- Switch between blacklist and whitelist",
                    "  &b/ifilter add [material] &7- Add held item or a named material",
                    "  &b/ifilter remove <mat>   &7- Remove an item from your filter",
                    "  &b/ifilter list           &7- List all items in your filter",
                    "  &b/ifilter clear          &7- Clear your entire filter",
                    "  &b/ifilter help           &7- Show this page",
                    ""
            );
        }
        playerLines.stream()
                .map(l -> l.replace("%prefix%", prefix).replace("%version%", version))
                .forEach(l -> sender.sendMessage(plugin.color(l)));

        if (sender.hasPermission("bitemfilter.admin")) {
            List<String> adminLines = plugin.getConfig().getStringList("help.admin");
            if (adminLines.isEmpty()) {
                adminLines = List.of(
                        "  &7Admin &8(bitemfilter.admin)&7:",
                        "  &b/ifilter admin clear <player>  &7- Clear a player's filter",
                        "  &b/ifilter admin list <player>   &7- View a player's filter",
                        "  &b/ifilter admin toggle <player> &7- Toggle a player's filter",
                        "  &b/ifilter reload                &7- Reload config and data",
                        ""
                );
            }
            adminLines.stream()
                    .map(l -> l.replace("%prefix%", prefix).replace("%version%", version))
                    .forEach(l -> sender.sendMessage(plugin.color(l)));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(List.of("open", "help", "toggle", "mode", "add", "remove", "list", "clear"));
            if (sender.hasPermission("bitemfilter.admin")) base.addAll(List.of("admin", "reload"));
            return base;
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "remove" -> {
                    if (!(sender instanceof Player player)) yield Collections.emptyList();
                    PlayerFilter f = manager.get(player.getUniqueId());
                    yield f == null ? Collections.emptyList() : new ArrayList<>(f.getMaterials());
                }
                case "add" -> {
                    String partial = args[1].toUpperCase();
                    yield Arrays.stream(Material.values())
                            .map(Material::name)
                            .filter(n -> n.startsWith(partial))
                            .limit(20)
                            .collect(java.util.stream.Collectors.toList());
                }
                case "admin" -> List.of("clear", "list", "toggle");
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3 && "admin".equals(args[0].toLowerCase())) {
            List<String> online = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> online.add(p.getName()));
            return online;
        }
        return Collections.emptyList();
    }
}
