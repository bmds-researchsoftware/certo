-- :name create-extensions
-- :command :execute
-- :result :raw
-- :doc Create extensions
create extension if not exists "uuid-ossp";
create extension if not exists btree_gist;


-- :name create-schema-sys
-- :command :execute
-- :result :raw
-- :doc Create schema sys
drop schema if exists sys cascade;
create schema sys;


-- :name create-trigger-sys-set-updated-at
-- :command :execute
-- :result :raw
-- :doc Create trigger sys.set_updated_at
create or replace function sys.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;


-- :name create-function-sys-create-trigger-set-updated-at
-- :command :execute
-- :result :raw
-- :doc Create function sys.create_trigger_set_updated_at
create or replace function sys.create_trigger_set_updated_at(tbl text) returns void as $$
begin
  execute format('create trigger trigger_set_updated_at before update on %s for each row execute procedure sys.set_updated_at();', tbl);
end;
$$ language plpgsql;


-- :name create-table-sys-options-usergroups
-- :command :execute
-- :result :raw
-- :doc Create table sys.options_usergroups
create table sys.options_usergroups (
  value text primary key,
  label text not null,
  location int8 constraint valid_sys_options_usergroups_location check (location is null or location >= 0),
  created_by text constraint valid_sys_options_usergroups_created_by check (created_by = 'root'),
  created_at timestamptz default current_timestamp,
  updated_by text constraint valid_sys_options_usergroups_updated_by check (updated_by = 'root'),
  updated_at timestamptz default current_timestamp);
select sys.create_trigger_set_updated_at('sys.options_usergroups');


-- :name create-table-sys-users
-- :command :execute
-- :result :raw
-- :doc Create table sys.users
create table sys.users (
  username text primary key,
  password text not null,
  usergroup text references sys.options_usergroups (value) not null,
  created_by text references sys.users (username),
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username),
  updated_at timestamptz default current_timestamp);
select sys.create_trigger_set_updated_at('sys.users');
create index on sys.users (usergroup);


-- :name create-table-sys-options-types
-- :command :execute
-- :result :raw
-- :doc Create table sys.options_types
create table sys.options_types (
  value text primary key,
  label text not null,
  location int8 constraint valid_sys_options_types_location check (location is null or location >= 0),
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
select sys.create_trigger_set_updated_at('sys.options_types');


-- :name create-table-sys-options-controls
-- :command :execute
-- :result :raw
-- :doc Create table sys.options_controls
create table sys.options_controls (
  value text primary key,
  label text not null,
  location int8 constraint valid_sys_options_controls_location check (location is null or location >= 0),
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
select sys.create_trigger_set_updated_at('sys.options_controls');


-- :name create-table-sys-options-foreign-key-queries
-- :command :execute
-- :result :raw
-- :doc Create table sys.options_foreign_key_queries
create table sys.options_foreign_key_queries (
  value text primary key,
  label text not null,
  query text,
  location int8 constraint valid_sys_options_foreign_key_queries_location check (location is null or location >= 0),
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
select sys.create_trigger_set_updated_at('sys.options_foreign_key_queries');


-- :name create-table-sys-options-function-names
-- :command :execute
-- :result :raw
-- :doc Create table sys.options_function_names
create table sys.options_function_names (
  value text primary key,
  label text not null,
  location int8 constraint valid_sys_options_function_names_location check (location is null or location >= 0),
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
select sys.create_trigger_set_updated_at('sys.options_function_names');


-- :name create-table-sys-tables
-- :command :execute
-- :result :raw
-- :doc Create table sys.tables
create table sys.tables (
  id serial8 primary key,
  schema_name text not null,
  table_name text not null,
  view text,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,
  unique (schema_name, table_name));
select sys.create_trigger_set_updated_at('sys.tables');


-- :name create-table-sys-fields
-- :command :execute
-- :result :raw
-- :doc Create table sys.fields
create table sys.fields (
  fields_id text primary key,  
  type text references sys.options_types (value) not null,
  is_pk boolean not null,
  is_pk_in_new boolean,
  label text not null,
  control text references sys.options_controls (value) not null,
  location int8 constraint valid_sys_fields_location check (location is not null and location >= 0),
  in_table_view boolean not null,
  disabled boolean not null,
  readonly boolean not null,
  required boolean not null,

  text_max_length int8,
  -- format text, <----- maybe use cl-format

  textarea_cols int8,
  textarea_rows int8,

  boolean_true text,
  boolean_false text,

  date_min date,
  date_max date,

  foreign_key_query text references sys.options_foreign_key_queries (value),
  foreign_key_size int8,
  
  integer_step int8,
  integer_min int8,
  integer_max int8,
  
  float_step float8,
  float_min float8,
  float_max float8,

  select_multiple boolean,
  select_size int8,
  options_schema_table text,

  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,

  unique (fields_id, location),

  constraint valid_pk
  check ((is_pk = 'true') or (is_pk = 'false' and is_pk_in_new is null)),

  -- start: constraints for types --
  constraint valid_boolean_type_controls
  check ((type = 'boolean' and (control='boolean-select')) or (type != 'boolean')),

  constraint valid_date_type_controls
  check ((type = 'date' and (control='date')) or (type != 'date')),

  constraint valid_float8_type_controls
  check ((type = 'float8' and (control='float')) or (type != 'float')),

  constraint valid_int8_type_controls
  check ((type = 'int8' and ((control='integer') or (control='integer-key') or (control='select') or (control='foreign-key-static'))) or (type != 'int8')),

  constraint valid_serial8_type_controls
  check ((type = 'serial8' and (control='integer-key')) or (type != 'serial8')),

  constraint valid_text_type_controls
  check ((type = 'text' and (control='foreign-key-static' or control='text-key' or control='text' or control='textarea' or control='select')) or (type != 'text')),

  constraint valid_timestamptz_type_controls
  check ((type = 'timestamptz' and ((control='timestamp') or (control='datetime'))) or (type != 'timestamptz')),

  -- TO DO: should make a uuid-id control
  constraint valid_uuid_type_controls
  check ((type = 'uuid' and (control='text')) or (type != 'uuid')),
  -- end: constraints for types --

  -- start: constraints for controls --
  constraint valid_boolean_select_control_attributes
  check ((control='boolean-select' and boolean_true is not null and boolean_false is not null) or
  	(control != 'boolean-select' and boolean_true is null and boolean_false is null)),

  constraint valid_date_control_attributes
  check ((control='date' and date_min is not null and date_max is not null and date_min <= date_max) or
  	(control='date' and (date_min is null or date_max is null)) or
	(control != 'date' and date_min is null and date_max is null)),

  -- constraint valid_datetime_control_attributes
  -- check ((control='datetime' and ???) or (control != 'datetime')),

  constraint valid_foreign_key_static_control_attributes
  check ((control='foreign-key-static' and foreign_key_query is not null and foreign_key_size >= 0) or 
  	(control != 'foreign-key-static' and foreign_key_query is null and foreign_key_size is null)),

  constraint valid_integer_control_attributes
  check (((control='integer' and integer_step is not null) and 
  			     integer_min is not null and integer_max is not null and integer_min <= integer_max) or
  	((control='integer' and integer_step is not null) and (integer_min is null or integer_max is null)) or
	(control != 'integer' and integer_step is null and integer_min is null and integer_max is null)),

  constraint valid_integer_key_control_attributes
  check ((control='integer-key' and disabled='false' and readonly='true' and required='false') or (control != 'integer-key')),

  constraint valid_float_control_attributes
  check (((control='float' and float_step is not null) and 
  			     float_min is not null and float_max is not null and float_min <= float_max) or
  	((control='float' and float_step is not null) and (float_min is null or float_max is null)) or
	(control != 'float' and float_step is null and float_min is null and float_max is null)),

  constraint valid_select_control_attributes
  check ((control='select' and select_multiple is not null and select_size is not null and select_size >= 0 and options_schema_table is not null) or
  	(control != 'select' and select_multiple is null and select_size is null and options_schema_table is null)),

  constraint valid_text_key_control_attributes
  check ((control='text-key' and disabled='false' and readonly='true' and required='false') or (control != 'text-key')),

  -- if text_max_length is not null and text_max_length > 0
  -- then control='foreign-key-static' or control = 'text' or control = 'textarea' or control = 'text-key'
  constraint valid_textual_control_attributes
  check ((text_max_length is null or text_max_length <= 0) or
  	(control='foreign-key-static' or control = 'text' or control = 'textarea' or control = 'text-key')),

  -- if textarea_cols is not null and textarea_cols > 0 and textarea_rows is not null and textarea_rows > 0
  -- then control = 'textarea'
  constraint valid_textarea_control_attributes
  check ((textarea_cols is null or textarea_cols <= 0 or textarea_rows is null or textarea_rows <= 0) or control = 'textarea')

  -- constraint valid_timestamp_control_attributes
  -- check ((control='timestamp' and ???) or (control != 'timestamp')),

  -- constraint valid_yes-no_control_attributes
  -- check ((control='yes-no' and ???) or (control != 'yes-no'))
  
  -- end: constraints for controls --
);
select create_trigger_set_updated_at('sys.fields');

create function schema_name(sys.fields)
returns text as
$$
  select (string_to_array($1.fields_id, '.'))[1]
$$
language sql stable;

create function table_name(sys.fields)
returns text as
$$
  select (string_to_array($1.fields_id, '.'))[2]
$$
language sql stable;

create function field_name(sys.fields)
returns text as
$$
  select (string_to_array($1.fields_id, '.'))[3]
$$
language sql stable;

-- ensures that there is only one primary key field per schema.table
create unique index unique_pk on sys.fields ((sys.fields.schema_name), (sys.fields.table_name)) where is_pk;


-- :name create-table-sys-event-classes
-- :command :execute
-- :result :raw
-- :doc Create table sys.event_classes
create table sys.event_classes (
  event_classes_id text primary key,
  -- If study.participant_id = 2345 and function_name='screen-participant', and
  -- sys.fields contains the row (id = 54, schema_name=study, table_name=people, field_name=participant_id),
  -- then study.participant_id = 2345 is passed into the
  -- function whose name is 'screen-participant'.
  function_name text references sys.options_function_names (value) not null,
  argument_name_id text references sys.fields (fields_id),
  description text,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
select sys.create_trigger_set_updated_at('sys.event_classes');


-- :name create-table-sys-event-classes_fields
-- :command :execute
-- :result :raw
-- :doc Create table sys.event_classes_fields
create table sys.event_classes_fields (
  event_classes_fields_id serial8 primary key,
  event_classes_id text references sys.event_classes (event_classes_id) not null,
  fields_id text references sys.fields (fields_id) not null,
  location int8 constraint valid_sys_event_classes_fields_location check (location is not null and location >= 0),
  disabled boolean not null,
  readonly boolean not null,
  required boolean not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
create index on sys.event_classes_fields (event_classes_id);
create index on sys.event_classes_fields (fields_id);
select sys.create_trigger_set_updated_at('sys.event_classes_fields');


-- :name create-table-sys-event-queue
-- :command :execute
-- :result :raw
-- :doc Create table sys.event_queue
create table sys.event_queue (
  event_queue_id serial8 primary key,
  event_classes_id text references sys.event_classes (event_classes_id),
  dates daterange,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
create index sys_event_queue_dates_index on sys.event_queue using gist (dates);
create index on sys.event_queue (event_classes_id);
select sys.create_trigger_set_updated_at('sys.event_queue');


-- :name create-table-sys-events
-- :command :execute
-- :result :raw
-- :doc Create table sys.events
create table sys.events (
  events_id serial8 primary key,
  event_classes_id text references sys.event_classes (event_classes_id),
  event_by text references sys.users (username) not null,
  event_at date not null,
  event_data jsonb,
  event_notes text,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
create index on sys.events (event_classes_id);
select sys.create_trigger_set_updated_at('sys.events');

