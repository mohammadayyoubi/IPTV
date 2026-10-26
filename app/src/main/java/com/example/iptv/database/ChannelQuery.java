package com.example.iptv.database;

/**
 * What a channel list should show. Translated to SQL by {@link ChannelDAO#queryPage} so that
 * searching and filtering work on the database, not on lists held in memory.
 */
public class ChannelQuery {

    public enum Availability {
        ALL,
        /** At least one server that isn't known to be broken */
        WATCHABLE,
        /** Has servers, all of them broken */
        NOT_WORKING,
        /** No servers at all */
        NO_SERVERS,
        /** Watched before, most recent first */
        RECENT
    }

    public String search = "";
    public Availability availability = Availability.ALL;
    public int countryId;     // 0 = any
    public int categoryId;    // 0 = any
    public boolean favoritesOnly;
    /** Channels of this country come first (the viewer's own country); 0 = plain A-Z. */
    public int priorityCountryId;

    public ChannelQuery copy() {
        ChannelQuery q = new ChannelQuery();
        q.search = search;
        q.availability = availability;
        q.countryId = countryId;
        q.categoryId = categoryId;
        q.favoritesOnly = favoritesOnly;
        q.priorityCountryId = priorityCountryId;
        return q;
    }

    /** Counts per availability for the same search / country / category / favorites. */
    public static class Counts {
        public int watchable, notWorking, noServers, all, recent;

        public int of(Availability availability) {
            switch (availability) {
                case WATCHABLE: return watchable;
                case NOT_WORKING: return notWorking;
                case NO_SERVERS: return noServers;
                case RECENT: return recent;
                default: return all;
            }
        }
    }
}
