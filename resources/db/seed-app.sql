-- :name insert-sys-fields-study-subjects
-- :command :execute
-- :result :raw
-- :doc Insert into sys.fields for study.subjects
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'id', 'serial8', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'first_name', 'text', 'false', 'First Name', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'last_name', 'text', 'false', 'Last Name', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, date_min, date_max, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'birth_date', 'date', 'false', 'Birth Date', 'date', 3, 'true', '1700-01-01', '2025-12-31', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, select_multiple, select_size, created_by, updated_by) values ('study', 'subjects', 'birth_state', 'text', 'false', 'Birth State', 'select', 4, 'false', 'false', 'false', 'false', 'false', 1, 'root', 'root');


-- :name seed-insert-app-select-options
-- :command :execute
-- :result :raw
-- :doc Insert into app.select_options
insert into app.select_options 
(schema_name, table_name, field_name, label, text_value, location, created_by, updated_by) values 
('study', 'subjects', 'birth_state', '',               '',   0,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Connecticut',    'CT', 1,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Delaware',       'DE', 2,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Georgia',        'GA', 3,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Maryland',       'MD', 4,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Massachusetts',  'MA', 5,  'root', 'root'),
('study', 'subjects', 'birth_state', 'New Hampshire',  'NH', 6,  'root', 'root'),
('study', 'subjects', 'birth_state', 'New Jersey',     'NJ', 7,  'root', 'root'),
('study', 'subjects', 'birth_state', 'New York',       'NY', 8,  'root', 'root'),
('study', 'subjects', 'birth_state', 'North Carolina', 'NC', 9,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Pennsylvania',   'PA', 10, 'root', 'root'),
('study', 'subjects', 'birth_state', 'Rhode Island',   'RI', 11, 'root', 'root'),
('study', 'subjects', 'birth_state', 'South Carolina', 'SC', 12, 'root', 'root'),
('study', 'subjects', 'birth_state', 'Virginia',       'VA', 13, 'root', 'root');


-- :name seed-insert-study-subjects
-- :command :execute
-- :result :raw
-- :doc Insert into study.subjects
insert into study.subjects 
(first_name, last_name, birth_date, created_by, updated_by) values 
('Martha',   'Washington', '1731-06-13', 'djneu', 'djneu'),
('Abigail',  'Adams',      '1744-11-22', 'djneu', 'djneu'),
('Martha',   'Jefferson',  '1748-10-30', 'djneu', 'djneu'),
('Betsy',    'Ross',       '1752-01-01', 'djneu', 'djneu'),
('Dolly',    'Madison',    '1768-05-20', 'djneu', 'djneu'),
('Elizabeth','Monroe',     '1768-06-30', 'djneu', 'djneu'),
('Jackie',   'Kennedy',    '1929-07-28', 'djneu', 'djneu');


-- :name seed-insert-app-notes
-- :command :execute
-- :result :raw
-- :doc Insert into app.notes
insert into app.notes
(subjects_id, note, created_by, updated_by) values
(2, 'Abigail Adams was married to the Second President of the United States.', 'djneu', 'djneu');

