-- :name insert-sys-users
-- :command :execute
-- :result :raw
-- :doc Insert into sys.users
insert into sys.users 
(username, password, usertype, created_by, updated_by) values 
('root',  'rootpw',  'superuser',     'root', 'root'),
('djneu', 'djneupw', 'administrator', 'root', 'root');


-- :name insert-val-usertypes
-- :command :execute
-- :result :raw
-- :doc Insert into val.usertypes
insert into val.usertypes
(usertype, created_by, updated_by) values 
('superuser',     'root', 'root'),
('administrator', 'root', 'root'),
('manager',       'root', 'root'),
('coordinator',   'root', 'root');


-- :name insert-val-types
-- :command :execute
-- :result :raw
-- :doc Insert into val.types
insert into val.types
(type, created_by, updated_by) values 
('boolean',     'root', 'root'),
('date',        'root', 'root'),
('float8',      'root', 'root'),
('int8',        'root', 'root'),
('serial8',     'root', 'root'),
('text',        'root', 'root'),
('timestamptz', 'root', 'root'),
('uuid',        'root', 'root');


-- :name insert-val-controls
-- :command :execute
-- :result :raw
-- :doc Insert into val.controls
insert into val.controls
(control, created_by, updated_by) values
('date',      'root', 'root'),
('datetime',  'root', 'root'),
('float',     'root', 'root'),
('integer',   'root', 'root'),
('select',    'root', 'root'),
('text',      'root', 'root'),
('textarea',  'root', 'root'),
('timestamp', 'root', 'root'),
('yes-no',    'root', 'root');


-- :name insert-sys-fields-sys-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.fields for sys.fields
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'id', 'serial8', 'true', 'ID', 'integer', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'schema_name', 'text', 'false', 'Schema Name', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'table_name', 'text', 'false', 'Table Name', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'field_name', 'text', 'false', 'Field Name', 'text', 3, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'type', 'text', 'false', 'Type', 'select', 4, 'true', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'is_pk', 'boolean', 'false', 'Is Primary Key?', 'yes-no', 5, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'label', 'text', 'false', 'Label', 'text', 6, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'control', 'text', 'false', 'Control', 'select', 7, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, integer_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'position', 'int8', 'false', 'Position', 'integer', 8, 'false',  1, 0, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'in_table_view', 'boolean', 'false', 'In Table View?', 'yes-no', 9, 'false', 'false', 'false', 'true', 'root', 'root');
--   -- -- format text, <----- maybe use cl-format
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, integer_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'text_max_length', 'int8', 'false', 'Text Max Length', 'integer', 10, 'false',  1, 1, 'false', 'false', 'false','root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'date_min', 'date', 'false', 'Date Min', 'date', 11, 'false', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'date_max', 'date', 'false', 'Date Max', 'date', 12, 'false', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'integer_step', 'int8', 'false', 'Integer Step', 'integer', 13, 'false',  1, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'integer_min', 'int8', 'false', 'Integer Min', 'integer', 14, 'false',  1, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'integer_max', 'int8', 'false', 'Integer Max', 'integer', 15, 'false',  1, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, float_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'float_step', 'float8', 'false', 'Float Step', 'float', 16, 'false', 0.0000000001, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, float_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'float_min', 'float8', 'false', 'Float Min', 'float', 17, 'false',  -1E-307, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, float_max, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'float_max', 'float8', 'false', 'Float Max', 'float', 18, 'false',  1E+308, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'select_multiple', 'boolean', 'false', 'Select Multiple?', 'yes-no', 19, 'false', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, integer_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'select_size', 'int8', 'false', 'Select Size', 'integer', 20, 'false', 1, 0, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'disabled', 'boolean', 'false', 'Disabled?', 'yes-no', 21, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'readonly', 'boolean', 'false', 'Readonly?', 'yes-no', 22, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'required', 'boolean', 'false', 'Required?', 'yes-no', 23, 'false', 'false', 'false', 'true', 'root', 'root');


-- :name insert-sys-fields-study-subjects
-- :command :execute
-- :result :raw
-- :doc Insert into sys.fields for study.subjects
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'id', 'serial8', 'true', 'ID', 'integer', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'first_name', 'text', 'false', 'First Name', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'last_name', 'text', 'false', 'Last Name', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, date_min, date_max, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'birth_date', 'date', 'false', 'Birth Date', 'date', 3, 'true', '1700-01-01', '2025-12-31', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'birth_state', 'text', 'false', 'Birth State', 'select', 4, 'false', 'false', 'false', 'false', 'root', 'root');


-- :name insert-sys-fields-val-controls
-- :command :execute
-- :result :raw
-- :doc Insert into sys.fields for val.controls
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('val', 'controls', 'id', 'uuid', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('val', 'controls', 'control', 'text', 'false', 'Control', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');


-- :name insert-sys-fields-val-types
-- :command :execute
-- :result :raw
-- :doc Insert into sys.fields for val.types
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('val', 'types', 'id', 'uuid', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('val', 'types', 'type', 'text', 'false', 'Type', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');


-- :name insert-sys-fields-sys-users
-- :command :execute
-- :result :raw
-- :doc Insert into sys.fields for sys.users
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'id', 'uuid', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'username', 'text', 'false', 'Username', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'password', 'text', 'false', 'Password', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'usertype', 'text', 'false', 'Usertype', 'select', 3, 'true', 'false', 'false', 'true', 'root', 'root');


-- :name insert-sys-event-classes
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_classes
insert into sys.event_classes 
(name, description, created_by, updated_by) values 
('Register subject', 'Register a subject in the study.', 'root', 'root');


-- :name insert-sys-event-classes-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_classes_fields
insert into sys.event_classes_fields 
(event_classes_id, fields_id, position, created_by, updated_by) values 
(1, 1, 1, 'root', 'root'),
(1, 2, 2, 'root', 'root'),
(1, 3, 3, 'root', 'root'),
(1, 4, 4, 'root', 'root');


-- :name insert-sys-select-options 
-- :command :execute
-- :result :raw
-- :doc Insert into sys.select_options
insert into sys.select_options 
(schema_name, table_name, field_name, label, text_value, position, created_by, updated_by) values 
('sys', 'users', 'usertype', '',              '',              0, 'root', 'root'),
('sys', 'users', 'usertype', 'Administrator', 'administrator', 3, 'root', 'root'),
('sys', 'users', 'usertype', 'Manager',        'manager',      2, 'root', 'root'),
('sys', 'users', 'usertype', 'Superuser',      'superuser',    4, 'root', 'root'),
('sys', 'users', 'usertype', 'Coordinator',    'coordinator',  1, 'root', 'root'),

('sys', 'fields', 'is_pk', '',    '',      0, 'root', 'root'),
('sys', 'fields', 'is_pk', 'Yes', 'true',  1, 'root', 'root'),
('sys', 'fields', 'is_pk', 'No',  'false', 2, 'root', 'root'),

('sys', 'fields', 'in_table_view', '',    '',      0, 'root', 'root'),
('sys', 'fields', 'in_table_view', 'Yes', 'true',  1, 'root', 'root'),
('sys', 'fields', 'in_table_view', 'No',  'false', 2, 'root', 'root'),

('sys', 'fields', 'type', '',               '',            0, 'root', 'root'),
('sys', 'fields', 'type', 'Boolean',        'boolean',     1, 'root', 'root'),
('sys', 'fields', 'type', 'Date',           'date',        2, 'root', 'root'),
('sys', 'fields', 'type', 'Floating point', 'float8',      3, 'root', 'root'),
('sys', 'fields', 'type', 'Integer',        'int8',        4, 'root', 'root'),
('sys', 'fields', 'type', 'ID',             'serial8',     5, 'root', 'root'),
('sys', 'fields', 'type', 'Text',           'text',        6, 'root', 'root'),
('sys', 'fields', 'type', 'Timestamp',      'timestamptz', 7, 'root', 'root'),
('sys', 'fields', 'type', 'UUID',           'uuid',        8, 'root', 'root'),

('sys', 'fields', 'control', '',         '',           0, 'root', 'root'),
('sys', 'fields', 'control', 'Date',     'date',       1, 'root', 'root'),
('sys', 'fields', 'control', 'Float',    'float',      2, 'root', 'root'),
('sys', 'fields', 'control', 'Integer',  'integer',    3, 'root', 'root'),
('sys', 'fields', 'control', 'Select',   'select',     4, 'root', 'root'),
('sys', 'fields', 'control', 'Text',     'text',       5, 'root', 'root'),
('sys', 'fields', 'control', 'Textarea', 'textarea',   6, 'root', 'root'),
('sys', 'fields', 'control', 'Yes/No',   'yes-no',     7, 'root', 'root');

