

-- ============================================================
--    CHANGES TO THIS FILE WILL BREAK UNIT TESTS
-- ============================================================


------------------------------------------------------------
-- STUDY METADATA TABLES
------------------------------------------------------------

--(stable_id, display_nme, internal_abbrev)
insert into study values ('DS-2324', 'GEMS', 'ds2324');

insert into StudyIdDatasetId values('DS-2324', 'datasetid_2222');

--(stable_id, study_stable_id, parent_stable_id, internal_abbrev, display_name, display_name_plural, description)
insert into entityTypeGraph values ('GEMS_House', 'DS-2324', null, 'Hshld', 'Household', 'Households', 'Households in the study');
insert into entityTypeGraph values ('GEMS_HouseObs', 'DS-2324', 'GEMS_House', 'HshldObsrvtn', 'Household Observation', 'Household Observations', 'Observations about households in the study');
insert into entityTypeGraph values ('GEMS_Part','DS-2324', 'GEMS_House', 'Prtcpnt', 'Participant', 'Participants', 'Participants in the study');
insert into entityTypeGraph values ('GEMS_PartObs', 'DS-2324', 'GEMS_Part', 'PrtcpntObsrvtn', 'Participant Observation', 'Participant Observations', 'Participant observations');
insert into entityTypeGraph values ('GEMS_Sample', 'DS-2324', 'GEMS_PartObs', 'Smpl', 'Sample', 'Samples', 'Sample');
insert into entityTypeGraph values ('GEMS_Treat', 'DS-2324', 'GEMS_PartObs', 'Trtmnt', 'Treatment', 'Treatments', 'Treatment');

------------------------------------------------------------
-- STUDY DATA TABLES
------------------------------------------------------------

-- households
insert into AttributeGraph_ds2324_Hshld values ('var_18', 200, null, '_address', 'City', 'default', 1, 'string', 0, 'categorical', null, null, null);

insert into Ancestors_ds2324_Hshld values ('101');
insert into AttributeValue_ds2324_Hshld values ('101', 'var_18', null, 'Miami', null);
insert into Ancestors_ds2324_Hshld values ('102');
insert into AttributeValue_ds2324_Hshld values ('102', 'var_18', null, 'Boston', null);

-- household observations
insert into AttributeGraph_ds2324_HshldObsrvtn values ('var_19', 200, null, '_watersupply', 'Water supply', 'default', 1, 'string', 0, 'categorical', null, null, null);

insert into Ancestors_ds2324_HshldObsrvtn values ('301', '101');
insert into AttributeValue_ds2324_HshldObsrvtn values ('301', 'var_19', null, 'piped', null);
insert into Ancestors_ds2324_HshldObsrvtn values ('302', '102');
insert into AttributeValue_ds2324_HshldObsrvtn values ('302', 'var_19', null, 'well', null);

-- participants

--(stable_id, ontology_term_id, parent_stable_id, provider_label, display_name, term_type, has_value, data_type, has_multiple_values_per_entity, data_shape, unit, unit_ontology_term_id, precision)
insert into AttributeGraph_ds2324_Prtcpnt values ('var_10', 300, null, '_networth', 'Net worth', 'default', 1, 'number', 0, 'continuous', 'dollars', null, 2);
insert into AttributeGraph_ds2324_Prtcpnt values ('var_11', 300, null, '_shoesize', 'Shoe size', 'default', 1, 'number', 0, 'categorical', 'size', null, 1);
insert into AttributeGraph_ds2324_Prtcpnt values ('var_20', 200, null, '_name', 'Name', 'default', 1, 'string', 0, 'categorical', null, null, null);
insert into AttributeGraph_ds2324_Prtcpnt values ('var_17', 200, null, '_haircolor', 'Hair color', 'default', 1, 'string', 0,  'categorical', null, null, null);
insert into AttributeGraph_ds2324_Prtcpnt values ('var_18', 200, null, '_earsize', 'Ear size', 'default', 1, 'string', 0,  'categorical', null, null, null);

insert into Ancestors_ds2324_Prtcpnt values ('201', '101');
insert into AttributeValue_ds2324_Prtcpnt values ('201', 'var_20', null, 'Martin', null);
insert into AttributeValue_ds2324_Prtcpnt values ('201', 'var_11', 11.5, null, null);
insert into AttributeValue_ds2324_Prtcpnt values ('201', 'var_17', null, 'blond', null);
insert into AttributeValue_ds2324_Prtcpnt values ('201', 'var_18', null, 'small', null);

insert into Ancestors_ds2324_Prtcpnt values ('202', '101');
insert into AttributeValue_ds2324_Prtcpnt values ('202', 'var_20', null, 'Abe', null);
insert into AttributeValue_ds2324_Prtcpnt values ('202', 'var_11', 10, null, null);
insert into AttributeValue_ds2324_Prtcpnt values ('202', 'var_17', null, 'blond', null);
insert into AttributeValue_ds2324_Prtcpnt values ('202', 'var_18', null, 'medium', null);

insert into Ancestors_ds2324_Prtcpnt values ('203', '102');
insert into AttributeValue_ds2324_Prtcpnt values ('203', 'var_20', null, 'Gladys', null);
insert into AttributeValue_ds2324_Prtcpnt values ('203', 'var_11', 11.5, null, null);
insert into AttributeValue_ds2324_Prtcpnt values ('203', 'var_17', null, 'brown', null);
insert into AttributeValue_ds2324_Prtcpnt values ('203', 'var_18', null, 'large', null);

insert into Ancestors_ds2324_Prtcpnt values ('204', '102');
insert into AttributeValue_ds2324_Prtcpnt values ('204', 'var_20', null, 'Susan', null);
insert into AttributeValue_ds2324_Prtcpnt values ('204', 'var_11', 10, null, null);
insert into AttributeValue_ds2324_Prtcpnt values ('204', 'var_17', null, 'silver', null);
-- intentionally omit var_18 to test left join logic

-- participant observations

--(stable_id, ontology_term_id, parent_stable_id, provider_label, display_name, term_type, has_value, data_type, has_multiple_values_per_entity, data_shape, unit, unit_ontology_term_id, precision)
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_12', 300, null, '_weight', 'Weight', 'default', 1, 'number', 0, 'continuous', 'pounds', null, 2);
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_13', 300, null, '_favnumber', 'Favorite number', 'default',  1, 'number', 0, 'categorical', null, null, null);
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_14', 400, null, '_startdate', 'Start date', 'default',  1, 'date', 0, 'continuous', 'date', null, null);
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_15', 300, null, '_visitdate', 'Visit date', 'default',  1, 'date', 0, 'continuous', 'date', null, null);
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_16', 200, null, '_mood', 'Mood', 'default', 1, 'string', 0, 'categorical', null, null, null);

-- samples
insert into AttributeGraph_ds2324_Smpl values ('var_22', 200, null, '_density', 'Density', 'default', 1, 'string', 0, 'categorical', null, null, null);

-- treatments
insert into AttributeGraph_ds2324_Trtmnt values ('var_23', 200, null, '_painkiller', 'Pain killer', 'default', 1, 'string', 0, 'categorical', null, null, null);


