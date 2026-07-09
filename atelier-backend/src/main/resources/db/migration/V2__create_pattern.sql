CREATE TABLE pattern (
                         id UUID PRIMARY KEY,
                         title VARCHAR(255),
                         pdf_path VARCHAR(255),

                         folder_id UUID,

                         CONSTRAINT fk_pattern_folder
                             FOREIGN KEY (folder_id)
                                 REFERENCES folder(id)
                                 ON DELETE CASCADE
);