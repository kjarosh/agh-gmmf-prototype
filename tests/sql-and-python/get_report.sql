do $$ begin
   perform report((select (min(time) + interval '20 second') from dbnotification), (select max(time) from dbnotification));
end $$
