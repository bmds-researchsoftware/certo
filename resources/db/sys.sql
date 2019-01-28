-- :name insert-options-table
-- :command :execute
-- :result :raw
-- :doc Insert into options table
insert into :i:options-schema-table
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


-- :name insert-sys-event-class-dnfs
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_class_enqueue_dnfs or sys.event_class_dequeue_dnfs
insert into :i:sys-event-class-dnfs-schema-table
  (event_classes_id, term, depends_on_event_classes_id, is_positive, lag_years, lag_months, lag_weeks, lag_hours, lag_days, lag_minutes, lag_seconds, created_by, updated_by)
values
  (:event_classes_id, :term, :depends_on_event_classes_id, :is_positive, :lag_years, :lag_months, :lag_weeks, :lag_hours, :lag_days, :lag_minutes, :lag_seconds, :created_by, :updated_by);


-- :name insert-sys-event-class-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.event_class_fields
insert into sys.event_class_fields 
  (event_classes_id, field_name, sys_fields_id, label, location, created_by, updated_by)
values 
  (:event_classes_id, :field_name, :sys_fields_id, :label, :location, :created_by, :updated_by);


-- :name insert-sys-event-queue-depends-on-self
-- :command :execute
-- :result :raw
-- :doc insert sys event queue depends on self
with
event_queue as (
  insert into sys.event_queue
    (event_classes_id, is_queued, lag_years, lag_months, lag_weeks, lag_days, lag_hours, lag_minutes, lag_seconds, start_tstz, created_by, updated_by)
  values
    (:event_classes_id, 'true', :lag_years, :lag_months, :lag_weeks, :lag_days, :lag_hours, :lag_minutes, :lag_seconds, :start_tstz::timestamptz, :created_by, :updated_by)
  returning *)

insert into app.event_queue_dimensions
    (event_queue_id, created_by, updated_by)
  values
    ((select event_queue_id from event_queue), :created_by, :updated_by);


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


-- :name select-event-to-dequeue
-- :command :execute
-- :result :raw
-- :doc Select event to dequeue
update sys.event_queue
set is_queued = 'false'
where event_queue_id = (
  select event_queue_id
  from sys.event_queue
  where event_queue_id = :event_queue_id and is_queued = 'true'
  for update skip locked);


-- :name enqueue-dequeue-event-class-dependencies
-- :command :query
-- :result many
-- :doc Select the event_classes_id and term of the event classes that depend on :depends-on-event-classes-id
select dnfs.event_classes_id, dnfs.term, count(*) degree
from :i:sys-event-class-dnfs-schema-table dnfs
inner join
  (select event_classes_id, term
   from :i:sys-event-class-dnfs-schema-table
   where depends_on_event_classes_id = :depends-on-event-classes-id) deps
  on (dnfs.event_classes_id=deps.event_classes_id and dnfs.term=deps.term)
group by dnfs.event_classes_id, dnfs.term;


-- :name enqueue-dequeue-event-class-candidates
-- :command :query
-- :result many
-- :doc Select information necessary to determine if :term for :event_classes_id is true
select distinct on (se.event_classes_id) secd.event_classes_id, secd.term, secd.depends_on_event_classes_id, secd.is_positive, se.is_event_done, aed.*
from :i:sys-event-class-dnfs-schema-table secd
inner join sys.events se
  on secd.depends_on_event_classes_id=se.event_classes_id
inner join app.event_dimensions aed
  on se.events_id=aed.events_id
where secd.event_classes_id = :event_classes_id and secd.term = :term :i:event-class-dimensions-superset-where-clause
order by se.event_classes_id, events_id desc;

