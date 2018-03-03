-- $ sudo -u postgres createuser certo -d -s -P

-- Add the following line to pg_hba.conf:
-- local	 all		 certo		 			 md5

-- $ sudo -u postgres service postgresql reload

-- $ createdb -U certo certo

-- Add the following line to .pgpass:
-- localhost:5432:certo:certo:PASSWORD

-- $ cd certo/resources

-- $ psql -U certo -d certo

-- certo=# \i createdb.sql


-- Note: primary keys can either be serial8 or uuid


-- start: extensions --
create extension if not exists "uuid-ossp";
create extension if not exists btree_gist;
-- end: extensions --


-- start: functions and triggers --
create or replace function set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

create or replace function create_trigger_set_updated_at(tbl text) returns void as $$
begin
  execute format('create trigger trigger_set_updated_at before update on %s for each row execute procedure set_updated_at();', tbl);
end;
$$ language plpgsql;
-- end: functions and triggers --


-- start: schema - sys --
drop schema if exists sys cascade;
create schema sys;

-- -- start: table - sys.users -- --
create table sys.users (
  id uuid primary key default uuid_generate_v1mc(),
  username text unique,
  password text not null,
  usertype text not null,
  created_by text references sys.users (username),
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username),
  updated_at timestamptz default current_timestamp);
  
select create_trigger_set_updated_at('sys.users');

insert into sys.users 
(username, password, usertype, created_by, updated_by) values 
('root',  'rootpw',  'superuser',     'root', 'root'),
('djneu', 'djneupw', 'administrator', 'root', 'root');
-- -- end: table - sys.users -- --
-- end: schema - sys --


-- start: schema - val --
drop schema if exists val cascade;
create schema val;

-- -- start: table - val.usertypes -- --
create table val.usertypes (
  -- id serial8 primary key,
  id uuid primary key default uuid_generate_v1mc(),
  usertype text unique not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select create_trigger_set_updated_at('val.usertypes');

insert into val.usertypes
(usertype, created_by, updated_by) values 
('superuser',     'root', 'root'),
('administrator', 'root', 'root'),
('manager',       'root', 'root'),
('coordinator',   'root', 'root');
-- -- end: table - val.usertypes -- --

-- -- start: table - val.types -- --
create table val.types (
  -- id serial8 primary key,
  id uuid primary key default uuid_generate_v1mc(),
  type text unique,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select create_trigger_set_updated_at('val.types');

insert into val.types 
(type, created_by, updated_by) values 
('boolean',     'root', 'root'),
('date',        'root', 'root'),
('float8',      'root', 'root'),
('int8',        'root', 'root'),
('serial8',     'root', 'root'),
('text',        'root', 'root'),
('timestamptz', 'root', 'root'),
('uuid',        'root', 'root');
-- -- end: table - val.types -- --

-- -- start: table - val.controls -- --
create table val.controls (
  -- id serial8 primary key,
  id uuid primary key default uuid_generate_v1mc(),
  control text unique,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select create_trigger_set_updated_at('val.controls');

insert into val.controls 
(control, created_by, updated_by) values 
('date',      'root', 'root'),
('datetime',  'root', 'root'),
('float',     'root', 'root'),
('integer',   'root', 'root'),
('select',    'root', 'root'),
('text',      'root', 'root'),
('textarea',  'root', 'root'),
('timestamp', 'root', 'root'),
('yes-no',    'root', 'root');
-- -- end: table - val.controls -- --
-- end: schema - val --


-- start: schema - sys --
-- -- start: table - sys.tables -- --
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
select create_trigger_set_updated_at('sys.tables');
-- -- end: table - sys.tables -- --


-- -- start: table - sys.fields -- --
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
  -- format text, <----- maybe use cl-format
  text_max_length int8,
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
  disabled boolean not null,
  readonly boolean not null,
  required boolean not null,
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

select create_trigger_set_updated_at('sys.fields');

-- ensures that there is only one primary key field per schema.table
create unique index unique_pk on sys.fields (schema_name, table_name) where is_pk;

-- TO DO:
-- add constraint date_min_less_than_date_max
-- add constraint integer_min_less_than_integer_max
-- add constraint float_min_less_than_float_max
-- add constraint that ensures that when control='select' select_multiple and select_size are required

-- -- -- start: sys.fields rows for sys.fields -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'id', 'serial8', 'true', 'ID', 'integer', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'schema_name', 'text', 'false', 'Schema Name', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'table_name', 'text', 'false', 'Table Name', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'field_name', 'text', 'false', 'Field Name', 'text', 3, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'type', 'text', 'false', 'Type', 'select', 4, 'true', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'is_pk', 'boolean', 'false', 'Is Primary Key?', 'yes-no', 5, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'label', 'text', 'false', 'Label', 'text', 6, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'control', 'text', 'false', 'Control', 'select', 7, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, integer_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'position', 'int8', 'false', 'Position', 'integer', 8, 'false',  1, 0, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'in_table_view', 'boolean', 'false', 'In Table View?', 'yes-no', 9, 'false', 'false', 'false', 'true', 'root', 'root');
--   -- -- format text, <----- maybe use cl-format
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, integer_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'text_max_length', 'int8', 'false', 'Text Max Length', 'integer', 10, 'false',  1, 1, 'false', 'false', 'false','root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'date_min', 'date', 'false', 'Date Min', 'date', 11, 'false', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'date_max', 'date', 'false', 'Date Max', 'date', 12, 'false', 'false', 'false', 'false', 'root', 'root');

insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'integer_step', 'int8', 'false', 'Integer Step', 'integer', 13, 'false',  1, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'integer_min', 'int8', 'false', 'Integer Min', 'integer', 14, 'false',  1, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'integer_max', 'int8', 'false', 'Integer Max', 'integer', 15, 'false',  1, 'false', 'false', 'false', 'root', 'root');

insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, float_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'float_step', 'float8', 'false', 'Float Step', 'float', 16, 'false', 0.0000000001, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, float_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'float_min', 'float8', 'false', 'Float Min', 'float', 17, 'false',  -1E-307, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, float_max, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'float_max', 'float8', 'false', 'Float Max', 'float', 18, 'false',  1E+308, 'false', 'false', 'false', 'root', 'root');

insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'select_multiple', 'boolean', 'false', 'Select Multiple?', 'yes-no', 19, 'false', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, integer_step, integer_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'select_size', 'int8', 'false', 'Select Size', 'integer', 20, 'false', 1, 0, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'disabled', 'boolean', 'false', 'Disabled?', 'yes-no', 21, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'readonly', 'boolean', 'false', 'Readonly?', 'yes-no', 22, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'required', 'boolean', 'false', 'Required?', 'yes-no', 23, 'false', 'false', 'false', 'true', 'root', 'root');
-- -- -- end: sys.fields rows for sys.fields -- -- --

-- -- -- start: sys.fields rows for study.subjects -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'id', 'serial8', 'true', 'ID', 'integer', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'first_name', 'text', 'false', 'First Name', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'last_name', 'text', 'false', 'Last Name', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, date_min, date_max, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'birth_date', 'date', 'false', 'Birth Date', 'date', 3, 'true', '1700-01-01', '2025-12-31', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'birth_state', 'text', 'false', 'Birth State', 'select', 4, 'false', 'false', 'false', 'false', 'root', 'root');
-- -- -- end: sys.fields rows for study.subjects -- -- --

-- -- -- start: sys.fields rows for val.controls -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('val', 'controls', 'id', 'uuid', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('val', 'controls', 'control', 'text', 'false', 'Control', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
-- -- -- end: sys.fields rows for val.controls -- -- --

-- -- -- start: sys.fields rows for val.types -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('val', 'types', 'id', 'uuid', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('val', 'types', 'type', 'text', 'false', 'Type', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
-- -- -- end: sys.fields rows for val.types -- -- --

-- -- -- start: sys.fields rows for sys.users -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'id', 'uuid', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'username', 'text', 'false', 'Username', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'password', 'text', 'false', 'Password', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, position, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'usertype', 'text', 'false', 'Usertype', 'select', 3, 'true', 'false', 'false', 'true', 'root', 'root');
-- -- -- end: sys.fields rows for sys.users -- -- --
-- -- end: table - sys.fields -- --

-- -- start: table - sys.event_classes -- --
create table sys.event_classes (
  id serial8 primary key,
  name text not null,
  description text,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
  
select create_trigger_set_updated_at('sys.event_classes');
-- -- end: table - sys.event_classes -- --

-- -- start: table - sys.event_classes_fields -- --
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
  
select create_trigger_set_updated_at('sys.event_classes_fields');
-- -- end: table - sys.event_classes_fields -- --

-- -- start: table - sys.events -- --
create table sys.events (
  id serial8 primary key,
  -- id uuid primary key default uuid_generate_v1mc(),
  event_classes_id int8 references sys.event_classes (id),
  data jsonb,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
  
select create_trigger_set_updated_at('sys.events');
-- -- end: table - sys.events -- --

-- -- start: table - sys.events_queue -- --
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
select create_trigger_set_updated_at('sys.events_queue');
-- -- end: table - sys.events_queue -- --

-- -- start: table - sys.notes -- --
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
  
select create_trigger_set_updated_at('sys.notes');
create unique index on sys.notes (event_classes_id) where event_classes_id is not null;
create unique index on sys.notes (events_id) where events_id is not null;
-- -- end: table - sys.notes -- --
-- end: schema - sys --


-- start: schema - study --
-- -- start: table - study.subjects -- --
drop schema if exists study cascade;
create schema study;

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

select create_trigger_set_updated_at('study.subjects');
-- -- end: table - study.subjects -- --


-- -- start: table - sys.event_classes -- --
insert into sys.event_classes 
(name, description, created_by, updated_by) values 
('Register subject', 'Register a subject in the study.', 'root', 'root');
-- -- end: table - sys.event_classes -- --


-- -- start: table - sys.event_classes_fields -- --
insert into sys.event_classes_fields 
(event_classes_id, fields_id, position, created_by, updated_by) values 
(1, 1, 1, 'root', 'root'),
(1, 2, 2, 'root', 'root'),
(1, 3, 3, 'root', 'root'),
(1, 4, 4, 'root', 'root');
-- -- end: table - sys.event_classes_fields -- --


-- -- start: table - study.notes -- --
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
  
select create_trigger_set_updated_at('study.notes');
create unique index on study.notes (subjects_id) where subjects_id is not null;
-- create unique index on study.notes (addresses_id) where addresses_id is not null;
-- -- end: table - study.notes -- --

insert into study.subjects 
(first_name, last_name, birth_date, created_by, updated_by) values 
('Martha',   'Washington', '1731-06-13', 'djneu', 'djneu'),
('Abigail',  'Adams',      '1744-11-22', 'djneu', 'djneu'),
('Martha',   'Jefferson',  '1748-10-30', 'djneu', 'djneu'),
('Betsy',    'Ross',       '1752-01-01', 'djneu', 'djneu'),
('Dolly',    'Madison',    '1768-05-20', 'djneu', 'djneu'),
('Elizabeth','Monroe',     '1768-06-30', 'djneu', 'djneu'),
('Jackie',   'Kennedy',    '1929-07-28', 'djneu', 'djneu');

insert into study.notes 
(subjects_id, note, created_by, updated_by) values 
(2, 'Abigail Adams was married to the Second President of the United States.', 'djneu', 'djneu');
-- end: schema - study --

-- -- start: table - sys.select_options -- --
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

  unique (schema_name, table_name, field_name, position)
);
select create_trigger_set_updated_at('sys.select_options');

insert into sys.select_options 
(schema_name, table_name, field_name, label, text_value, position, created_by, updated_by) values 
('sys', 'users', 'usertype', '',              '',              0, 'root', 'root'),
('sys', 'users', 'usertype', 'Administrator', 'administrator', 3, 'root', 'root'),
('sys', 'users', 'usertype', 'Manager',        'manager',      2, 'root', 'root'),
('sys', 'users', 'usertype', 'Superuser',      'superuser',    4, 'root', 'root'),
('sys', 'users', 'usertype', 'Coordinator',    'coordinator',  1, 'root', 'root'),

('sys', 'fields', 'is_pk', '',    '',      0, 'root', 'root'),
('sys', 'fields', 'is_pk', 'Yes', 'true',  1, 'root', 'root'),
('sys', 'fields', 'is_pk', 'No',  'false', 2, 'root', 'root'),

('sys', 'fields', 'in_table_view', '',    '',      0, 'root', 'root'),
('sys', 'fields', 'in_table_view', 'Yes', 'true',  1, 'root', 'root'),
('sys', 'fields', 'in_table_view', 'No',  'false', 2, 'root', 'root'),

('sys', 'fields', 'type', '',               '',            0, 'root', 'root'),
('sys', 'fields', 'type', 'Boolean',        'boolean',     1, 'root', 'root'),
('sys', 'fields', 'type', 'Date',           'date',        2, 'root', 'root'),
('sys', 'fields', 'type', 'Floating point', 'float8',      3, 'root', 'root'),
('sys', 'fields', 'type', 'Integer',        'int8',        4, 'root', 'root'),
('sys', 'fields', 'type', 'ID',             'serial8',     5, 'root', 'root'),
('sys', 'fields', 'type', 'Text',           'text',        6, 'root', 'root'),
('sys', 'fields', 'type', 'Timestamp',      'timestamptz', 7, 'root', 'root'),
('sys', 'fields', 'type', 'UUID',           'uuid',        8, 'root', 'root'),

('sys', 'fields', 'control', '',         '',           0, 'root', 'root'),
('sys', 'fields', 'control', 'Date',     'date',       1, 'root', 'root'),
('sys', 'fields', 'control', 'Float',    'float',      2, 'root', 'root'),
('sys', 'fields', 'control', 'Integer',  'integer',    3, 'root', 'root'),
('sys', 'fields', 'control', 'Select',   'select',     4, 'root', 'root'),
('sys', 'fields', 'control', 'Text',     'text',       5, 'root', 'root'),
('sys', 'fields', 'control', 'Textarea', 'textarea',   6, 'root', 'root'),
('sys', 'fields', 'control', 'Yes/No',   'yes-no',     7, 'root', 'root');
-- -- end: table - sys.select_options -- --

-- start: schema - app --
drop schema if exists app cascade;
create schema app;

-- -- start: table - app.select_options -- --
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
select create_trigger_set_updated_at('app.select_options');

insert into app.select_options 
(schema_name, table_name, field_name, label, text_value, position, created_by, updated_by) values 
('study', 'subjects', 'birth_state', '',               '',   0,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Connecticut',    'CT', 1,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Delaware',       'DE', 2,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Georgia',        'GA', 3,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Maryland',       'MD', 4,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Massachusetts',  'MA', 5,  'root', 'root'),
('study', 'subjects', 'birth_state', 'New Hampshire',  'NH', 6,  'root', 'root'),
('study', 'subjects', 'birth_state', 'New Jersey',     'NJ', 7,  'root', 'root'),
('study', 'subjects', 'birth_state', 'New York',       'NY', 8,  'root', 'root'),
('study', 'subjects', 'birth_state', 'North Carolina', 'NC', 9,  'root', 'root'),
('study', 'subjects', 'birth_state', 'Pennsylvania',   'PA', 10, 'root', 'root'),
('study', 'subjects', 'birth_state', 'Rhode Island',   'RI', 11, 'root', 'root'),
('study', 'subjects', 'birth_state', 'South Carolina', 'SC', 12, 'root', 'root'),
('study', 'subjects', 'birth_state', 'Virginia',       'VA', 13, 'root', 'root');
-- -- end: table - study.select_options -- --
-- end: schema - app --

