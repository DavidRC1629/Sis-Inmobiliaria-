ALTER TABLE projects ADD COLUMN logo_url LONGTEXT NULL;

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(120) NOT NULL PRIMARY KEY,
    setting_value LONGTEXT NULL
);
