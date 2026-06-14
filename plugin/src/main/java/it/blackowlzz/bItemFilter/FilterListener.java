package it.blackowlzz.bItemFilter;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

// modern listener for 1.12+, see LegacyFilterListener for the dinosaur version
public final class FilterListener implements Listener {

    private final FilterManager manager;

    public FilterListener(FilterManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerFilter filter = manager.get(player.getUniqueId());
        if (filter == null) return;
        String material = event.getItem().getItemStack().getType().name();
        if (filter.shouldBlock(material)) {
            event.setCancelled(true);
        }
    }
}
