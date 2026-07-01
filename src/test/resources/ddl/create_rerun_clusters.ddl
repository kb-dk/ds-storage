CREATE TABLE rerun_clusters (
    id UUID PRIMARY KEY,
    file_id UUID UNIQUE NOT NULL,
    rerun_cluster_id UUID NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL,
    job_id CHARACTER VARYING NOT NULL,
    inserted TIMESTAMP WITH TIME ZONE NOT NULL,
    updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX rerun_clusters_file_id_idx ON rerun_clusters(file_id);
CREATE INDEX rerun_clusters_rerun_cluster_id_idx ON rerun_clusters(rerun_cluster_id);
CREATE INDEX rerun_clusters_created_idx ON rerun_clusters(created);

COMMENT ON TABLE rerun_clusters IS 'Table of rerun clusters data';
COMMENT ON COLUMN rerun_clusters.id IS 'Unique UUID id';
COMMENT ON COLUMN rerun_clusters.file_id IS 'Filename without extension from Preservica. Is the same as presentation copy from Preservica and a field in Solr';
COMMENT ON COLUMN rerun_clusters.rerun_cluster_id IS 'UUID id of a rerun cluster. Multiple file_id can share the same rerun_cluster_id';
COMMENT ON COLUMN rerun_clusters.created IS 'Timestamp of execution of rerun clusters matching job';
COMMENT ON COLUMN rerun_clusters.job_id IS 'Id of rerun clusters matching job. Only useful for logging and debugging';
COMMENT ON COLUMN rerun_clusters.inserted IS 'When did team Arkivblik insert the row from the foreign data wrapper table';
COMMENT ON COLUMN rerun_clusters.updated IS 'When did team Arkivblik update the row from the foreign data wrapper table';
