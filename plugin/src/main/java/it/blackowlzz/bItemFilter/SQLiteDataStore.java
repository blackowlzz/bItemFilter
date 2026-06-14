package it.blackowlzz.bItemFilter;

import java.io.File;
import java.sql.*;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class SQLiteDataStore implements DataStore {

    private final File dbFile;
    private final Logger logger;
    private final FilterMode defaultMode;
    private final boolean defaultEnabled;
    private Connection connection;

    public SQLiteDataStore(File dataFolder, Logger logger, boolean defaultEnabled, FilterMode defaultMode) {
        this.dbFile         = new File(dataFolder, "filters.db");
        this.logger         = logger;
        this.defaultEnabled = defaultEnabled;
        this.defaultMode    = defaultMode;
    }


    public boolean initialize() {
        dbFile.getParentFile().mkdirs();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS filters (" +
                    "  uuid    TEXT PRIMARY KEY," +
                    "  enabled INTEGER NOT NULL DEFAULT 1," +
                    "  mode    TEXT    NOT NULL DEFAULT 'BLACKLIST'" +
                    ")"
                );
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS filter_items (" +
                    "  uuid     TEXT NOT NULL," +
                    "  material TEXT NOT NULL," +
                    "  PRIMARY KEY (uuid, material)" +
                    ")"
                );
            }
            logger.info("SQLite database initialized: " + dbFile.getName());
            return true;
        } catch (Exception e) {
            logger.severe("Failed to initialize SQLite database: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void load(Map<UUID, PlayerFilter> target) {
        if (connection == null) return;
        String sql =
            "SELECT f.uuid, f.enabled, f.mode, i.material " +
            "FROM filters f " +
            "LEFT JOIN filter_items i ON f.uuid = i.uuid " +
            "ORDER BY f.uuid";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            UUID current = null;
            PlayerFilter currentFilter = null;
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                if (!uuid.equals(current)) {
                    current = uuid;
                    boolean enabled = rs.getInt("enabled") == 1;
                    FilterMode mode;
                    try { mode = FilterMode.valueOf(rs.getString("mode").toUpperCase()); }
                    catch (IllegalArgumentException ignored) { mode = defaultMode; }
                    currentFilter = new PlayerFilter(uuid, enabled, mode);
                    target.put(uuid, currentFilter);
                }
                String material = rs.getString("material");
                if (material != null && currentFilter != null) {
                    currentFilter.addMaterial(material);
                }
            }
        } catch (SQLException e) {
            // how did you manage to do this, seriously, did you hack into the sqlite dataset and mess with the tables? (also if this happens please send me the error message and tell me how you destroyed everything so i can fix it, thanks!)
            logger.warning("Failed to load filters from SQLite: " + e.getMessage());
        }
    }

    @Override
    public void savePlayer(PlayerFilter filter) {
        if (connection == null) return;
        // PreparedStatement with ? params: no string concat, no injection possible.
        // materials come from Material.valueOf() so they're already enum-safe. sleep well.
        String uuid = filter.getPlayerUuid().toString();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO filters (uuid, enabled, mode) VALUES (?, ?, ?)")) {
                ps.setString(1, uuid);
                ps.setInt(2, filter.isEnabled() ? 1 : 0);
                ps.setString(3, filter.getMode().name());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM filter_items WHERE uuid = ?")) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            }
            if (!filter.getMaterials().isEmpty()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO filter_items (uuid, material) VALUES (?, ?)")) {
                    for (String mat : filter.getMaterials()) {
                        ps.setString(1, uuid);
                        ps.setString(2, mat);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            logger.warning("Failed to save filter for " + uuid + ": " + e.getMessage());
            try { connection.rollback(); } catch (SQLException ignored) {}
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public void saveAll(Collection<PlayerFilter> filters) {
        for (PlayerFilter filter : filters) savePlayer(filter);
    }

    @Override
    public void close() {
        if (connection != null) {
            try { connection.close(); }
            catch (SQLException e) { logger.warning("Failed to close SQLite connection: " + e.getMessage()); }
        }
    }
}
