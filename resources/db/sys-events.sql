-- :name insert-event
-- :command :returning-execute
-- :result :one
-- :doc Insert the event
insert into sys.events
  (events_id, event_classes_id, event_by, event_date, is_event_done, event_not_done_reason, event_data, event_notes, created_by, updated_by)
values
  (:event_queue_id, :event_classes_id, :event_by, :event_date::date, :is_event_done, :event_not_done_reason, :event_data::jsonb, :event_notes, :created_by, :updated_by)
returning *;

