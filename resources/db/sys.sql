-- :name insert-sys-options-usergroups
-- :command :execute
-- :result :raw
-- :doc Insert into sys.options_usergroups
insert into sys.options_usergroups
  (label, value, location, created_by, updated_by)
values
  (:label, :value, :location, :created_by, :updated_by);


-- :name insert-sys-users
-- :command :execute
-- :result :raw
-- :doc Insert into sys.users
insert into sys.users 
  (username, password, usergroup, created_by, updated_by)
values 
  (:username, :password, :usergroup, :created_by, :updated_by);


-- :name insert-sys-options-types
-- :command :execute
-- :result :raw
-- :doc Insert into sys.options_types
insert into sys.options_types
  (label, value, location, created_by, updated_by)
values
  (:label, :value, :location, :created_by, :updated_by);


-- :name insert-sys-options-controls
-- :command :execute
-- :result :raw
-- :doc Insert into sys.options_controls
insert into sys.options_controls
  (label, value, location, created_by, updated_by)
values
  (:label, :value, :location, :created_by, :updated_by);


-- :name insert-sys-options-foreign-key-queries
-- :command :execute
-- :result :raw
-- :doc Insert into sys.options_foreign_key_queries
insert into sys.options_foreign_key_queries
  (label, value, query, location, created_by, updated_by)
values
  (:label, :value, :query, :location, :created_by, :updated_by);


-- :name insert-sys-options-function-names
-- :command :execute
-- :result :raw
-- :doc Insert into sys.options_function_names
insert into sys.options_function_names
  (value, label, location, created_by, updated_by)
values
  (:value, :label, :location, :created_by, :updated_by);


-- :name insert-val-controls
-- :command :execute
-- :result :raw
-- :doc Insert into val.controls
insert into val.controls 
  (control, created_by, updated_by)
values 
  (:control, :created_by, :updated_by);


-- :name insert-val-types
-- :command :execute
-- :result :raw
-- :doc Insert into val.types
insert into val.types 
  (type, created_by, updated_by)
values
  (:type, :created_by, :updated_by);


-- :name insert-val-usergroups
-- :command :execute
-- :result :raw
-- :doc Insert into val.usergroups
insert into val.usergroups
  (usergroup, created_by, updated_by)
values
  (:usergroup, :created_by, :updated_by);


-- :name insert-sys-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.fields
insert into sys.fields 
  (schema_name, table_name, field_name, 
	type, is_pk, is_pk_in_new, label, control, location, in_table_view, disabled, readonly, required,
  text_max_length,
  boolean_true, boolean_false,
  date_min, date_max,
  foreign_key_query, foreign_key_size,
  integer_step, integer_min, integer_max,
  float_step, float_min, float_max,
  select_multiple, select_size, options_schema_table,
  created_by, updated_by)
values
  (:schema_name, :table_name, :field_name, 
	:type, :is_pk, :is_pk_in_new, :label, :control, :location, :in_table_view, :disabled, :readonly, :required,
  :text_max_length,
  :boolean_true, :boolean_false,
  :date_min::date, :date_max::date,
  :foreign_key_query, :foreign_key_size,
  :integer_step, :integer_min, :integer_max,
  :float_step, :float_min, :float_max,
  :select_multiple, :select_size, :options_schema_table,
  :created_by, :updated_by);
  

-- :name insert-sys-event-classes
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_classes
insert into sys.event_classes
  (event_classes_id, function_name, argument_name_id, description, created_by, updated_by)
values
  (:event_classes_id, :function_name, :argument_name_id, :description, :created_by, :updated_by);


-- :name insert-sys-event-classes-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_classes_fields
insert into sys.event_classes_fields 
  (event_classes_id, fields_id, location, disabled, readonly, required, created_by, updated_by)
values 
  (:event_classes_id, :fields_id, :location, :disabled, :readonly, :required, :created_by, :updated_by);

