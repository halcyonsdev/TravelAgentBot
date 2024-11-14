-- =========================================
-- Description: Create the travels table
-- Author: Halcyon
-- Version: v1
-- =========================================

CREATE TABLE IF NOT EXISTS travels (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    creator_id BIGINT NOT NULL
)