 CREATE TABLE IF NOT EXISTS ds_records (
id VARCHAR(255) PRIMARY KEY,
origin VARCHAR(31), 
orgid VARCHAR(255),
id_error INTEGER,
deleted INTEGER,  
data TEXT,
ctime BIGINT,
mtime BIGINT,
parentid VARCHAR(255),
recordtype VARCHAR(31),
kalturareferenceid VARCHAR(255),
kalturainternalid VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS i ON ds_records(id);
CREATE UNIQUE INDEX IF NOT EXISTS m ON ds_records(mtime);
CREATE INDEX IF NOT EXISTS b ON ds_records(origin);
CREATE INDEX IF NOT EXISTS bd ON ds_records(origin,deleted);
CREATE INDEX IF NOT EXISTS p ON ds_records(parentid);
CREATE INDEX IF NOT EXISTS rt ON ds_records(recordtype);
CREATE INDEX IF NOT EXISTS om ON ds_records (origin, mtime);
CREATE INDEX IF NOT EXISTS orm ON ds_records (origin, recordtype, mtime);
CREATE UNIQUE INDEX IF NOT EXISTS kref ON ds_records (kalturareferenceid);