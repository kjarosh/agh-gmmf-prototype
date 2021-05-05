-- get_report.sql

do $$ begin
   perform report((select (min(time) + double precision '0.1' * (max(time) - min(time))) from dbnotification), (select max(time) from dbnotification));
end $$

-- old version - doesn't ignore launching period
/*
do $$ begin
   perform report((select min(time) from dbnotification), (select max(time) from dbnotification));
end $$
*/
