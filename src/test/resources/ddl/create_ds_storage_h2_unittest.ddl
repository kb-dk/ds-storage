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
    referenceid VARCHAR(255),
    kalturaid VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS i ON ds_records(id);
CREATE UNIQUE INDEX IF NOT EXISTS m ON ds_records(mtime);
CREATE INDEX IF NOT EXISTS b ON ds_records(origin);
CREATE INDEX IF NOT EXISTS bd ON ds_records(origin,deleted);
CREATE INDEX IF NOT EXISTS p ON ds_records(parentid);
CREATE INDEX IF NOT EXISTS rt ON ds_records(recordtype);
CREATE INDEX IF NOT EXISTS om ON ds_records (origin, mtime);
CREATE INDEX IF NOT EXISTS orm ON ds_records (origin, recordtype, mtime);
CREATE INDEX IF NOT EXISTS kref ON ds_records (referenceid);
CREATE INDEX IF NOT EXISTS kalid ON ds_records (kalturaid);


CREATE TABLE IF NOT EXISTS transcriptions ( 
    fileid VARCHAR(255) PRIMARY KEY,
    filename VARCHAR(255),
    mtime BIGINT,
    transcription TEXT,
    transcription_lines TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS fileid_trans ON transcriptions(fileid);
CREATE UNIQUE INDEX IF NOT EXISTS m_trans ON transcriptions(mtime);

CREATE TABLE IF NOT EXISTS rerun_clusters (
    id SERIAL PRIMARY KEY, -- OR id UUID PRIMARY KEY DEFAULT gen_random_uuid()
    file_id UUID UNIQUE NOT NULL,
    rerun_cluster_id UUID NOT NULL,
    cluster_id_creation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_time TIMESTAMP WITH TIME ZONE NOT NULL, -- auditing
    modified_time TIMESTAMP WITH TIME ZONE NOT NULL -- auditing
);

CREATE INDEX IF NOT EXISTS rerun_clusters_file_id_idx ON rerun_clusters(file_id);
CREATE INDEX IF NOT EXISTS rerun_clusters_rerun_cluster_id_idx ON rerun_clusters(rerun_cluster_id);
