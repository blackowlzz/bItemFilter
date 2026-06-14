package it.blackowlzz.bItemFilter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FilterManager {

    // ConcurrentHashMap because multiple async tasks exist and I'd rather not find out what happens without it
    private final Map<UUID, PlayerFilter> filters = new ConcurrentHashMap<>();
    private final DataStore dataStore;

    private boolean defaultEnabled = true;
    private FilterMode defaultMode = FilterMode.BLACKLIST;
    private int maxItems = 0;

    public FilterManager(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void setDefaults(boolean defaultEnabled, FilterMode defaultMode, int maxItems) {
        this.defaultEnabled = defaultEnabled;
        this.defaultMode    = defaultMode;
        this.maxItems       = maxItems;
    }

    public synchronized void load() {
        filters.clear();
        dataStore.load(filters);
    }

    public void savePlayer(PlayerFilter filter) {
        dataStore.savePlayer(filter);
    }

    public synchronized void save() {
        dataStore.saveAll(filters.values());
    }

    public void close() {
        dataStore.close();
    }

    public PlayerFilter getOrCreate(UUID uuid) {
        // if the player doesn't have a filter yet, make one with the server defaults. welcome to the club.
        return filters.computeIfAbsent(uuid,
                id -> new PlayerFilter(id, defaultEnabled, defaultMode));
    }

    public PlayerFilter get(UUID uuid) {
        return filters.get(uuid);
    }

    public int getMaxItems() { return maxItems; }
}
