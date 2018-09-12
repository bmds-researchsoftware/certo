-- :name event_record
-- :command :returning-execute
-- :result :one
-- :doc Insert a record event
insert into sys.events
  (event_classes_id, event_by, event_date, is_event_done, event_not_done_reason, event_data, event_notes, created_by, updated_by)
values
  (:event_classes_id, :event_by, :event_date::date, :is_event_done, :event_not_done_reason, :event_data::jsonb, :event_notes, :created_by, :updated_by)
returning null;

