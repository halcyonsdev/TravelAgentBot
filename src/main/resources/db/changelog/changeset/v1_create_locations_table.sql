-- =========================================
-- Description: Create the locations table
-- Author: Ruslan Sadikov
-- Version: v1
-- =========================================

CREATE TABLE IF NOT EXISTS locations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    name VARCHAR(500) NOT NULL,
    street VARCHAR(500) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    travel_id BIGINT NOT NULL,
    FOREIGN KEY (travel_id) REFERENCES travels(id)
)