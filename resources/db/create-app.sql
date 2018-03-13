-- :name create-schema-app
-- :command :execute
-- :result :raw
-- :doc Create schema app
drop schema if exists app cascade;
create schema app;


-- :name create-schema-study
-- :command :execute
-- :result :raw
-- :doc Create schema study
drop schema if exists study cascade;
create schema study;


-- :name create-table-app-options-states
-- :command :execute
-- :result :raw
-- :doc Create table app.options_states
create table app.options_states (
  id serial8 primary key,
  label text not null,
  value text unique,
  location int8,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,

  constraint valid_location
  check ((location is null) or (location >= 0)));
  
select sys.create_trigger_set_updated_at('app.options_states');


-- :name create-table-study-subjects
-- :command :execute
-- :result :raw
-- :doc Create table study.subjects
create table study.subjects (
  id serial8 primary key,
  first_name text not null,
  last_name text not null,
  birth_date date,
  birth_state text references app.options_states (value),
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select sys.create_trigger_set_updated_at('study.subjects');
create index on study.subjects (birth_state);


-- :name create-table-app-notes
-- :command :execute
-- :result :raw
-- :doc Create table app.notes
create table app.notes (
  id serial8 primary key,
  subjects_id int8 references study.subjects (id),
  -- addresses_id int8 references study.addresses (id),
  note text not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,
  check ((subjects_id is not null)::integer = 1));
  
select sys.create_trigger_set_updated_at('app.notes');
create unique index on app.notes (subjects_id) where subjects_id is not null;
-- create unique index on app.notes (addresses_id) where addresses_id is not null;

