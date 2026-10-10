ALTER TABLE household
    ADD COLUMN household_type ENUM('SINGLE', 'FAMILY')
    NOT NULL DEFAULT 'SINGLE';

UPDATE household h
JOIN (
    SELECT household_id
    FROM household_member
    GROUP BY household_id
    HAVING COUNT(*) >= 2
) m ON m.household_id = h.id
SET h.household_type = 'FAMILY';
