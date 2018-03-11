-- start: extensions --
create extension if not exists "uuid-ossp";
create extension if not exists btree_gist;
-- end: extensions --


drop schema if exists sys cascade;
create schema sys;

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
-- drop schema if exists sys cascade;
-- create schema sys;

-- -- start: table - sys.users -- --
create table sys.users (
  id uuid primary key default uuid_generate_v1mc(),
  username text unique,
  password text not null,
  usergroup text not null,
  created_by text references sys.users (username),
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username),
  updated_at timestamptz default current_timestamp);
  
select create_trigger_set_updated_at('sys.users');

insert into sys.users 
(username, password, usergroup, created_by, updated_by) values 
('root',  'rootpw',  'superuser',     'root', 'root'),
('djneu', 'djneupw', 'administrator', 'root', 'root');
-- -- end: table - sys.users -- --
-- end: schema - sys --


-- start: schema - val --
drop schema if exists val cascade;
create schema val;

-- -- start: table - val.usergroups -- --
create table val.usergroups (
  -- id serial8 primary key,
  id uuid primary key default uuid_generate_v1mc(),
  usergroup text unique not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);

select create_trigger_set_updated_at('val.usergroups');

insert into val.usergroups
(usergroup, created_by, updated_by) values 
('superuser',     'root', 'root'),
('administrator', 'root', 'root'),
('manager',       'root', 'root'),
('coordinator',   'root', 'root');
-- -- end: table - val.usergroups -- --

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

  -- TO DO: position is a reserved word change to location

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


-- -- -- start: sys.fields rows for sys.fields -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'id', 'serial8', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');

insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'schema_name', 'text', 'false', 'Schema Name', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'table_name', 'text', 'false', 'Table Name', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'field_name', 'text', 'false', 'Field Name', 'text', 3, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, select_multiple, select_size, created_by, updated_by) values ('sys', 'fields', 'type', 'text', 'false', 'Type', 'select', 4, 'true', 'false', 'false', 'true', 'false', 1, 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'is_pk', 'boolean', 'false', 'Is Primary Key?', 'yes-no', 5, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'label', 'text', 'false', 'Label', 'text', 6, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, select_multiple, select_size, created_by, updated_by) values ('sys', 'fields', 'control', 'text', 'false', 'Control', 'select', 7, 'false', 'false', 'false', 'true', 'false', 1, 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, integer_step, integer_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'location', 'int8', 'false', 'Location', 'integer', 8, 'false',  1, 0, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'in_table_view', 'boolean', 'false', 'In Table View?', 'yes-no', 9, 'false', 'false', 'false', 'true', 'root', 'root');
--   -- -- format text, <----- maybe use cl-format
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, integer_step, integer_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'text_max_length', 'int8', 'false', 'Text Max Length', 'integer', 10, 'false',  1, 1, 'false', 'false', 'false','root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'date_min', 'date', 'false', 'Date Min', 'date', 11, 'false', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'date_max', 'date', 'false', 'Date Max', 'date', 12, 'false', 'false', 'false', 'false', 'root', 'root');

insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, integer_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'integer_step', 'int8', 'false', 'Integer Step', 'integer', 13, 'false',  1, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, integer_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'integer_min', 'int8', 'false', 'Integer Min', 'integer', 14, 'false',  1, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, integer_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'integer_max', 'int8', 'false', 'Integer Max', 'integer', 15, 'false',  1, 'false', 'false', 'false', 'root', 'root');

insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, float_step, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'float_step', 'float8', 'false', 'Float Step', 'float', 16, 'false', 0.0000000001, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, float_step, float_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'float_min', 'float8', 'false', 'Float Min', 'float', 17, 'false',  0.0000000001, -1E-307, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, float_step, float_max, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'float_max', 'float8', 'false', 'Float Max', 'float', 18, 'false', 0.0000000001, 1E+308, 'false', 'false', 'false', 'root', 'root');

insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'select_multiple', 'boolean', 'false', 'Select Multiple?', 'yes-no', 19, 'false', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, integer_step, integer_min, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'select_size', 'int8', 'false', 'Select Size', 'integer', 20, 'false', 1, 0, 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'disabled', 'boolean', 'false', 'Disabled?', 'yes-no', 21, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'readonly', 'boolean', 'false', 'Readonly?', 'yes-no', 22, 'false', 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'fields', 'required', 'boolean', 'false', 'Required?', 'yes-no', 23, 'false', 'false', 'false', 'true', 'root', 'root');
-- -- -- end: sys.fields rows for sys.fields -- -- --

-- -- -- start: sys.fields rows for study.subjects -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'id', 'serial8', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'first_name', 'text', 'false', 'First Name', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'last_name', 'text', 'false', 'Last Name', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, date_min, date_max, disabled, readonly, required, created_by, updated_by) values ('study', 'subjects', 'birth_date', 'date', 'false', 'Birth Date', 'date', 3, 'true', '1700-01-01', '2025-12-31', 'false', 'false', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, select_multiple, select_size, created_by, updated_by) values ('study', 'subjects', 'birth_state', 'text', 'false', 'Birth State', 'select', 4, 'false', 'false', 'false', 'false', 'false', 1, 'root', 'root');
-- -- -- end: sys.fields rows for study.subjects -- -- --

-- -- -- start: sys.fields rows for val.controls -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('val', 'controls', 'id', 'uuid', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('val', 'controls', 'control', 'text', 'false', 'Control', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
-- -- -- end: sys.fields rows for val.controls -- -- --

-- -- -- start: sys.fields rows for val.types -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('val', 'types', 'id', 'uuid', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('val', 'types', 'type', 'text', 'false', 'Type', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
-- -- -- end: sys.fields rows for val.types -- -- --

-- -- -- start: sys.fields rows for sys.users -- -- --
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'id', 'uuid', 'true', 'ID', 'text', 0, 'true', 'false', 'true', 'false', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'username', 'text', 'false', 'Username', 'text', 1, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, text_max_length, disabled, readonly, required, created_by, updated_by) values ('sys', 'users', 'password', 'text', 'false', 'Password', 'text', 2, 'true', 25, 'false', 'false', 'true', 'root', 'root');
insert into sys.fields (schema_name, table_name, field_name, type, is_pk, label, control, location, in_table_view, disabled, readonly, required, select_multiple, select_size, created_by, updated_by) values ('sys', 'users', 'usergroup', 'text', 'false', 'Usergroup', 'select', 3, 'true', 'false', 'false', 'true', 'false', 1, 'root', 'root');
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
  location int8 not null,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,
  constraint valid_location check (location >= 0));
  
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
(event_classes_id, fields_id, location, created_by, updated_by) values 
(1, 1, 1, 'root', 'root'),
(1, 2, 2, 'root', 'root'),
(1, 3, 3, 'root', 'root'),
(1, 4, 4, 'root', 'root');
-- -- end: table - sys.event_classes_fields -- --


-- -- start: table - app.notes -- --
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
  
select create_trigger_set_updated_at('app.notes');
create unique index on app.notes (subjects_id) where subjects_id is not null;
-- create unique index on app.notes (addresses_id) where addresses_id is not null;
-- -- end: table - app.notes -- --

insert into study.subjects 
(first_name, last_name, birth_date, created_by, updated_by) values 
('Martha',   'Washington', '1731-06-13', 'djneu', 'djneu'),
('Abigail',  'Adams',      '1744-11-22', 'djneu', 'djneu'),
('Martha',   'Jefferson',  '1748-10-30', 'djneu', 'djneu'),
('Betsy',    'Ross',       '1752-01-01', 'djneu', 'djneu'),
('Dolly',    'Madison',    '1768-05-20', 'djneu', 'djneu'),
('Elizabeth','Monroe',     '1768-06-30', 'djneu', 'djneu'),
('Jackie',   'Kennedy',    '1929-07-28', 'djneu', 'djneu');

insert into app.notes 
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

  unique (schema_name, table_name, field_name, location)
);
select create_trigger_set_updated_at('sys.select_options');

insert into sys.select_options 
(schema_name, table_name, field_name, label, text_value, location, created_by, updated_by) values 
('sys', 'users', 'usergroup', '',              '',              0, 'root', 'root'),
('sys', 'users', 'usergroup', 'Administrator', 'administrator', 3, 'root', 'root'),
('sys', 'users', 'usergroup', 'Manager',       'manager',       2, 'root', 'root'),
('sys', 'users', 'usergroup', 'Superuser',     'superuser',     4, 'root', 'root'),
('sys', 'users', 'usergroup', 'Coordinator',   'coordinator',   1, 'root', 'root'),

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
  location int8,
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,

  constraint valid_value
  check ((text_value is not null and integer_value is null) or (text_value is null and integer_value is not null)),

  constraint valid_location
  check ((location is null) or (location >= 0)),

  foreign key (schema_name, table_name, field_name) references sys.fields (schema_name, table_name, field_name));
select create_trigger_set_updated_at('app.select_options');

insert into app.select_options 
(schema_name, table_name, field_name, label, text_value, location, created_by, updated_by) values 
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

create or replace function sys.isf (
  -- id int8,
  _schema_name text,
  _table_name text,
  _field_name text,
  _type text,
  _is_pk boolean,
  _label text,
  _control text,
  _pos int8,
  _in_table_view boolean,
  _disabled boolean,
  _readonly boolean,
  _required boolean,

  _text_max_length int8,
  
  _date_min date,
  _date_max date,
  
  _integer_step integer,
  _integer_min integer,
  _integer_max integer,
  
  _float_step float,  
  _float_min float,
  _float_max float,
  
  _select_multiple boolean,
  _select_size int8,

  _created_by text,
  -- _created_at timestamptz,
  _updated_by text
  -- _updated_at timestamptz
) returns void as $$

insert into sys.fields values (
  default,
  _schema_name,
  _table_name,
  _field_name,
  _type,
  _is_pk,
  _label,
  _control,
  _pos,
  _in_table_view,
  _disabled,
  _readonly,
  _required,

  _text_max_length,
  
  _date_min,
  _date_max,
  
  _integer_step,
  _integer_min,
  _integer_max,
  
  _float_step,
  _float_min,
  _float_max,
  
  _select_multiple,
  _select_size,

  _created_by,
  default,
  _updated_by,
  default);

$$ language sql;
-- select sys.isf('study', 'subjects', 'first_name', 'text', 'false', 'First Name', 'text', 1, 'true', 'false', 'false', 'true', 25, null,  null, null, null, null, null, null, null, null, null,'root', 'root');

create or replace function sys.isf_boolean_yes_no (
  -- _id int8,
  _schema_name text,
  _table_name text,
  _field_name text,
  _type text,
  _is_pk boolean,
  _label text,
  _control text,
  _pos int8,
  _in_table_view boolean,
  _disabled boolean,
  _readonly boolean,
  _required boolean,

  _created_by text,
  -- _created_at timestamptz,
  _updated_by text
  -- _updated_at timestamptz
) returns void as $$

insert into sys.fields values (
  default,
  _schema_name,
  _table_name,
  _field_name,
  _type,
  _is_pk,
  _label,
  _control,
  _pos,
  _in_table_view,
  _disabled,
  _readonly,
  _required,

  null, -- text_max_length
  
  null, -- date_min
  null, -- date_max
  
  null, -- integer_step
  null, -- integer_min
  null, -- integer_max
  
  null, -- float_step
  null, -- float_min
  null, -- float_max
  
  null, -- select_multiple,
  null, -- select_size,

  _created_by,
  default,
  _updated_by,
  default);

$$ language sql;

--
create or replace function sys.isf_date_date (
  -- _id int8,
  _schema_name text,
  _table_name text,
  _field_name text,
  _type text,
  _is_pk boolean,
  _label text,
  _control text,
  _pos int8,
  _in_table_view boolean,
  _disabled boolean,
  _readonly boolean,
  _required boolean,

  _date_min date,
  _date_max date,  

  _created_by text,
  -- _created_at timestamptz,
  _updated_by text
  -- _updated_at timestamptz
) returns void as $$

insert into sys.fields values (
  default,
  _schema_name,
  _table_name,
  _field_name,
  _type,
  _is_pk,
  _label,
  _control,
  _pos,
  _in_table_view,
  _disabled,
  _readonly,
  _required,

  null, -- text_max_length
  
  _date_min,
  _date_max,
  
  null, -- integer_step
  null, -- integer_min
  null, -- integer_max
  
  null, -- float_step
  null, -- float_min
  null, -- float_max
  
  null, -- select_multiple,
  null, -- select_size,

  _created_by,
  default,
  _updated_by,
  default);

$$ language sql;
--

create or replace function sys.isf_text_text (
  -- _id int8,
  _schema_name text,
  _table_name text,
  _field_name text,
  _type text,
  _is_pk boolean,
  _label text,
  _control text,
  _pos int8,
  _in_table_view boolean,
  _disabled boolean,
  _readonly boolean,
  _required boolean,

  _text_max_length int8,

  _created_by text,
  -- _created_at timestamptz,
  _updated_by text
  -- _updated_at timestamptz
) returns void as $$

insert into sys.fields values (
  default,
  _schema_name,
  _table_name,
  _field_name,
  _type,
  _is_pk,
  _label,
  _control,
  _pos,
  _in_table_view,
  _disabled,
  _readonly,
  _required,

  _text_max_length,
  
  null, -- date_min
  null, -- date_max
  
  null, -- integer_step
  null, -- integer_min
  null, -- integer_max
  
  null, -- float_step
  null, -- float_min
  null, -- float_max
  
  null, -- select_multiple,
  null, -- select_size,

  _created_by,
  default,
  _updated_by,
  default);

$$ language sql;

-- select sys.isf_text_text('study', 'subjects', 'first_name', 'text', 'false', 'First Name', 'text', 1, 'true', 'false', 'false', 'true', 25, 'root', 'root');

