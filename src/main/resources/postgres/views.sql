create or replace view event_processing_summary as
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
    b.all_senders,
    b.all_participants,
    b.zone_participants
from (
    select
        trace,
        sum(forkchildren) + 1 as all_events,
        count(id) filter (where type = 'start') as started_events,
        count(id) filter (where type = 'end') as ended_events,
        count(id) filter (where type = 'fail') as failed_events,
        min(time) as start_time,
        max(time) as finish_time,
        (max(time) - min(time)) as duration,
        array_agg(distinct originalsender) as original_sender,
        array_agg(distinct sender) as all_senders,
        array_agg(distinct vertex) as all_participants,
        array_agg(distinct zone) as zone_participants
    from dbnotification
    where type in ('start', 'end', 'fail', 'fork')
    group by trace) as b
order by b.start_time;

create or replace view queue_summary as
select
    b.zone,
    b.eventid,
    b.trace,
    b.vertex,
    (b.start_time - b.queued_time) as wait_duration,
    b.queued_time,
    b.start_time,
    b.queued_events,
    b.started_events
from (
    select
        zone,
        eventid,
        trace,
        vertex,
        min(time) filter (where type = 'queue') as queued_time,
        max(time) filter (where type = 'start') as start_time,
        count(id) filter (where type = 'queue') as queued_events,
        count(id) filter (where type = 'start') as started_events
    from dbnotification
    where type in ('start', 'queue')
    group by zone, eventid, trace, vertex) as b
order by b.start_time;
