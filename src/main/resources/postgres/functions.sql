create or replace function report(st timestamp, et timestamp) returns void as $$
declare
	total_ops int;
	total_events int;
	total_duration interval;
	total_duration_sec double precision;

	sample_duration interval;
	sample_duration_sec double precision;
	sample_ops int;
	sample_ops_finished int;
	sample_ops_succeeded int;
	sample_events int;
	sample_events_started int;
	sample_events_ended int;
	sample_op_duration_min interval;
	sample_op_duration_avg interval;
	sample_op_duration_max interval;
	sample_evt_q_duration_min interval;
	sample_evt_q_duration_avg interval;
	sample_evt_q_duration_max interval;
	sample_evt_p_duration_min interval;
	sample_evt_p_duration_avg interval;
	sample_evt_p_duration_max interval;
begin
    if st >= et then
        raise exception 'start time if after end time';
    end if;
    select count(*) into total_ops from operations;
	select count(*) into total_events from events;
	select max(start_time) - min(start_time) into total_duration from operations;
	total_duration_sec \:= extract(epoch from total_duration);

    raise notice 'Operations: %', total_ops;
    raise notice 'Events: %', total_events;
    raise notice 'Notifications: %', (select count(*) from dbnotification);
    raise notice 'Time between first / last operation: % (% s)', total_duration, total_duration_sec;
    raise notice 'Operations per second: %', total_ops / total_duration_sec;
    raise notice 'Events per second: %', total_events / total_duration_sec;
    raise notice '================================================================================';

	sample_duration \:= et - st;
	sample_duration_sec \:= extract(epoch from sample_duration);
	select count(*) into sample_ops from operations
	    where start_time > st and start_time <= et;
	select count(*) into sample_ops_finished from operations
		where start_time > st and start_time <= et and finished;
	select count(*) into sample_ops_succeeded from operations
		where start_time > st and start_time <= et and success;
	select sum(started_events) into sample_events_started from operations
		where start_time > st and start_time <= et;
	select sum(ended_events) into sample_events_ended from operations
		where start_time > st and start_time <= et;
	select count(*) into sample_events from events
		where start_time > st and start_time <= et;

	select min(duration) into sample_op_duration_min from operations
		where start_time > st and start_time <= et;
	select avg(duration) into sample_op_duration_avg from operations
		where start_time > st and start_time <= et;
	select max(duration) into sample_op_duration_max from operations
		where start_time > st and start_time <= et;

	select min(wait_duration) into sample_evt_q_duration_min from queue_summary
		where start_time > st and start_time <= et;
	select avg(wait_duration) into sample_evt_q_duration_avg from queue_summary
		where start_time > st and start_time <= et;
	select max(wait_duration) into sample_evt_q_duration_max from queue_summary
		where start_time > st and start_time <= et;

	select min(duration) into sample_evt_p_duration_min from events
		where start_time > st and start_time <= et;
	select avg(duration) into sample_evt_p_duration_avg from events
		where start_time > st and start_time <= et;
	select max(duration) into sample_evt_p_duration_max from events
		where start_time > st and start_time <= et;

    raise notice 'Start time: %', st;
    raise notice 'End time: %', et;
    raise notice 'Duration: % (% s)', sample_duration, sample_duration_sec;
    raise notice 'Operations: %', sample_ops;
    raise notice 'Operations finished: %', sample_ops_finished;
    raise notice 'Operations succeeded: %', sample_ops_succeeded;
    raise notice 'Events: %', sample_events;
    raise notice 'Events started: %', sample_events_started;
    raise notice 'Events ended: %', sample_events_ended;
    raise notice 'Operations per second: %', sample_ops / sample_duration_sec;
    raise notice 'Events per second: %', sample_events / sample_duration_sec;
    raise notice 'Operation duration:';
    raise notice '  min: %', sample_op_duration_min;
    raise notice '  avg: %', sample_op_duration_avg;
    raise notice '  max: %', sample_op_duration_max;
    raise notice 'Event queue wait duration:';
    raise notice '  min: %', sample_evt_q_duration_min;
    raise notice '  avg: %', sample_evt_q_duration_avg;
    raise notice '  max: %', sample_evt_q_duration_max;
    raise notice 'Event processing duration:';
    raise notice '  min: %', sample_evt_p_duration_min;
    raise notice '  avg: %', sample_evt_p_duration_avg;
    raise notice '  max: %', sample_evt_p_duration_max;
end;
$$ language plpgsql;
