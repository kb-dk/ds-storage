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