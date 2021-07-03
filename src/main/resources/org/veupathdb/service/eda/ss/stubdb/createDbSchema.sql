
-- use oracle syntax throughout execution
SET DATABASE SQL SYNTAX ORA TRUE;

-- the EDA service doesn't need to know much about the Study, because the WDK will serve that data
-- the abbrev would be used in the name of the tall and ancestors tables
-- the study_id is a stable ID
create table Study (
  stable_id varchar(50) not null,
  display_name varchar(30) not null,
  internal_abbrev varchar(30),
  PRIMARY KEY (stable_id)
);
alter table Study add unique (display_name);
alter table Study add unique (internal_abbrev);;

create table StudyIdDatasetId (
  study_stable_id varchar(50) not null,
  dataset_id varchar(50) not null,
)

-- The entities types per study, eg Participants, and their tree relationships
CREATE TABLE EntityTypeGraph (
  stable_id varchar(50) not null,
  study_stable_id varchar(50) not null,
  parent_stable_id varchar(30),
  internal_abbrev varchar(20),
  display_name varchar(30) not null,
  display_name_plural varchar(30),
  description varchar(100),
);
alter table EntityTypeGraph add unique (study_stable_id, stable_id);
alter table EntityTypeGraph add unique (study_stable_id, internal_abbrev);
alter table EntityTypeGraph add unique (display_name, study_stable_id, stable_id);
ALTER TABLE EntityTypeGraph
   ADD FOREIGN KEY (study_stable_id) REFERENCES Study (stable_id);

--------------------------------------------------------------------------------------------------
-- the following tables are per-study.  Their name is formed using the study ID, in this case ds2324.
-- Each entity type gets two tables:
--    AttributeValue_XXXXX_YYYYY
--    Ancestors_XXXXX_YYYYY
--  where XXXXX is the study internal_abbrev and YYYYY is the entity's internal_abbrev
--------------------------------------------------------------------------------------------------
create table AttributeGraph_ds2324_Hshld (
STABLE_ID                      varchar(255) not null,
PARENT_STABLE_ID               varchar(255),
PROVIDER_LABEL                 varchar(1500),
DISPLAY_NAME                   varchar(1500),
DEFINITION                     varchar(4000),
VOCABULARY                     varchar(1500),
DISPLAY_TYPE                   varchar(20),
DISPLAY_ORDER                  integer,
DISPLAY_RANGE_MIN              varchar(16),
DISPLAY_RANGE_MAX              varchar(16),
RANGE_MIN                      varchar(128),
RANGE_MAX                      varchar(128),
BIN_WIDTH_OVERRIDE             varchar(16),
BIN_WIDTH_COMPUTED             varchar(128),
IS_TEMPORAL                    integer,
IS_FEATURED                    integer,
IS_MERGE_KEY                   integer,
IS_REPEATED                    integer,
HAS_VALUES                     integer,
DATA_TYPE                      varchar(10),
DISTINCT_VALUES_COUNT          integer,
IS_MULTI_VALUED                integer,
DATA_SHAPE                     varchar(30),
UNIT                           varchar(30),
PRECISION                      integer,
PRIMARY KEY (stable_id)
);

create table AttributeValue_ds2324_Hshld (
  hshld_stable_id varchar(20),
  attribute_stable_id varchar(30),
  number_value integer, 
  string_value varchar(100),
  date_value varchar(30),
);
ALTER TABLE AttributeValue_ds2324_Hshld
   ADD FOREIGN KEY (attribute_stable_id) REFERENCES AttributeGraph_ds2324_Hshld (stable_id);
CREATE INDEX AttributeValue_ds2324_Hshld_i1
ON AttributeValue_ds2324_Hshld (attribute_stable_id, hshld_stable_id, number_value, string_value, date_value);
-- for test db only
CREATE unique INDEX AttributeValue_ds2324_Hshld_i2 ON AttributeValue_ds2324_Hshld (attribute_stable_id, hshld_stable_id);

create table Ancestors_ds2324_Hshld (
  hshld_stable_id varchar(30),
  PRIMARY KEY (Hshld_stable_id)
);

-----------------------------------------------------------------------------
create table AttributeGraph_ds2324_HshldObsrvtn (
STABLE_ID                      varchar(255) not null,
PARENT_STABLE_ID               varchar(255),
PROVIDER_LABEL                 varchar(1500),
DISPLAY_NAME                   varchar(1500),
DEFINITION                     varchar(4000),
VOCABULARY                     varchar(1500),
DISPLAY_TYPE                   varchar(20),
DISPLAY_ORDER                  integer,
DISPLAY_RANGE_MIN              varchar(16),
DISPLAY_RANGE_MAX              varchar(16),
RANGE_MIN                      varchar(128),
RANGE_MAX                      varchar(128),
BIN_WIDTH_OVERRIDE             varchar(16),
BIN_WIDTH_COMPUTED             varchar(128),
IS_TEMPORAL                    integer,
IS_FEATURED                    integer,
IS_MERGE_KEY                   integer,
IS_REPEATED                    integer,
HAS_VALUES                     integer,
DATA_TYPE                      varchar(10),
DISTINCT_VALUES_COUNT          integer,
IS_MULTI_VALUED                integer,
DATA_SHAPE                     varchar(30),
UNIT                           varchar(30),
PRECISION                      integer,
PRIMARY KEY (stable_id)
);

create table AttributeValue_ds2324_HshldObsrvtn (
  HshldObsrvtn_stable_id varchar(30),
  attribute_stable_id varchar(30),
  number_value integer, 
  string_value varchar(100),
  date_value varchar(30),
);
ALTER TABLE AttributeValue_ds2324_HshldObsrvtn
   ADD FOREIGN KEY (attribute_stable_id) REFERENCES AttributeGraph_ds2324_HshldObsrvtn (stable_id);
CREATE INDEX AttributeValue_ds2324_HshldObsrvtn_i1
ON AttributeValue_ds2324_HshldObsrvtn (attribute_stable_id, HshldObsrvtn_stable_id, number_value, string_value, date_value);
-- for test db only
CREATE unique INDEX AttributeValue_ds2324_HshldObsrvtn_i2 ON AttributeValue_ds2324_HshldObsrvtn (attribute_stable_id, HshldObsrvtn_stable_id);

create table Ancestors_ds2324_HshldObsrvtn (
  HshldObsrvtn_stable_id varchar(30),
  Hshld_stable_id varchar(30),
  PRIMARY KEY (Hshld_stable_id)
);

-----------------------------------------------------------------------------
create table AttributeGraph_ds2324_Prtcpnt (
STABLE_ID                      varchar(255) not null,
PARENT_STABLE_ID               varchar(255),
PROVIDER_LABEL                 varchar(1500),
DISPLAY_NAME                   varchar(1500),
DEFINITION                     varchar(4000),
VOCABULARY                     varchar(1500),
DISPLAY_TYPE                   varchar(20),
DISPLAY_ORDER                  integer,
DISPLAY_RANGE_MIN              varchar(16),
DISPLAY_RANGE_MAX              varchar(16),
RANGE_MIN                      varchar(128),
RANGE_MAX                      varchar(128),
BIN_WIDTH_OVERRIDE             varchar(16),
BIN_WIDTH_COMPUTED             varchar(128),
IS_TEMPORAL                    integer,
IS_FEATURED                    integer,
IS_MERGE_KEY                   integer,
IS_REPEATED                    integer,
HAS_VALUES                     integer,
DATA_TYPE                      varchar(10),
DISTINCT_VALUES_COUNT          integer,
IS_MULTI_VALUED                integer,
DATA_SHAPE                     varchar(30),
UNIT                           varchar(30),
PRECISION                      integer,
PRIMARY KEY (stable_id)
);
create table AttributeValue_ds2324_Prtcpnt (
  prtcpnt_stable_id varchar(30),
  attribute_stable_id varchar(30),
  number_value integer,
  string_value varchar(100),
  date_value varchar(30),
);
ALTER TABLE AttributeValue_ds2324_Prtcpnt
   ADD FOREIGN KEY (attribute_stable_id) REFERENCES AttributeGraph_ds2324_Prtcpnt (stable_id);
CREATE INDEX AttributeValue_ds2324_Prtcpnt_i1
ON AttributeValue_ds2324_Prtcpnt (attribute_stable_id, prtcpnt_stable_id, number_value, string_value, date_value);
-- for test db only
CREATE unique INDEX AttributeValue_ds2324_Prtcpnt_i2 ON AttributeValue_ds2324_Prtcpnt (attribute_stable_id, prtcpnt_stable_id);

create table Ancestors_ds2324_Prtcpnt (
  prtcpnt_stable_id varchar(30),
  hshld_stable_id varchar(30),
  PRIMARY KEY (prtcpnt_stable_id)
);

-----------------------------------------------------------------------------
create table AttributeGraph_ds2324_PrtcpntObsrvtn (
STABLE_ID                      varchar(255) not null,
PARENT_STABLE_ID               varchar(255),
PROVIDER_LABEL                 varchar(1500),
DISPLAY_NAME                   varchar(1500),
DEFINITION                     varchar(4000),
VOCABULARY                     varchar(1500),
DISPLAY_TYPE                   varchar(20),
DISPLAY_ORDER                  integer,
DISPLAY_RANGE_MIN              varchar(16),
DISPLAY_RANGE_MAX              varchar(16),
RANGE_MIN                      varchar(128),
RANGE_MAX                      varchar(128),
BIN_WIDTH_OVERRIDE             varchar(16),
BIN_WIDTH_COMPUTED             varchar(128),
IS_TEMPORAL                    integer,
IS_FEATURED                    integer,
IS_MERGE_KEY                   integer,
IS_REPEATED                    integer,
HAS_VALUES                     integer,
DATA_TYPE                      varchar(10),
DISTINCT_VALUES_COUNT          integer,
IS_MULTI_VALUED                integer,
DATA_SHAPE                     varchar(30),
UNIT                           varchar(30),
PRECISION                      integer,
PRIMARY KEY (stable_id)
);

create table AttributeValue_ds2324_PrtcpntObsrvtn (
  PrtcpntObsrvtn_stable_id varchar(30),
  attribute_stable_id varchar(30),
  number_value integer, 
  string_value varchar(100),
  date_value varchar(30),
);
ALTER TABLE AttributeValue_ds2324_PrtcpntObsrvtn
   ADD FOREIGN KEY (attribute_stable_id) REFERENCES AttributeGraph_ds2324_PrtcpntObsrvtn (stable_id);
CREATE INDEX AttributeValue_ds2324_PrtcpntObsrvtn_i1
ON AttributeValue_ds2324_PrtcpntObsrvtn (attribute_stable_id, PrtcpntObsrvtn_stable_id, number_value, string_value, date_value);
-- for test db only
CREATE unique INDEX AttributeValue_ds2324_PrtcpntObsrvtn_i2 ON AttributeValue_ds2324_PrtcpntObsrvtn (attribute_stable_id, PrtcpntObsrvtn_stable_id);

create table Ancestors_ds2324_PrtcpntObsrvtn (
  PrtcpntObsrvtn_stable_id varchar(30),
  Prtcpnt_stable_id varchar(30),
  Hshld_stable_id varchar(30),
  PRIMARY KEY (PrtcpntObsrvtn_stable_id)
);

create table AttributeGraph_ds2324_Smpl (
STABLE_ID                      varchar(255) not null,
PARENT_STABLE_ID               varchar(255),
PROVIDER_LABEL                 varchar(1500),
DISPLAY_NAME                   varchar(1500),
DEFINITION                     varchar(4000),
VOCABULARY                     varchar(1500),
DISPLAY_TYPE                   varchar(20),
DISPLAY_ORDER                  integer,
DISPLAY_RANGE_MIN              varchar(16),
DISPLAY_RANGE_MAX              varchar(16),
RANGE_MIN                      varchar(128),
RANGE_MAX                      varchar(128),
BIN_WIDTH_OVERRIDE             varchar(16),
BIN_WIDTH_COMPUTED             varchar(128),
IS_TEMPORAL                    integer,
IS_FEATURED                    integer,
IS_MERGE_KEY                   integer,
IS_REPEATED                    integer,
HAS_VALUES                     integer,
DATA_TYPE                      varchar(10),
DISTINCT_VALUES_COUNT          integer,
IS_MULTI_VALUED                integer,
DATA_SHAPE                     varchar(30),
UNIT                           varchar(30),
PRECISION                      integer,
PRIMARY KEY (stable_id)
);

create table AttributeGraph_ds2324_Trtmnt (
STABLE_ID                      varchar(255) not null,
PARENT_STABLE_ID               varchar(255),
PROVIDER_LABEL                 varchar(1500),
DISPLAY_NAME                   varchar(1500),
DEFINITION                     varchar(4000),
VOCABULARY                     varchar(1500),
DISPLAY_TYPE                   varchar(20),
DISPLAY_ORDER                  integer,
DISPLAY_RANGE_MIN              varchar(16),
DISPLAY_RANGE_MAX              varchar(16),
RANGE_MIN                      varchar(128),
RANGE_MAX                      varchar(128),
BIN_WIDTH_OVERRIDE             varchar(16),
BIN_WIDTH_COMPUTED             varchar(128),
IS_TEMPORAL                    integer,
IS_FEATURED                    integer,
IS_MERGE_KEY                   integer,
IS_REPEATED                    integer,
HAS_VALUES                     integer,
DATA_TYPE                      varchar(10),
DISTINCT_VALUES_COUNT          integer,
IS_MULTI_VALUED                integer,
DATA_SHAPE                     varchar(30),
UNIT                           varchar(30),
PRECISION                      integer,
PRIMARY KEY (stable_id)
);





