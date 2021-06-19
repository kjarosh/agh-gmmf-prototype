create or replace function report(st timestamp, et timestamp) returns void as $$
declare
	total_ops int;
	total_events int;
	total_duration interval;
	total_duration_sec double precision;

	sample_duration interval;
	sample_duration_sec double precision;
	sample_ops int;
    sample_q_ops int;
	sample_ops_finished int;
	sample_ops_succeeded int;
    sample_ops_failed int;
	sample_events int;
    sample_events_queued int;
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
        raise exception 'start time is after end time';
    end if;
    select count(*) into total_ops from operations;
	select count(*) into total_events from events;
	select max(start_time) - min(start_time) into total_duration from operations;
	select extract(epoch from total_duration) into total_duration_sec;

    raise notice 'Operations: %', total_ops;
    raise notice 'Events: %', total_events;
    raise notice 'Notifications: %', (select count(*) from dbnotification);
    raise notice 'Time between first / last operation: % (% s)', total_duration, total_duration_sec;
    raise notice 'Operations per second: %', total_ops / total_duration_sec;
    raise notice 'Events per second: %', total_events / total_duration_sec;
    raise notice '================================================================================';

    select et - st into sample_duration;
	select extract(epoch from sample_duration) into sample_duration_sec;

    raise notice 'Start time: %', st;
    raise notice 'End time: %', et;
    raise notice 'Duration: % (% s)', sample_duration, sample_duration_sec;

	select count(*) into sample_ops from operations
	    where start_time > st and start_time <= et;
    select count(*) into sample_q_ops from operations
	    where queued_time > st and queued_time <= et;
	select count(*) into sample_ops_finished from operations
		where start_time > st and start_time <= et and finished;
	select count(*) into sample_ops_succeeded from operations
		where start_time > st and start_time <= et and success and finished;
    select count(*) into sample_ops_failed from operations
        where start_time > st and start_time <= et and failed;

    raise notice 'Operations: %', sample_ops;
    raise notice 'Operations queued: %', sample_q_ops;
    raise notice 'Operations finished: %', sample_ops_finished;
    raise notice 'Operations succeeded: %', sample_ops_succeeded;
    raise notice 'Operations failed: %', sample_ops_failed;
    raise notice 'Operations per second: %', sample_ops_finished / sample_duration_sec;
    raise notice 'Queued operations per second: %', sample_q_ops / sample_duration_sec;

    select count(*) into sample_events from events
        where start_time > st and start_time <= et;
    select count(*) into sample_events_queued from events
        where queued_time > st and queued_time <= et;
    select count(*) into sample_events_started from events
        where start_time > st and start_time <= et;
    select count(*) into sample_events_ended from events
        where end_time > st and end_time <= et;

    raise notice 'Events: %', sample_events;
    raise notice 'Events queued: %', sample_events_queued;
    raise notice 'Events started: %', sample_events_started;
    raise notice 'Events ended: %', sample_events_ended;
    raise notice 'Events per second: %', sample_events_ended / sample_duration_sec;
    raise notice 'Queued events per second: %', sample_events_queued / sample_duration_sec;

	select min(duration) into sample_op_duration_min from operations
		where start_time > st and start_time <= et;
	select avg(duration) into sample_op_duration_avg from operations
		where start_time > st and start_time <= et;
	select max(duration) into sample_op_duration_max from operations
		where start_time > st and start_time <= et;

	select min(wait_duration) into sample_evt_q_duration_min from events
		where start_time > st and start_time <= et;
	select avg(wait_duration) into sample_evt_q_duration_avg from events
		where start_time > st and start_time <= et;
	select max(wait_duration) into sample_evt_q_duration_max from events
		where start_time > st and start_time <= et;

	select min(duration) into sample_evt_p_duration_min from events
		where start_time > st and start_time <= et;
	select avg(duration) into sample_evt_p_duration_avg from events
		where start_time > st and start_time <= et;
	select max(duration) into sample_evt_p_duration_max from events
		where start_time > st and start_time <= et;

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




create or replace function time_plot(st timestamp, et timestamp, step interval) returns void as $$
declare
    i int;
    points int;
    interval_sec real;
    step_sec real;

    point_int interval;
	point_int_sec real;
    point_st timestamp;
    point_et timestamp;

    sample_events_ended real;
    sample_q_ops real;
    sample_events_queued real;
    sample_ops_finished real;
begin
    if st >= et then
        raise exception 'start time if after end time';
    end if;

    select extract(epoch from et - st) into interval_sec;
    select extract(epoch from step) into step_sec;
    select floor(interval_sec / step_sec) into points;

    raise notice 'plotting % points, step=%, interval=%', points, step, et-st;
    raise notice ',time,dt,queued,operations,queued_events,events';

    for i in 1..points loop
        select (et - st) * i / points into point_int;
        select st + point_int into point_et;
        select point_et - step into point_st;

    	select extract(epoch from point_int) into point_int_sec;

        select count(*) into sample_events_ended from events
            where end_time > point_st and end_time <= point_et;
        select count(*) into sample_q_ops from operations
            where queued_time > point_st and queued_time <= point_et;
        select count(*) into sample_events_queued from events
            where queued_time > point_st and queued_time <= point_et;
        select count(*) into sample_ops_finished from operations
            where start_time > point_st and start_time <= point_et and finished;

        raise notice ',%,%,%,%,%,%',
            point_st,
			point_int_sec,
            sample_q_ops / step_sec,
            sample_ops_finished / step_sec,
            sample_events_queued / step_sec,
            sample_events_ended / step_sec;
    end loop;
end;
$$ language plpgsql;
