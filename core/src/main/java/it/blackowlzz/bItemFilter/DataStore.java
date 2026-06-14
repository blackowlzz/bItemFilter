package it.blackowlzz.bItemFilter;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface DataStore {
    void load(Map<UUID, PlayerFilter> target);
    void savePlayer(PlayerFilter filter);
    void saveAll(Collection<PlayerFilter> filters);
    void close();
}
