 CREATE TABLE IF NOT EXISTS ds_records (
id VARCHAR(255) PRIMARY KEY,
base VARCHAR(31), 
orgid VARCHAR(255),
id_error INTEGER,
deleted INTEGER,  
data TEXT,
ctime BIGINT,
mtime BIGINT,
parentId VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS i ON ds_records(id);
CREATE UNIQUE INDEX IF NOT EXISTS m ON ds_records(mtime);
CREATE INDEX IF NOT EXISTS b ON ds_records(base);
CREATE INDEX IF NOT EXISTS bd ON ds_records(base,deleted);
CREATE INDEX IF NOT EXISTS p ON ds_records(parentId);
