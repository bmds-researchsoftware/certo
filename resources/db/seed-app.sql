-- :name insert-app-select-options
-- :command :execute
-- :result :raw
-- :doc Insert into app.select_options
insert into app.select_options
(schema_name, table_name, field_name, label, text_value, position, created_by, updated_by) values 
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


-- :name insert-study-subjects
-- :command :execute
-- :result :raw
-- :doc Insert into study.subjects
insert into study.subjects
(first_name, last_name, birth_date, birth_state, created_by, updated_by) values 
('Martha',   'Washington', '1731-06-13', 'VA', 'djneu', 'djneu'),
('Abigail',  'Adams',      '1744-11-22', null, 'djneu', 'djneu'),
('Martha',   'Jefferson',  '1748-10-30', null, 'djneu', 'djneu'),
('Betsy',    'Ross',       '1752-01-01', null, 'djneu', 'djneu'),
('Dolly',    'Madison',    '1768-05-20', null, 'djneu', 'djneu'),
('Elizabeth','Monroe',     '1768-06-30', null, 'djneu', 'djneu'),
('Jackie',   'Kennedy',    '1929-07-28', null, 'djneu', 'djneu');


-- :name insert-study-notes
-- :command :execute
-- :result :raw
-- :doc Insert into study.notes
insert into study.notes 
(subjects_id, note, created_by, updated_by) values 
(2, 'Abigail Adams was married to the Second President of the United States.', 'djneu', 'djneu');

