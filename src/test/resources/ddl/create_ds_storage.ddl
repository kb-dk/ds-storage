 CREATE TABLE ds_records (
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

CREATE UNIQUE INDEX i ON ds_records(id);
CREATE UNIQUE INDEX m ON ds_records(mtime);
CREATE INDEX b ON ds_records(base);
CREATE INDEX bd ON ds_records(base,deleted);
CREATE INDEX p ON ds_records(parentId);
