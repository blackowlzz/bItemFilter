package it.blackowlzz.bItemFilter;

public enum FilterMode {
    BLACKLIST("Blacklist"),
    WHITELIST("Whitelist");

    private final String displayName;

    FilterMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // philosophical question: are we the blacklist or are we the whitelist?
    public FilterMode toggle() {
        return this == BLACKLIST ? WHITELIST : BLACKLIST;
    }
}
