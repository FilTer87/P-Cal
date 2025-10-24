# All-Day Events

## Overview

All-day events are tasks that span entire days without specific start and end times, such as birthdays, holidays, or deadlines.

## Creating All-Day Events

1. Click on a day in the calendar to open the task creation form
2. Enable the **"All-Day"** toggle switch
3. Fill in the event details (title, description, color, etc.)
4. Save the event

When the "All-Day" toggle is enabled:
- Time fields are hidden
- The event is stored as spanning the entire day (00:00 to 23:59 in UTC)
- Start and end dates must be on the same day (single-day events only)

## Viewing All-Day Events

### Week View

All-day events appear in a dedicated banner above the hourly time grid.

**Single Event:**
- Click the event to view or edit details
- Displays with the event's assigned color

**Multiple Events:**
- When multiple all-day events exist on the same day, a badge shows the count (e.g., "3")
- Click the badge to expand a list of all events
- Click individual events in the list to view/edit
- Click outside or on the badge again to collapse the list

### Month View

All-day events display at the top of each day cell.

## Import/Export Compatibility

All-day events are compatible with the iCalendar (.ics) format:

**Import:**
- Events with `DTSTART;VALUE=DATE` format are recognized as all-day
- Events with date-only values (no time component) become all-day events

**Export:**
- All-day events are exported using the `VALUE=DATE` format
- Compatible with Google Calendar, Apple Calendar, Outlook, and other CalDAV applications

## Converting Events

**To All-Day:**
1. Edit an existing event
2. Enable the "All-Day" toggle
3. Save - times are removed, only the date remains

**To Timed Event:**
1. Edit an all-day event
2. Disable the "All-Day" toggle
3. Set specific start and end times
4. Save

## Recurring All-Day Events

All-day events support recurrence patterns (RRULE):
- Daily, weekly, monthly, or yearly repetition
- Recurrence rules are preserved during import/export
- Each occurrence is treated as an all-day event

## Reminders

All-day events can have reminders with customizable timing:
- Set the exact time when you want to be notified (e.g., 9:00 AM on the event day)
- Multiple reminders can be added to a single event
- Reminder times are independent of the all-day event itself
