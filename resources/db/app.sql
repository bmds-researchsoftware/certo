-- :name select-study-subjects-all
-- :command :query
-- :result many
-- :doc select all the study.subjectss
select *
from 
study.subjects


-- :name select-study-subjects-by-last-name
-- :command :query
-- :result many
-- :doc select all the study.subject where last_name=:last_name
select *
from study.subjects
where last_name = :last_name


-- :name insert-study-subjects-one
-- :command :execute
-- :result :raw
-- :doc Insert some study.subjects
insert into study.subjects
(first_name, last_name, birth_date, created_by, updated_by) values 
(:first_name, :last_name, :birth_date, :created_by, :updated_by)

