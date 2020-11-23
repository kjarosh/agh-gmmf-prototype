create or replace view summary as
select
    b.trace,
    b.start_time,
    b.finish_time,
    b.duration,
    started_events = ended_events as finished,
    failed_events = 0 and started_events = ended_events as success,
    failed_events > 0 as failed,
    b.all_events,
    b.started_events,
    b.ended_events,
    b.failed_events,
    b.original_sender,
    b.all_senders
from (select
        trace,
        sum(forkchildren) + 1 as all_events,
        count(id) filter (where type = 'start') as started_events,
        count(id) filter (where type = 'end') as ended_events,
        count(id) filter (where type = 'fail') as failed_events,
        min(time) as start_time,
        max(time) as finish_time,
        (max(time) - min(time)) as duration,
        array_agg(distinct originalsender) as original_sender,
        array_agg(distinct sender) as all_senders
    from dbnotification
    group by trace) as b
order by b.start_time;
