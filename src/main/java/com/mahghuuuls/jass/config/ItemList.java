package com.mahghuuuls.jass.config;

import java.util.List;

/** A parsed per-item configuration list, for load warnings and unknown-item checks. */
public interface ItemList {

    /** Registry names mentioned by the list. */
    List<String> registryNames();

    /** Registry names that have an entry for one specific metadata value. */
    List<String> namesWithMetadataEntries();

    /** One warning per skipped line. */
    List<String> warnings();
}
