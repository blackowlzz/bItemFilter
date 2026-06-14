package it.blackowlzz.bItemFilter;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

// for servers still running pre-1.12. yes, they exist. yes, it's fine.
@SuppressWarnings("deprecation")
public final class LegacyFilterListener implements Listener {

    private final FilterManager manager;

    public LegacyFilterListener(FilterManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        PlayerFilter filter = manager.get(player.getUniqueId());
        if (filter == null) return;
        String material = event.getItem().getItemStack().getType().name();
        if (filter.shouldBlock(material)) {
            event.setCancelled(true);
        }
    }
}
