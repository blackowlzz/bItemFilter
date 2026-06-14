bItemFilter is a **per-player item pickup filter** for Spigot and Paper servers. Each player manages their own independent filter — what one player blocks doesn't affect anyone else.

---

# Download on Modrinth!

[![Modrinth](https://img.shields.io/badge/Download-Modrinth-green)](https://modrinth.com/plugin/bitemfilter)

---

## Discord / Support

Join the community for support and updates.

[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?style=flat&logo=discord)](https://discord.gg/ZxBc4NvAnt)

---

## How it works

Two modes, pick your poison:

**Blacklist** — items on the list stay on the ground. Everything else is picked up normally. Good for blocking junk like rotten flesh or gravel.

**Whitelist** — only items on the list get picked up. Everything else is ignored. Good for hardcore grinders who only want specific drops.

Players can switch modes, toggle the filter on/off without losing their list, and manage everything from a GUI or commands. Filters are saved to SQLite and survive restarts.

---

## GUI

Open with `/ifilter`. Everything is clickable:

| Slot | What it does |
|------|-------------|
| Rows 1-4 | Items currently in your filter. Click any to remove it. |
| Your inventory | Click any item you're holding to add or remove it from the filter — no commands needed. |
| Slot 47 | **Mode** button — switch between Blacklist and Whitelist. |
| Slot 49 | **Toggle** button — enable or disable the filter without clearing it. |
| Slot 51 | **Clear** button — shift-click to wipe the list. |
| Slots 45, 53 | **Arrows** — page navigation for more than 36 entries. |

On **1.20.5+** the GUI title and button names render in Unicode small caps.

---

## Commands

All aliases work the same: `/bitemfilter`, `/itemfilter`, `/ifilter`, `/filter`.

| Command | What it does |
|---------|-------------|
| `/ifilter` | Open the GUI |
| `/ifilter toggle` | Enable or disable your filter |
| `/ifilter mode` | Switch blacklist / whitelist |
| `/ifilter add [material]` | Add the item in your hand, or name one |
| `/ifilter remove <material>` | Remove an item from the filter |
| `/ifilter list` | List your filtered items in chat |
| `/ifilter clear` | Clear the entire filter |
| `/ifilter help` | Show command reference |

### Admin commands

Require `bitemfilter.admin`.

| Command | What it does |
|---------|-------------|
| `/ifilter admin clear <player>` | Clear another player's filter |
| `/ifilter admin list <player>` | View another player's filter |
| `/ifilter admin toggle <player>` | Toggle another player's filter |
| `/ifilter reload` | Reload config from disk |

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `bitemfilter.use` | everyone | Use the filter and personal commands |
| `bitemfilter.admin` | op | Admin commands and update notifications |

---

## Storage

Player data is stored in **SQLite** (`plugins/bItemFilter/filters.db`) with WAL mode. No YAML files to corrupt, no full rewrites on every save — each player's data is saved atomically only when it changes.

---

## Configuration

Everything in `config.yml` is configurable: all messages support `&` color codes and a `%prefix%` placeholder, every GUI button's material and lore can be changed, and the help sections are editable lists. The default mode, default enabled state, and per-player item cap are set globally.

---

## Compatibility

- Minecraft **1.8+** — uses `PlayerPickupItemEvent` on pre-1.12, `EntityPickupItemEvent` on 1.12+, detected automatically at startup
- Spigot and Paper
- Does not conflict with other pickup or economy plugins — cancels at `NORMAL` priority and respects already-cancelled events

---

## License

bItemFilter is free and open source under the **GNU General Public License v3.0**.
