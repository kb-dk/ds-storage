 CREATE TABLE ds_records (
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
referenceid VARCHAR(255),
kalturaid VARCHAR(255)
);


 CREATE TABLE ds_mapping (
 referenceid VARCHAR(255) PRIMARY KEY,
 kalturaid VARCHAR(255)
);

CREATE UNIQUE INDEX i ON ds_records(id);
CREATE UNIQUE INDEX m ON ds_records(mtime);
CREATE INDEX b ON ds_records(origin);
CREATE INDEX bd ON ds_records(origin,deleted);
CREATE INDEX p ON ds_records(parentid);
CREATE INDEX rt ON ds_records(recordtype);
CREATE INDEX om ON ds_records (origin, mtime);
CREATE INDEX orm ON ds_records (origin, recordtype, mtime);
CREATE INDEX kref ON ds_records (referenceid);
CREATE INDEX kalid ON ds_records (kalturaid);

CREATE UNIQUE INDEX mapping_i ON ds_mapping(referenceid);