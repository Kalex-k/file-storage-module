-- Ensure created_by and updated_by columns exist
ALTER TABLE resource
ADD COLUMN IF NOT EXISTS created_by BIGINT,
ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- Add foreign key constraints if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_resource_created_by'
    ) THEN
        ALTER TABLE resource
        ADD CONSTRAINT fk_resource_created_by 
        FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL;
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_resource_updated_by'
    ) THEN
        ALTER TABLE resource
        ADD CONSTRAINT fk_resource_updated_by 
        FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Update existing records if needed
UPDATE resource 
SET created_by = (SELECT id FROM app_user LIMIT 1)
WHERE created_by IS NULL 
AND EXISTS (SELECT 1 FROM app_user);

-- Make created_by NOT NULL if all records have it
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM resource WHERE created_by IS NULL
    ) THEN
        ALTER TABLE resource
        ALTER COLUMN created_by SET NOT NULL;
    END IF;
END $$;
