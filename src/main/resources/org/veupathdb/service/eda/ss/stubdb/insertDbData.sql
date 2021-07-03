

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
-- stable_id, parent_stable_id, provider_label, display_name, definition, ordinal_values, display_type, display_order, display_range_min, display_range_max, range_min, range_max, bin_width_override, bin_width_computed, is_temporal, is_featured, is_merge_key, is_repeated, has_values, data_type, distinct_values_count, is_multi_valued, data_shape, unit, precision
insert into AttributeGraph_ds2324_Hshld values ('var_18', null, '_address', 'City', 'The city where they live', null, 'default', 2, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'string', 10, 0, 'categorical', null, null);

insert into Ancestors_ds2324_Hshld values ('101');
insert into AttributeValue_ds2324_Hshld values ('101', 'var_18', null, 'Miami', null);
insert into Ancestors_ds2324_Hshld values ('102');
insert into AttributeValue_ds2324_Hshld values ('102', 'var_18', null, 'Boston', null);

-- household observations
-- stable_id, parent_stable_id, provider_label, display_name, definition, ordinal_values, display_type, display_order, display_range_min, display_range_max, range_min, range_max, bin_width_override, bin_width_computed, is_temporal, is_featured, is_merge_key, is_repeated, has_values, data_type, distinct_values_count, is_multi_valued, data_shape, unit, precision
insert into AttributeGraph_ds2324_HshldObsrvtn values ('var_19', null, '_watersupply', 'Water supply', 'Their water supply', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'string', 10, 0, 'categorical', null, null);

insert into Ancestors_ds2324_HshldObsrvtn values ('301', '101');
insert into AttributeValue_ds2324_HshldObsrvtn values ('301', 'var_19', null, 'piped', null);
insert into Ancestors_ds2324_HshldObsrvtn values ('302', '102');
insert into AttributeValue_ds2324_HshldObsrvtn values ('302', 'var_19', null, 'well', null);

-- participants
-- stable_id, parent_stable_id, provider_label, display_name, definition, ordinal_values, display_type, display_order, display_range_min, display_range_max, range_min, range_max, bin_width_override, bin_width_computed, is_temporal, is_featured, is_merge_key, is_repeated, has_values, data_type, distinct_values_count, is_multi_valued, data_shape, unit, precision
insert into AttributeGraph_ds2324_Prtcpnt values ('var_10', null, '_networth', 'Net worth', 'Their net worth', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'number', 10, 0, 'continuous', 'dollars', 2);
insert into AttributeGraph_ds2324_Prtcpnt values ('var_11', null, '_shoesize', 'Shoe size', 'Their shoe size', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'number', 10, 0, 'categorical', 'size', 1);
insert into AttributeGraph_ds2324_Prtcpnt values ('var_20', null, '_name', 'Name', 'Their name', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'string', 10, 0, 'categorical', null, null);
insert into AttributeGraph_ds2324_Prtcpnt values ('var_17', null, '_haircolor', 'Hair color', 'Their hair color', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'string', 10, 0, 'categorical', null, null);
insert into AttributeGraph_ds2324_Prtcpnt values ('var_18', null, '_earsize', 'Ear size', 'Their ear size', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0,1, 'string', 10, 0,  'categorical', null, null);

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
-- stable_id, parent_stable_id, provider_label, display_name, definition, ordinal_values, display_type, display_order, display_range_min, display_range_max, range_min, range_max, bin_width_override, bin_width_computed, is_temporal, is_featured, is_merge_key, is_repeated, has_values, data_type, distinct_values_count, is_multi_valued, data_shape, unit, precision
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_12', null, '_weight', 'Weight', 'Their weight', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'number', 10, 0, 'continuous', 'pounds', 2);
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_13', null, '_favnumber', 'Favorite number', 'Their favorite number', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'number', 10, 0, 'categorical', null, null);
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_14', null, '_startdate', 'Start date', 'Their start date', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'date', 10, 0, 'continuous', 'date', null);
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_15', null, '_visitdate', 'Visit date', 'Their visit date', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'date', 10, 0, 'continuous', 'date', null);
insert into AttributeGraph_ds2324_PrtcpntObsrvtn values ('var_16', null, '_mood', 'Mood', 'Their mood', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'string', 10, 0, 'categorical', null, null);

-- samples
insert into AttributeGraph_ds2324_Smpl values ('var_22', null, '_density', 'Density', 'Their density', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'string', 10, 0, 'categorical', null, null);

-- treatments
insert into AttributeGraph_ds2324_Trtmnt values ('var_23', null, '_painkiller', 'Pain killer', 'Their pain killer', null, 'default', 1, null, null, null, null, null, null, 0, 0, 0, 0, 1, 'string', 10, 0, 'categorical', null, null);


