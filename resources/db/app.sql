-- :name select-study-subjects-all
-- :command :query
-- :result many
-- :doc select all the study.subjectss
select *
from 
study.subjects


-- :name select-study-subjects-by-last-name
-- :command :query
-- :result many
-- :doc select all the study.subject where last_name=:last_name
select *
from study.subjects
where last_name = :last_name


-- :name insert-app-select-options
-- :command :execute
-- :result :raw
-- :doc Insert into app.select_options
insert into app.select_options 
  (schema_name, table_name, field_name, label, text_value, location, created_by, updated_by)
values
  (:schema_name, :table_name, :field_name, :label, :text_value, :location, :created_by, :updated_by);


-- :name insert-app-options-states
-- :command :execute
-- :result :raw
-- :doc Insert into app.options_states
insert into app.options_states
  (label, value, location, created_by, updated_by)
values
  (:label, :value, :location, :created_by, :updated_by);


-- :name insert-study-subjects
-- :command :execute
-- :result :raw
-- :doc Insert some study.subjects
insert into study.subjects
  (first_name, last_name, birth_date, birth_state, created_by, updated_by) 
values 
  (:first_name, :last_name, :birth_date::date, :birth_state, :created_by, :updated_by)


-- :name insert-app-notes
-- :command :execute
-- :result :raw
-- :doc Insert into app.notes
insert into app.notes
  (subjects_id, note, created_by, updated_by)
 values
  (:subjects_id, :note, :created_by, :updated_by);

