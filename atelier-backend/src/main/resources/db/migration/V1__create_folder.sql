CREATE TABLE folder (
                        id UUID PRIMARY KEY,
                        folder_name VARCHAR(255),
                        ref INTEGER,
                        image_path VARCHAR(255),

                        origin VARCHAR(255) NOT NULL,
                        level VARCHAR(255) NOT NULL,
                        garment_type VARCHAR(255),

                        parent_id UUID,

                        CONSTRAINT fk_folder_parent
                            FOREIGN KEY (parent_id)
                                REFERENCES folder(id)
                                ON DELETE CASCADE
);