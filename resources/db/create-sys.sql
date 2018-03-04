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
  usertype text not null,
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


-- :name create-table-val-usertypes
-- :command :execute
-- :result :raw
-- :doc Create table val.usertypes
create table val.usertypes (
  -- id serial8 primary key,
  id uuid primary key default uuid_generate_v1mc(),
  usertype text unique not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select sys.create_trigger_set_updated_at('val.usertypes');


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
  unique (schema_name, table_name)
);

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
  position int8 not null,
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

  unique (schema_name, table_name, field_name, position),

  constraint valid_position check (position >= 0),

  constraint valid_select_size 
  check ((select_size is null) or (select_size >= 0)),

  constraint valid_attribute_text_max_length
  check ((text_max_length is null) or (text_max_length is not null and (type='text') and (control='select' or control='text' or control='textarea'))),

  constraint valid_attribute_date_min
  check ((date_min is null) or (date_min is not null and (type='date') and (control='date'))),

  constraint valid_attribute_date_max
  check ((date_max is null) or (date_max is not null and (type='date') and (control='date'))),

  constraint valid_attribute_integer_min
  check ((integer_min is null) or (integer_min is not null and (type='int8') and (control='integer'))),

  constraint valid_attribute_integer_max
  check ((integer_max is null) or (integer_max is not null and (type='int8') and (control='integer'))),

  constraint valid_attribute_float_min
  check ((float_min is null) or (float_min is not null and (type='float8') and (control='float'))),

  constraint valid_attribute_float_max
  check ((float_max is null) or (float_max is not null and (type='float8') and (control='float'))));

select sys.create_trigger_set_updated_at('sys.fields');

-- TO DO:
-- add constraint date_min_less_than_date_max
-- add constraint integer_min_less_than_integer_max
-- add constraint float_min_less_than_float_max
-- add constraint that ensures that when control='select' select_multiple and select_size are required

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
  position int8 not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,
  constraint valid_position check (position >= 0));
  
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
  position int8,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,

  constraint valid_value
  check ((text_value is not null and integer_value is null) or (text_value is null and integer_value is not null)),

  constraint valid_position 
  check ((position is null) or (position >= 0)),

  foreign key (schema_name, table_name, field_name) references sys.fields (schema_name, table_name, field_name),

  unique (schema_name, table_name, field_name, position));

select sys.create_trigger_set_updated_at('sys.select_options');

