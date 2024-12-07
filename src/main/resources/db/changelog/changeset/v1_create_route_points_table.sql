-- =========================================
-- Description: Create the route_points table
-- Author: Ruslan Sadikov
-- Version: v1
-- =========================================

CREATE TABLE IF NOT EXISTS route_points (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    route_id BIGINT,
    next_point_id BIGINT,
    FOREIGN KEY (route_id) REFERENCES routes(id),
    FOREIGN KEY (next_point_id) REFERENCES route_points(id)
)