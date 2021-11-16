CREATE TABLE ds_records (
id VARCHAR(255) PRIMARY KEY,
base VARCHAR(31), 
deleted INTEGER, 
indexable INTEGER, 
data TEXT,
ctime BIGINT,
mtime BIGINT,
parentId VARCHAR(255)
);

CREATE UNIQUE INDEX i ON ds_records(id);
CREATE UNIQUE INDEX m ON ds_records(mtime);
CREATE INDEX b ON ds_records(base);
CREATE INDEX bdi ON ds_records(base,deleted,indexable);
CREATE INDEX p ON ds_records(parentId);
