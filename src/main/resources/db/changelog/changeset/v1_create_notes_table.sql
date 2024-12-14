-- =========================================
-- Description: Create the notes table
-- Author: Ruslan Sadikov
-- Version: v1
-- =========================================

CREATE TABLE IF NOT EXISTS notes (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    name VARCHAR(100) NOT NULL,
    text VARCHAR(500),
    type VARCHAR(5) NOT NULL,
    file_id TEXT,
    travel_id BIGINT NOT NULL,
    FOREIGN KEY (travel_id) REFERENCES travels(id)
)