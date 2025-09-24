-- Migration: Remove is_all_day column from tasks table
-- Date: 2025-09-24
-- Description: Removes the is_all_day boolean column from tasks table as the feature has been removed from the application

-- Remove is_all_day column from tasks table
ALTER TABLE tasks DROP COLUMN IF EXISTS is_all_day;