ALTER TABLE ewyp_reports
ALTER COLUMN scoring_classification
    TYPE double precision
    USING scoring_classification::double precision;
