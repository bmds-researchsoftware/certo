-- :name create-schema-study
-- :command :execute
-- :result :raw
-- :doc Create schema study
drop schema if exists study cascade;
create schema study;


-- :name create-table-study-subjects
-- :command :execute
-- :result :raw
-- :doc Create table study.subjects
create table study.subjects (
  id serial8 primary key,
  first_name text not null,
  last_name text not null,
  birth_date date,
  birth_state text,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select sys.create_trigger_set_updated_at('study.subjects');


-- TO DO: Move this to the app schema
-- :name create-table-study-notes
-- :command :execute
-- :result :raw
-- :doc Create table study.notes
create table study.notes (
  id serial8 primary key,
  subjects_id int8 references study.subjects (id),
  -- addresses_id int8 references study.addresses (id),
  note text not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,
  check ((subjects_id is not null)::integer = 1));
  
select sys.create_trigger_set_updated_at('study.notes');
create unique index on study.notes (subjects_id) where subjects_id is not null;
-- create unique index on study.notes (addresses_id) where addresses_id is not null;


-- :name create-schema-app
-- :command :execute
-- :result :raw
-- :doc Create schema app
drop schema if exists app cascade;
create schema app;


-- :name create-table-app-select-options
-- :command :execute
-- :result :raw
-- :doc Create table app-select-options
create table app.select_options (
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

  foreign key (schema_name, table_name, field_name) references sys.fields (schema_name, table_name, field_name));
  
select sys.create_trigger_set_updated_at('app.select_options');

