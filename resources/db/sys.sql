-- :name insert-sys-fields
-- :command :execute
-- :result :raw
-- :doc Insert into sys.fields
insert into sys.fields 
(schema_name,   table_name,  field_name,  type,  is_pk,  label,  control,  position,  in_table_view,  disabled,  readonly,  required,  text_max_length, date_min,        date_max,        integer_step,  integer_min,  integer_max,  float_step,  float_min,  float_max,  select_multiple,  select_size,  created_by,  updated_by) values 
(:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :text_max_length, :date_min::date, :date_max::date, :integer_step, :integer_min, :integer_max, :float_step, :float_min, :float_max, :select_multiple, :select_size, :created_by, :updated_by)

