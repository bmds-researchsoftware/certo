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


-- :name create-table-sys-options-schema_table
-- :command :execute
-- :result :raw
-- :doc Create table sys.options_schema_tables
create table sys.options_schema_tables (
  value text primary key,
  label text not null,
  location int8 constraint valid_sys_options_schema_tables_location check (location is null or location >= 0),
  created_by text constraint valid_sys_options_schema_tables_created_by check (created_by = 'root'),
  created_at timestamptz default current_timestamp,
  updated_by text constraint valid_sys_options_schema_tables_updated_by check (updated_by = 'root'),
  updated_at timestamptz default current_timestamp);
select sys.create_trigger_set_updated_at('sys.options_schema_tables');


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


-- :name create-table-sys-options-select-result-function-names
-- :command :execute
-- :result :raw
-- :doc Create table sys.options_select_result_function_names
create table sys.options_select_result_function_names (
  value text primary key,
  label text not null,
  location int8 constraint valid_sys_options_select_result_function_names_location check (location is null or location >= 0),
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp);
select sys.create_trigger_set_updated_at('sys.options_select_result_function_names');


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
  tables_id text not null unique, -- populated by a before trigger as schema_name.table_name
  schema_name text, -- part of pk
  table_name text, -- part of pk
  is_view boolean,
  -- TO DO: order-by
  -- either have this be the name of a postgres function that
  -- returns the order by as a string, or have it be the name
  -- in a one-to-many relationship, i.e. fields_id, ascending boolean, location int8
  -- also see
  -- http://clojure.github.io/java.jdbc/#:clojure.java.jdbc.spec/order-by
  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,
  primary key (schema_name, table_name));
select sys.create_trigger_set_updated_at('sys.tables');


create or replace function sys.update_sys_tables()
returns trigger as $$
begin
  new.tables_id = new.schema_name || '.' || new.table_name;
  return new;
end;
$$ language plpgsql;


create trigger trigger_sys_update_sys_tables
before insert or update on sys.tables
for each row
execute procedure sys.update_sys_tables();

create unique index tables_id_index on sys.tables (tables_id);


-- :name create-table-sys-fields
-- :command :execute
-- :result :raw
-- :doc Create table sys.fields
create table sys.fields (
  fields_id text unique not null, -- populated by a before trigger as schema_name.table_name.field_name

  schema_name text not null, -- part of pk
  table_name text not null, -- part of pk
  field_name text not null, -- part of pk

  type text references sys.options_types (value) not null,

  is_function boolean not null,
  is_id boolean not null,
  is_uk boolean not null,
  is_fk boolean not null,
  is_settable boolean not null,

  label text not null,
  control text references sys.options_controls (value) not null,

  disabled boolean not null,
  readonly boolean not null,
  required boolean not null,

  location int8 constraint valid_sys_fields_location check (location is not null and location >= 0),

  in_table_view boolean not null,
  -- Used to generate link such as https://example.com/sys/people?study.people.particpants_id=1234
  search_fields_id text references sys.fields (fields_id),

  boolean_true text,
  boolean_false text,

  date_min date,
  date_max date,

  datetime_min date,
  datetime_max date,

  integer_step int8,
  integer_min int8,
  integer_max int8,
  
  float_step float8,
  float_min float8,
  float_max float8,

  select_multiple boolean,
  select_size int8,

  select_option_schema_table text references sys.tables (tables_id),
  
  -- TO DO: sys.options_select_result_function_names should be populated from a hash-map of available functions and should not be editable by any user
  select_result_function_name text references sys.options_select_result_function_names (value),

  text_max_length int8,
  -- TO DO: format text, maybe use cl-format
  textarea_cols int8,
  textarea_rows int8,

  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,

  foreign key (schema_name, table_name) references sys.tables (schema_name, table_name),
  primary key (schema_name, table_name, field_name),
  unique (schema_name, table_name, field_name, location),

  -- is_settable='false' => disabled='true' and readonly='true' and required='false'
  constraint valid_settable
  -- check (is_settable='true' or (disabled='true' and readonly='true' and required='false') or is_settable='true'),
  -- or equivalently
  -- ++ confirm this !wxy!z = w V xy!z
  check ((is_settable='false' and disabled='true' and readonly='true' and required='false') or is_settable='true'),

  constraint valid_disabled_readonly_required
  check ((disabled='false' and readonly='false') or 
  	(disabled='true' and readonly='true' and required='false')),
 -- or
 -- 	(disabled is null and readonly is null and required is null)
  -- TO DO: Should restrict to the following
  -- select disabled, readonly, required from sys.fields group by disabled, readonly, required order by disabled, readonly, required;
  --  disabled | readonly | required 
  -- ----------+----------+----------
  --  f        | f        | f
  --  f        | f        | t
  --  t        | t        | f

  -- is_function = 'true' => is_settable = 'false'
  constraint valid_function
  check (is_function='false' or is_settable='false'),

  -- start: constraints for types --
  constraint valid_boolean_type_controls
  check ((type = 'boolean' and control='select-boolean') or (type != 'boolean')),

  constraint valid_date_type_controls
  check ((type = 'date' and control='date') or (type != 'date')),

  constraint valid_float8_type_controls
  check ((type = 'float8' and control='float') or (type != 'float')),

  constraint valid_int8_type_controls
  check ((type = 'int8' and (control='integer' or control='select-option' or control='select-result')) or (type != 'int8')),

  constraint valid_serial8_type_controls
  check ((type = 'serial8' and control='integer') or (type != 'serial8')),

  constraint valid_text_type_controls
  check ((type = 'text' and (control='select-result' or control='text' or control='textarea' or control='select-option')) or (type != 'text')),

  constraint valid_timestamptz_type_controls
  check ((type = 'timestamptz' and (control='timestamp' or control='datetime')) or (type != 'timestamptz')),

  constraint valid_uuid_type_controls
  check ((type = 'uuid' and control='text') or (type != 'uuid')),
  -- end: constraints for types --

  -- start: constraints for controls --
  constraint valid_select_boolean_control_attributes
  check ((control='select-boolean' and boolean_true is not null and boolean_false is not null) or
  	(control != 'select-boolean' and boolean_true is null and boolean_false is null)),

  -- TO DO: Add a time control and type

  constraint valid_date_control_attributes
  check ((control='date' and date_min is not null and date_max is not null and date_min <= date_max) or
  	(control='date' and (date_min is null or date_max is null)) or
	(control='date' and is_settable='false') or
	(control != 'date' and date_min is null and date_max is null)),

  constraint valid_datetime_control_attributes
  check ((control='datetime' and datetime_min is not null and datetime_max is not null and datetime_min <= datetime_max) or
  	(control='datetime' and (datetime_min is null or datetime_max is null)) or
	(control='datetime' and is_settable='false') or
	(control != 'datetime' and datetime_min is null and datetime_max is null)),

  constraint valid_integer_control_attributes
  check ((control='integer' and is_settable='false') or
  	((control='integer' and integer_step is not null) and integer_min is not null and integer_max is not null and integer_min <= integer_max) or
 	((control='integer' and integer_step is not null) and (integer_min is null or integer_max is null)) or
	(control='integer' and is_settable='false') or
	(control != 'integer' and integer_step is null and integer_min is null and integer_max is null)),

  constraint valid_float_control_attributes
  check (((control='float' and float_step is not null) and float_min is not null and float_max is not null and float_min <= float_max) or
  	((control='float' and float_step is not null) and (float_min is null or float_max is null)) or
	(control='float' and is_settable='false') or
	(control != 'float' and float_step is null and float_min is null and float_max is null)),

  constraint valid_select_control_attributes
  check (((control='select-option' or control='select-result') and select_multiple is not null and select_size is not null and select_size >= 0) or
  	(control != 'select-option' and control != 'select-result')),

  constraint valid_select_option_control_attributes
  check ((control='select-option' and select_option_schema_table is not null) or (control != 'select-option')),

  constraint valid_select_result_static_control_attributes
  check ((control='select-result' and select_result_function_name is not null) or (control != 'select-result')),
  
  constraint valid_textual_control_attributes
  check (((control = 'text' or control = 'textarea') and text_max_length is not null and text_max_length > 0) or
 	((control = 'text' or control = 'textarea') and is_settable='false') or
	(control != 'text' and control != 'textarea')),

  -- if textarea_cols is not null and textarea_cols > 0 and textarea_rows is not null and textarea_rows > 0
  -- then control = 'textarea'
  constraint valid_textarea_control_attributes
  check ((control = 'textarea' and  (textarea_cols is not null and textarea_cols > 0 and textarea_rows is not null or textarea_rows > 0)) or
  	(control = 'textarea' and is_settable='false') or
  	(control != 'textarea'))
  -- end: constraints for controls --
);
select sys.create_trigger_set_updated_at('sys.fields');

create or replace function sys.update_sys_fields()
returns trigger as $$
begin
  new.fields_id = new.schema_name || '.' || new.table_name || '.' || new.field_name;
  return new;
end;
$$ language plpgsql;

create trigger trigger_sys_update_sys_fields
before insert or update on sys.fields
for each row
execute procedure sys.update_sys_fields();

create unique index fields_id_index on sys.fields (fields_id);

-- ensures that there is at most one id field per schema.table
create unique index unique_id on sys.fields (schema_name, table_name, field_name) where is_id;

create index sys_fields_schema_name_table_name on sys.fields (schema_name, table_name);

-- :name create-table-sys-view-fields
-- :command :execute
-- :result :raw
-- :doc Create table sys.view_fields
create table sys.view_fields (
  view_fields_id text unique not null, -- populated by a before trigger as schema_name.table_name.field_name

  schema_name text not null, -- part of pk
  table_name text not null, -- part of pk
  field_name text not null, -- part of pk

  fields_id text references sys.fields (fields_id),

  label text not null,
  location int8 constraint valid_sys_fields_location check (location is not null and location >= 0),

  created_by text references sys.users (username) not null,
  created_at timestamptz default current_timestamp,
  updated_by text references sys.users (username) not null,
  updated_at timestamptz default current_timestamp,

  foreign key (schema_name, table_name) references sys.tables (schema_name, table_name),
  primary key (schema_name, table_name, field_name),
  unique (schema_name, table_name, field_name, location));
select sys.create_trigger_set_updated_at('sys.view_fields');

create or replace function sys.update_sys_view_fields()
returns trigger as $$
begin
  new.view_fields_id = new.schema_name || '.' || new.table_name || '.' || new.field_name;
  return new;
end;
$$ language plpgsql;

create trigger trigger_sys_update_sys_view_fields
before insert or update on sys.view_fields
for each row
execute procedure sys.update_sys_view_fields();

create unique index view_fields_id_index on sys.view_fields (view_fields_id);


-- Populates table sys.options_schema_tables with value =
-- schema_name.table_name and reference it from
-- sys.fields.options_table_schema
create or replace function sys.update_sys_options_schema_tables()
returns trigger as
$$
begin
  if new.table_name like 'options_%' then
    insert into sys.options_schema_tables
      (value, label) 
    values
      (new.schema_name || '.' || new.table_name, new.schema_name || '.' || new.table_name)
    on conflict (value) do nothing;
  end if;
  return new;
end;
$$
language plpgsql;

create trigger trigger_sys_update_sys_options_schema_tables
after insert or update on sys.tables
for each row
execute procedure sys.update_sys_options_schema_tables();


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


create view sys.sys_event_classes_all as
select event_classes_id as value, function_name 
from sys.event_classes
order by event_classes_id, function_name;

create or replace function sys.sys_event_classes_all(x int8)
returns setof sys.sys_event_classes_all as $$
  select * from sys.sys_event_classes_all;
$$ language sql;


create view sys.sys_fields_all as
select sys.fields.fields_id as value, sys.fields.schema_name as sn, sys.fields.table_name as tn, sys.fields.field_name as fn 
from sys.fields;

create or replace function sys.sys_fields_all(x int8)
returns setof sys.sys_fields_all as $$
  select * from sys.sys_fields_all;
$$ language sql;


create or replace function sys.sys_fields_all_tn(text)
returns setof sys.sys_fields_all as $$
  select * from sys.sys_fields_all as sfa where tn = $1;
$$ language sql;

