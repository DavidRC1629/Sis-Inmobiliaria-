ALTER TABLE proformas
    ADD COLUMN pdf_data LONGBLOB NULL,
    ADD COLUMN pdf_file_name VARCHAR(255) NULL,
    ADD COLUMN pdf_content_type VARCHAR(120) NULL;
