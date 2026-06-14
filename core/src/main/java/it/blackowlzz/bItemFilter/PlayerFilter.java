package it.blackowlzz.bItemFilter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerFilter {

    private final UUID playerUuid;
    private boolean enabled;
    private FilterMode mode;
    // LinkedHashSet so the GUI shows items in the order they were added, not random chaos
    private final Set<String> materials = new LinkedHashSet<>();

    public PlayerFilter(UUID playerUuid, boolean enabled, FilterMode mode) {
        this.playerUuid = playerUuid;
        this.enabled    = enabled;
        this.mode       = mode;
    }

    public UUID getPlayerUuid() { return playerUuid; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void toggleEnabled() { this.enabled = !this.enabled; }

    public FilterMode getMode() { return mode; }
    public void setMode(FilterMode mode) { this.mode = mode; }
    public void toggleMode() { this.mode = mode.toggle(); }

    public boolean hasMaterial(String material) {
        return materials.contains(material.toUpperCase());
    }

    public boolean addMaterial(String material) {
        return materials.add(material.toUpperCase());
    }

    public boolean removeMaterial(String material) {
        return materials.remove(material.toUpperCase());
    }

    public void clear() {
        materials.clear();
    }

    public int getItemCount() {
        return materials.size();
    }

    public Set<String> getMaterials() {
        return Collections.unmodifiableSet(materials);
    }

    // the single line that justifies this entire plugin's existence
    public boolean shouldBlock(String material) {
        if (!enabled) return false;
        boolean inList = hasMaterial(material);
        return mode == FilterMode.BLACKLIST ? inList : !inList;
    }
}
