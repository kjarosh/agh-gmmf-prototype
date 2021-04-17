-- get_report.sql
do $$ begin
   perform report((select min(time) from dbnotification), (select max(time) from dbnotification));
end $$

