package com.example.iptv;

import java.util.Set;

/** What an import / database update should bring in (see {@link getFromInternet}). */
public class ImportOptions {

    /** Which channels to keep, based on their streams. */
    public enum ServerFilter {
        /** Every channel, even without a stream URL */
        ALL,
        /** Only channels that have at least one stream URL */
        WITH_SERVERS,
        /** Only channels with at least one stream that answers right now (tested during import) */
        WORKING
    }

    /** Import a single ISO country ("LB"), or null. */
    public String countryCode;
    /** Restrict to these ISO countries (e.g. those already in the database), or null for all. */
    public Set<String> countryCodes;
    /** Cap per country; 0 or less = no cap. */
    public int maxPerCountry = -1;
    public ServerFilter serverFilter = ServerFilter.WITH_SERVERS;
    /** Before importing channels, add categories and countries that are missing locally. */
    public boolean syncCategoriesAndCountries;
    /** Replace logos that changed at the source (only logos that came from an import). */
    public boolean updateLogos = true;

    /** What changed, for the summary shown when the task finishes. */
    public static class Result {
        public int newChannels, updatedChannels, newServers, logosUpdated, newCategories, newCountries;

        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append(newChannels).append(" new channels, ").append(newServers).append(" new servers");
            if (updatedChannels > 0) sb.append(", ").append(updatedChannels).append(" channels updated");
            if (logosUpdated > 0) sb.append(", ").append(logosUpdated).append(" logos updated");
            if (newCategories > 0) sb.append(", ").append(newCategories).append(" new categories");
            if (newCountries > 0) sb.append(", ").append(newCountries).append(" new countries");
            return sb.toString();
        }
    }
}
