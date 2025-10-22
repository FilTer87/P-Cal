# Calendar Import & Export

## Overview

Import and export your calendar data using the standard iCalendar (.ics) format. This allows you to migrate events from other calendar applications (Google Calendar, Apple Calendar, Outlook, etc.) or create backups of your data.

## Exporting Your Calendar

### How to Export

1. Go to **Profile** → **Calendar Data**
2. Click the **"Export Calendar"** button
3. A `.ics` file will be downloaded to your device

### What Gets Exported

The exported file includes:
- All your tasks and events (both timed and all-day)
- Event details (title, description, location, color)
- Start and end dates/times
- Recurrence rules (RRULE) for repeating events
- Reminders and notifications

### File Format

- Format: iCalendar (.ics) - RFC 5545 compliant
- Components: VEVENT (standard calendar events)
- Encoding: UTF-8
- Compatible with all major calendar applications

## Importing Calendars

### How to Import

1. Go to **Profile** → **Calendar Data**
2. Click the **"Upload .ics"** button
3. Select an `.ics` or `.ical` file from your device
4. Wait for the analysis to complete

### Import Preview

When you upload a file, the system automatically analyzes it for duplicates:

**If no duplicates are found:**
- Events are imported immediately
- You'll see a success message with the count of imported events

**If duplicates are detected:**
- A preview modal appears showing:
  - Number of new events
  - Number of duplicate events
  - Number of errors (if any)
- You can expand the duplicates list to see which events already exist
- Events marked as "Modified" have different content compared to existing ones

### Duplicate Handling Strategies

When duplicates are found, choose one of three strategies:

**Skip Duplicates (Default - Recommended):**
- Imports only new events
- Existing events remain unchanged
- Safe option for re-importing the same file

**Update Existing:**
- Overwrites existing events with data from the imported file
- Use when you've modified events externally and want to sync changes
- **Warning:** Local changes to those events will be lost

**Create Anyway:**
- Creates new events even if duplicates exist
- Useful for intentionally duplicating events
- New events receive a different unique identifier to avoid conflicts

### Duplicate Detection

Duplicates are identified using **UID (Unique Identifier)**:
- Each event in a properly formatted .ics file has a unique UID
- When importing, the system checks if events with the same UID already exist
- This prevents accidental duplication when re-importing the same calendar

**Note:** If you import a calendar for the first time, then export and re-import it, all events will be correctly recognized as duplicates on the second import.

### Supported Event Types

**VEVENT (Events):**
- Standard calendar events with start and end times
- All-day events (VALUE=DATE format)
- Recurring events with RRULE

**VTODO (To-Do Items):**
- Tasks with due dates
- Converted to calendar events with "[TODO]" prefix in title
- If the TODO has a time component, it's imported as a timed event
- If the TODO has only a date (no time), it's imported as an all-day event
- TODOs without a due date are imported as all-day events for the current day

### Import Validation

The system validates:
- File format (.ics or .ical extension required)
- File structure (must be valid iCalendar format)
- Field sizes (titles max 100 chars, descriptions max 2500 chars)
- Date/time validity

**Field Truncation:**
- If description exceeds 2500 characters, it's automatically truncated
- A warning is logged, but the import continues
- The import result will show as "partial success" (yellow indicator)

### Import Results

After import, you'll see a summary:

**Full Success (Green):**
- All events imported successfully
- No errors or warnings

**Partial Success (Yellow):**
- Some events imported, some failed
- Check the error list for details
- Common causes: oversized fields (automatically truncated), invalid dates

**Error (Red):**
- Import failed completely
- Error message explains the cause
- Check file format and try again

## Compatibility

### Tested Applications

✓ Google Calendar
✓ Apple Calendar (macOS/iOS)
✓ Microsoft Outlook
✓ Mozilla Thunderbird
✓ Any CalDAV-compliant application

### Format Support

- **iCalendar Version:** 2.0 (RFC 5545)
- **Character Encoding:** UTF-8
- **Maximum Description Length:** 2500 characters (auto-truncated)
- **Maximum Title Length:** 100 characters
- **Maximum Location Length:** 200 characters

## Best Practices

### For Exporting

1. **Regular Backups:** Export your calendar periodically as a backup
2. **Before Major Changes:** Create an export before making bulk modifications
3. **Migration:** Use export to move data between calendar applications

### For Importing

1. **Test with Small Files:** When importing from a new source, test with a small calendar first
2. **Review Duplicates:** Always review the duplicate preview before confirming
3. **Use Skip by Default:** When re-importing, use "Skip Duplicates" to avoid creating duplicates
4. **Check Results:** Review the import summary to ensure all events were processed correctly

## Troubleshooting

**Issue:** Import shows all events as errors
- **Solution:** Ensure the file is a valid .ics format; try opening it in another calendar app first

**Issue:** All events appear as duplicates on first import
- **Solution:** This is expected if you've already imported the same file before; use "Skip Duplicates"

**Issue:** Some events missing after import
- **Solution:** Check the import results for errors; some events may have failed validation

**Issue:** Descriptions are cut off
- **Solution:** Original descriptions over 2500 characters are truncated; this is logged as a warning

**Issue:** Wrong timezone for events
- **Solution:** Ensure your browser timezone matches your preferred timezone; all-day events are timezone-independent

## Privacy & Data

- Import/export happens entirely through your browser
- Files are not stored on the server after processing
- Only the parsed event data is saved to your account
- Export generates data on-demand, no pre-generated files are stored
