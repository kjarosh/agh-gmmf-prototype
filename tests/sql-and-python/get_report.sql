-- noinspection SqlNoDataSourceInspectionForFile

-- get_report.sql

do $$ begin
   perform report((select (min(time) + interval '20 second') from dbnotification), (select max(time) from dbnotification));
end $$

-- old version - doesn't ignore launching period
/*
do $$ begin
   perform report((select min(time) from dbnotification), (select max(time) from dbnotification));
end $$
*/

-- old version - ignores first 10% of the measurement period
/*
do $$ begin
   perform report((select (min(time) + double precision '0.1' * (max(time) - min(time))) from dbnotification), (select max(time) from dbnotification));
end $$
*/
