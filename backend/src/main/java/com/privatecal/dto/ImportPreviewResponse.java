package com.privatecal.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response containing preview information for a CalDAV import
 */
public class ImportPreviewResponse {
    private int totalEvents;
    private int newEvents;
    private int duplicateEvents;
    private int errorEvents;
    private List<DuplicateEventInfo> duplicates;

    public ImportPreviewResponse() {
        this.duplicates = new ArrayList<>();
    }

    // Getters and Setters
    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public int getNewEvents() {
        return newEvents;
    }

    public void setNewEvents(int newEvents) {
        this.newEvents = newEvents;
    }

    public int getDuplicateEvents() {
        return duplicateEvents;
    }

    public void setDuplicateEvents(int duplicateEvents) {
        this.duplicateEvents = duplicateEvents;
    }

    public int getErrorEvents() {
        return errorEvents;
    }

    public void setErrorEvents(int errorEvents) {
        this.errorEvents = errorEvents;
    }

    public List<DuplicateEventInfo> getDuplicates() {
        return duplicates;
    }

    public void setDuplicates(List<DuplicateEventInfo> duplicates) {
        this.duplicates = duplicates;
    }

    /**
     * Information about a duplicate event
     */
    public static class DuplicateEventInfo {
        private String uid;
        private String title;
        private String existingDate;
        private String newDate;
        private boolean contentChanged;

        // Getters and Setters
        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getExistingDate() {
            return existingDate;
        }

        public void setExistingDate(String existingDate) {
            this.existingDate = existingDate;
        }

        public String getNewDate() {
            return newDate;
        }

        public void setNewDate(String newDate) {
            this.newDate = newDate;
        }

        public boolean isContentChanged() {
            return contentChanged;
        }

        public void setContentChanged(boolean contentChanged) {
            this.contentChanged = contentChanged;
        }
    }
}
