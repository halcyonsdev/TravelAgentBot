-- =========================================
-- Description: Create the routes table
-- Author: Ruslan Sadikov
-- Version: v2
-- =========================================

CREATE TABLE IF NOT EXISTS routes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    api_map_url VARCHAR(10000) NOT NULL,
    size INT NOT NULL,
    start_point_id BIGINT,
    destination_point_id BIGINT,
    travel_id BIGINT NOT NULL,
    FOREIGN KEY (travel_id) REFERENCES travels(id)
)