-- :name insert-options-table
-- :command :execute
-- :result :raw
-- :doc Insert into options table
insert into :i:schema-table
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


-- :name insert-sys-tables
-- :command :execute
-- :result :raw
-- :doc Insert into sys.tables
insert into sys.tables
  (schema_name, table_name, table_type, created_by, updated_by)
values
  (:schema_name, :table_name, :table_type, :created_by, :updated_by)


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
  select_multiple, select_size, select_option_table, select_result_view, select_result_to_text,
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
  :select_multiple, :select_size, :select_option_table, :select_result_view, :select_result_to_text,
  :text_max_length,
  :textarea_cols, :textarea_rows,
  :created_by, :updated_by);
  

-- :name insert-sys-field-sets
-- :command :execute
-- :result :raw
-- :doc Insert into sys.field_sets
insert into sys.field_sets
  (schema_name, table_name, field_name, sys_fields_id, label, location, created_by, updated_by)
values
  (:schema_name, :table_name, :field_name, :sys_fields_id, :label, :location, :created_by, :updated_by);
  

-- :name upsert-sys-event-classes
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_classes
insert into sys.event_classes
  (event_classes_id, schema_name, table_name, function_name, is_time_required, created_by, updated_by)
values
  (:event_classes_id, :schema_name, :table_name, :function_name, :is_time_required, :created_by, :updated_by)
on conflict (event_classes_id) do update set
  (schema_name, table_name, function_name, is_time_required, created_by, updated_by) = (:schema_name, :table_name, :function_name, :is_time_required, :created_by, :updated_by);


-- :name insert-sys-event-class-dependencies
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_class_dependencies
insert into sys.event_class_dependencies
  (event_classes_id, term, depends_on_event_classes_id, is_positive, lag, created_by, updated_by)
values
  (:event_classes_id, :term, :depends_on_event_classes_id, :is_positive, :lag, :created_by, :updated_by);


-- :name insert-sys-event-class-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_class_fields
insert into sys.event_class_fields 
  (event_classes_id, field_name, sys_fields_id, label, location, created_by, updated_by)
values 
  (:event_classes_id, :field_name, :sys_fields_id, :label, :location, :created_by, :updated_by);


-- :name select-sys-event-classes-all
-- :command :query
-- :result many
-- :doc Select all event classes
select event_classes_id as value, function_name 
from sys.event_classes 
order by event_classes_id, function_name


-- :name select-sys-fields-sets-by-schema-table
-- :command :query
-- :result many
-- :doc Select all fields in sys.field_sets for a given schema and table, and join with sys.fields.
select sf.*,
  sfs.field_sets_id "vf_fields_id",
  sfs.tables_id "vf_tables_id",
  sfs.schema_name "vf_schema_name",
  sfs.table_name "vf_table_name",
  sfs.field_name "vf_field_name",
  sfs.sys_fields_id "vf_sys_fields_id",
  sfs.label "vf_label",
  sfs.location "vf_location",
  sfs.created_by "vf_created_by",
  sfs.created_at "vf_created_at",
  sfs.updated_by "vf_updated_by",
  sfs.updated_at "vf_updated_at"
from sys.field_sets as sfs
inner join sys.fields as sf on sfs.sys_fields_id=sf.fields_id
where sfs.schema_name = :schema and sfs.table_name = :table


-- :name select-sys-event-class-fields-by-event-classes-id
-- :command :query
-- :result many
-- :doc Select all fields in sys.event_class_fields for a given event-classes-id, and join with sys.fields.
select sf.*,
  ecf.event_class_fields_id "vf_fields_id",
  ecf.event_classes_id "vf_tables_id",
  'event' "vf_schema_name",
  ecf.event_classes_id "vf_table_name",
  ecf.field_name "vf_field_name",
  ecf.sys_fields_id "vf_sys_fields_id",
  ecf.label "vf_label",
  ecf.location "vf_location",
  ecf.created_by "vf_created_by",
  ecf.created_at "vf_created_at",
  ecf.updated_by "vf_updated_by",
  ecf.updated_at "vf_updated_at" 
from sys.event_class_fields as ecf 
inner join sys.fields as sf on ecf.sys_fields_id=sf.fields_id
where ecf.event_classes_id = :event_classes_id


-- :name select-sys-fields-sets-in-select-result-control-by-schema-table
-- :command :query
-- :result many
-- :doc Select all fields in sys.field_sets that are in a select-result control for a given schema and table, and join with sys.fields.
select srsf.*, -- srsf = select result sys fields
  sfs.field_sets_id "vf_fields_id",
  sfs.tables_id "vf_tables_id",
  sfs.schema_name "vf_schema_name",
  sfs.table_name "vf_table_name",
  sfs.field_name "vf_field_name",
  sfs.sys_fields_id "vf_sys_fields_id",
  sfs.label "vf_label",
  sfs.location "vf_location",
  sfs.created_by "vf_created_by",
  sfs.created_at "vf_created_at",
  sfs.updated_by "vf_updated_by",
  sfs.updated_at "vf_updated_at"
from sys.fields as sf
inner join sys.field_sets as sfs on sf.select_result_view=sfs.tables_id
inner join sys.fields as srsf on sfs.sys_fields_id=srsf.fields_id
where sf.control='select-result' and sf.schema_name = :schema and sf.table_name = :table


-- :name select-sys-fields-sets-in-select-result-control-by-event-classes-id
-- :command :query
-- :result many
-- :doc Select all fields in sys.field_sets that are in a select-result control for a given event-classes-id, and join with sys.fields.
select srsf.*, -- srsf = select result sys fields
  sfs.field_sets_id "vf_fields_id",
  sfs.tables_id "vf_tables_id",
  sfs.schema_name "vf_schema_name",
  sfs.table_name "vf_table_name",
  sfs.field_name "vf_field_name",
  sfs.sys_fields_id "vf_sys_fields_id",
  sfs.label "vf_label",
  sfs.location "vf_location",
  sfs.created_by "vf_created_by",
  sfs.created_at "vf_created_at",
  sfs.updated_by "vf_updated_by",
  sfs.updated_at "vf_updated_at"
from sys.fields as sf
inner join sys.field_sets as sfs on sf.select_result_view=sfs.tables_id
inner join sys.fields as srsf on sfs.sys_fields_id=srsf.fields_id
inner join sys.event_class_fields as ecf on sf.fields_id=ecf.sys_fields_id
where sf.control='select-result' and ecf.event_classes_id=:event_classes_id;


