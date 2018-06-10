-- :name insert-sys-options-usergroups
-- :command :execute
-- :result :raw
-- :doc Insert into sys.options_usergroups
insert into sys.options_usergroups
  (value, label, location, created_by, updated_by)
values
  (:value, :label, :location, :created_by, :updated_by);


-- :name insert-sys-users
-- :command :execute
-- :result :raw
-- :doc Insert into sys.users
insert into sys.users 
  (username, password, full_name, display_name, email, usergroup, created_by, updated_by)
values 
  (:username, :password, :full_name, :display_name, :email, :usergroup, :created_by, :updated_by);


-- :name insert-sys-options-types
-- :command :execute
-- :result :raw
-- :doc Insert into sys.options_types
insert into sys.options_types
  (value, label, location, created_by, updated_by)
values
  (:value, :label, :location, :created_by, :updated_by);


-- :name insert-sys-options-controls
-- :command :execute
-- :result :raw
-- :doc Insert into sys.options_controls
insert into sys.options_controls
  (value, label, location, created_by, updated_by)
values
  (:value, :label, :location, :created_by, :updated_by);


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


-- :name insert-sys-tables
-- :command :execute
-- :result :raw
-- :doc Insert into sys.tables
insert into sys.tables
  (schema_name, table_name, is_view, created_by, updated_by)
values
  (:schema_name, :table_name, :is_view, :created_by, :updated_by);


-- :name insert-sys-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.fields
insert into sys.fields 
  (schema_name, table_name, field_name, type, is_function, is_id, is_uk, is_fk, is_settable, label, control, size, disabled, readonly, required, location, in_table_view, search_fields_id, 
  boolean_true, boolean_false,
  date_min, date_max,
  datetime_min, datetime_max,
  integer_step, integer_min, integer_max,
  float_step, float_min, float_max,
  select_multiple, select_size, select_option_table, select_result_view,
  text_max_length,
  textarea_cols, textarea_rows,
  created_by, updated_by)
values
  (:schema_name, :table_name, :field_name, :type, :is_function, :is_id, :is_uk, :is_fk, :is_settable, :label, :control, :size, :disabled, :readonly, :required, :location, :in_table_view, :search_fields_id,
  :boolean_true, :boolean_false,
  :date_min::date, :date_max::date,
  :datetime_min::timestamptz, :datetime_max::timestamptz,
  :integer_step, :integer_min, :integer_max,
  :float_step, :float_min, :float_max,
  :select_multiple, :select_size, :select_option_table, :select_result_view,
  :text_max_length,
  :textarea_cols, :textarea_rows,
  :created_by, :updated_by);
  

-- :name insert-sys-view-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.view_fields
insert into sys.view_fields 
  (schema_name, table_name, field_name, sys_fields_id, label, location, created_by, updated_by)
values
  (:schema_name, :table_name, :field_name, :sys_fields_id, :label, :location, :created_by, :updated_by);
  

-- :name insert-sys-event-classes
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_classes
insert into sys.event_classes
  (event_classes_id, function_name, argument_name_id, precedence_expression, precedence_events, created_by, updated_by)
values
  (:event_classes_id, :function_name, :argument_name_id, :precedence_expression, :precedence_events, :created_by, :updated_by);


-- :name insert-sys-event-class-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_class_fields
insert into sys.event_class_fields 
  (event_classes_id, fields_id, location, disabled, readonly, required, created_by, updated_by)
values 
  (:event_classes_id, :fields_id, :location, :disabled, :readonly, :required, :created_by, :updated_by);


-- :name select-sys-event-classes-all
-- :command :query
-- :result many
-- :doc Select all event classes
select event_classes_id as value, function_name 
from sys.event_classes 
order by event_classes_id, function_name


