-- Create app_user table
CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    nickname VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create project table
CREATE TABLE IF NOT EXISTS project (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    storage_size BIGINT DEFAULT 0,
    max_storage_size BIGINT DEFAULT 2147483648,
    owner_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create resource table
CREATE TABLE IF NOT EXISTS resource (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    key VARCHAR(512),
    content_type VARCHAR(255),
    size BIGINT,
    type VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    project_id BIGINT NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES app_user(id) ON DELETE SET NULL
);

-- Create resource_allowed_roles table
CREATE TABLE IF NOT EXISTS resource_allowed_roles (
    resource_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (resource_id, role),
    FOREIGN KEY (resource_id) REFERENCES resource(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_resource_project ON resource(project_id);
CREATE INDEX IF NOT EXISTS idx_resource_status ON resource(status);
CREATE INDEX IF NOT EXISTS idx_resource_key ON resource(key);
CREATE INDEX IF NOT EXISTS idx_allowed_roles_resource ON resource_allowed_roles(resource_id);
CREATE INDEX IF NOT EXISTS idx_user_username ON app_user(username);
