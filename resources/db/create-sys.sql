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


-- :name create-functions-and-triggers
-- :command :execute
-- :result :raw
-- :doc Create functions and triggers
create or replace function sys.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

create or replace function sys.create_trigger_set_updated_at(tbl text) returns void as $$
begin
  execute format('create trigger trigger_set_updated_at before update on %s for each row execute procedure sys.set_updated_at();', tbl);
end;
$$ language plpgsql;


-- :name create-trigger-set-updated-at
-- :command :query
-- :result :raw
-- :doc Create trigger create_trigger_set_updated_at
-- select sys.create_trigger_set_updated_at(:table);


-- :name create-table-sys-users
-- :command :execute
-- :result :raw
-- :doc Create table sys.users
create table sys.users (
  id uuid primary key default uuid_generate_v1mc(),
  username text unique,
  password text not null,
  usergroup text not null,
  created_by text references sys.users (username),
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username),
  updated_at timestamptz default current_timestamp);

select sys.create_trigger_set_updated_at('sys.users');


-- :name create-schema-val
-- :command :execute
-- :result :raw
-- :doc Create schema val
drop schema if exists val cascade;
create schema val;


-- :name create-table-val-usergroups
-- :command :execute
-- :result :raw
-- :doc Create table val.usergroups
create table val.usergroups (
  -- id serial8 primary key,
  id uuid primary key default uuid_generate_v1mc(),
  usergroup text unique not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select sys.create_trigger_set_updated_at('val.usergroups');


-- :name create-table-val-types
-- :command :execute
-- :result :raw
-- :doc Create table val.types
create table val.types (
  -- id serial8 primary key,
  id uuid primary key default uuid_generate_v1mc(),
  type text unique,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select sys.create_trigger_set_updated_at('val.types');


-- :name create-table-val-controls
-- :command :execute
-- :result :raw
-- :doc Create table val.controls
create table val.controls (
  -- id serial8 primary key,
  id uuid primary key default uuid_generate_v1mc(),
  control text unique,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select sys.create_trigger_set_updated_at('val.controls');


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
  id serial8 primary key,
  schema_name text not null,
  table_name text not null,
  field_name text not null,
  type text references val.types (type) not null,
  is_pk boolean not null,
  label text not null,
  control text references val.controls (control) not null,
  location int8 not null check (location >= 0),
  in_table_view boolean not null,
  disabled boolean not null,
  readonly boolean not null,
  required boolean not null,

  text_max_length int8,
  -- format text, <----- maybe use cl-format
  
  date_min date,
  date_max date,
  
  integer_step integer,
  integer_min integer,
  integer_max integer,
  
  float_step float,  
  float_min float,
  float_max float,
  
  select_multiple boolean,
  select_size int8,

  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,


  unique (schema_name, table_name, field_name),
  unique (schema_name, table_name, field_name, location),


  -- start: constraints for types --
  constraint valid_boolean_type_controls
  check ((type = 'boolean' and (control='yes-no')) or (type != 'boolean')),

  constraint valid_date_type_controls
  check ((type = 'date' and (control='date')) or (type != 'date')),

  constraint valid_float8_type_controls
  check ((type = 'float8' and (control='float')) or (type != 'float')),

  constraint valid_int8_type_controls
  check ((type = 'int8' and ((control='integer') or (control='select'))) or (type != 'float')),

  -- TO DO: should make an integer-id control
  constraint valid_serial8_type_controls
  check ((type = 'serial8' and (control='text')) or (type != 'serial8')),

  constraint valid_text_type_controls
  check ((type = 'text' and ((control='text') or (control='textarea') or (control='select'))) or (type != 'text')),

  constraint valid_timestamptz_type_controls
  check ((type = 'timestamptz' and ((control='timestamp') or (control='datetime'))) or (type != 'timestamptz')),

  -- TO DO: should make a uuid-id control
  constraint valid_uuid_type_controls
  check ((type = 'uuid' and (control='text')) or (type != 'uuid')),
  -- end: constraints for types --


  -- start: constraints for controls --
  constraint valid_date_control_attributes
  check ((control='date' and date_min is not null and date_max is not null and date_min <= date_max) or
  	(control='date' and (date_min is null or date_max is null)) or
	(control != 'date' and date_min is null and date_max is null)),

  -- constraint valid_datetime_control_attributes
  -- check ((control='datetime' and ???) or (control != 'datetime')),

  constraint valid_integer_control_attributes
  check (((control='integer' and integer_step is not null) and 
  			     integer_min is not null and integer_max is not null and integer_min <= integer_max) or
  	((control='integer' and integer_step is not null) and (integer_min is null or integer_max is null)) or
	(control != 'integer' and integer_step is null and integer_min is null and integer_max is null)),

  constraint valid_float_control_attributes
  check (((control='float' and float_step is not null) and 
  			     float_min is not null and float_max is not null and float_min <= float_max) or
  	((control='float' and float_step is not null) and (float_min is null or float_max is null)) or
	(control != 'float' and float_step is null and float_min is null and float_max is null)),

  constraint valid_select_control_attributes
  check ((control='select' and select_multiple is not null and select_size >= 0) or
  	(control != 'select' and select_multiple is null and select_size is null)),

  constraint valid_text_textarea_control_attributes
  check (((control='text' or control='textarea') and text_max_length > 0) or 
  	(control != 'text' and control != 'textarea' and text_max_length is null))

  -- constraint valid_timestamp_control_attributes
  -- check ((control='timestamp' and ???) or (control != 'timestamp')),

  -- constraint valid_yes-no_control_attributes
  -- check ((control='yes-no' and ???) or (control != 'yes-no'))
  
  -- end: constraints for controls --
);

select create_trigger_set_updated_at('sys.fields');

-- ensures that there is only one primary key field per schema.table
create unique index unique_pk on sys.fields (schema_name, table_name) where is_pk;


-- :name create-table-sys-event-classes
-- :command :execute
-- :result :raw
-- :doc Create table sys.event_classes
create table sys.event_classes (
  id serial8 primary key,
  name text not null,
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
  id serial8 primary key,
  event_classes_id int8 references sys.event_classes (id) not null,
  fields_id int8 references sys.fields (id) not null,
  location int8 not null check (location >= 0),  
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
  
select sys.create_trigger_set_updated_at('sys.event_classes_fields');


-- :name create-table-sys-events
-- :command :execute
-- :result :raw
-- :doc Create table sys.events
create table sys.events (
  id serial8 primary key,
  -- id uuid primary key default uuid_generate_v1mc(),
  event_classes_id int8 references sys.event_classes (id),
  data jsonb,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
  
select sys.create_trigger_set_updated_at('sys.events');


-- :name create-table-sys-events-queue
-- :command :execute
-- :result :raw
-- :doc Create table sys.events_queue
create table sys.events_queue (
  id serial8 primary key,
  event_classes_id int8 references sys.event_classes (id) not null,
  -- this corresponds to the dimension table that this new event pertains to.
  -- add more dimension tables as necessary
  subjects_id int8 references study.subjects (id),
  active_date_range daterange,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,
    -- add more dimension tables as necessary
  check ((subjects_id is not null)::integer = 1));

create index active_date_range_index on sys.events_queue using gist (active_date_range);
  -- add more dimension tables as necessary
create unique index on sys.events_queue (subjects_id) where subjects_id is not null;
select sys.create_trigger_set_updated_at('sys.events_queue');


-- :name create-table-sys-notes
-- :command :execute
-- :result :raw
-- :doc Create table sys.notes
create table sys.notes (
  id serial8 primary key,
  event_classes_id int8 references sys.event_classes (id),
  events_id int8 references sys.events (id),
  subjects_id int8 references study.subjects (id),
  note text not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,
  check (((event_classes_id is not null)::integer + (events_id is not null)::integer) = 1));
  
select sys.create_trigger_set_updated_at('sys.notes');
create unique index on sys.notes (event_classes_id) where event_classes_id is not null;
create unique index on sys.notes (events_id) where events_id is not null;


-- :name create-table-sys-select-options
-- :command :execute
-- :result :raw
-- :doc Create table sys.select_options
create table sys.select_options (
  id serial8 primary key,
  schema_name text not null,
  table_name text not null,
  field_name text not null,
  label text not null,
  text_value text,
  integer_value int8,
  location int8,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,

  constraint valid_value
  check ((text_value is not null and integer_value is null) or (text_value is null and integer_value is not null)),

  constraint valid_location 
  check ((location is null) or (location >= 0)),

  foreign key (schema_name, table_name, field_name) references sys.fields (schema_name, table_name, field_name),

  unique (schema_name, table_name, field_name, location));

select sys.create_trigger_set_updated_at('sys.select_options');

