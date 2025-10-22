package com.privatecal.dto;

/**
 * Strategy for handling duplicate events during CalDAV import
 */
public enum DuplicateStrategy {
    /**
     * Skip duplicate events - import only new events
     */
    SKIP,

    /**
     * Update existing events with new data from import
     */
    UPDATE,

    /**
     * Create new events anyway - allows duplicates
     */
    CREATE_ANYWAY
}
