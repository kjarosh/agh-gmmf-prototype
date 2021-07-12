drop materialized view if exists operations;
create materialized view operations as
select
    b.trace,
    b.queued_time,
    b.start_time,
    b.finish_time,
    b.duration,
    all_events = started_events and started_events = ended_events + failed_events as finished,
    failed_events = 0 and started_events = ended_events as success,
    failed_events > 0 as failed,
    b.all_events,
    b.queued_events,
    b.started_events,
    b.ended_events,
    b.failed_events,
    b.original_sender,
--     b.all_senders,
--     b.all_participants,
    b.zone_participants
from (
    select
        trace,
        sum(forkchildren) + 2 as all_events,
        count(id) filter (where type = 'queue') as queued_events,
        count(id) filter (where type = 'start') as started_events,
        count(id) filter (where type = 'end') as ended_events,
        count(id) filter (where type = 'fail') as failed_events,
        min(time) filter (where type = 'queue') as queued_time,
        min(time) filter (where type = 'start') as start_time,
        max(time) filter (where type = 'end') as finish_time,
        (max(time) - min(time)) as duration,
        array_agg(distinct originalsender) as original_sender,
--         array_agg(distinct sender) as all_senders,
--         array_agg(distinct vertex) as all_participants,
        array_agg(distinct zone) as zone_participants
    from dbnotification
    where type in ('start', 'end', 'fail', 'fork', 'queue')
    group by trace) as b
order by b.start_time;
create index operations_ix_1
    on operations (start_time);
create index operations_ix_2
    on operations (finished);
create index operations_ix_3
    on operations (success);

drop materialized view if exists events;
create materialized view events as
select
    b.zone,
    b.eventid,
    b.trace,
    b.vertex,
    (b.start_time - b.queued_time) as wait_duration,
    (b.end_time - b.start_time) as duration,
    b.queued_time,
    b.start_time,
    b.end_time
from (
    select
        zone,
        eventid,
        trace,
        vertex,
        min(time) filter (where type = 'queue') as queued_time,
        min(time) filter (where type = 'start') as start_time,
        max(time) filter (where type = 'end') as end_time
    from dbnotification
    where type in ('start', 'end', 'queue')
    group by zone, eventid, trace, vertex) as b
order by b.start_time;
create index events_ix_1
    on events (start_time);


create or replace view stats as
select
    b.name,
    b.value
from (
    select
        'time in queue / average' as name,
        cast(avg(wait_duration) as text) as value
    from events
    union
    select
        'time in queue / min' as name,
        cast(min(wait_duration) as text) as value
    from events
    union
    select
        'time in queue / max' as name,
        cast(max(wait_duration) as text) as value
    from events
    union
    select
        'time processing / average' as name,
        cast(avg(duration) as text) as value
    from operations
    union
    select
        'time processing / min' as name,
        cast(min(duration) as text) as value
    from operations
    union
    select
        'time processing / max' as name,
        cast(max(duration) as text) as value
    from operations
    union
    select
        'events failed / count' as name,
        cast(sum(failed_events) as text) as value
    from operations
    union
    select
        'events started / count' as name,
        cast(sum(started_events) as text) as value
    from operations
    union
    select
        'events queued / count' as name,
        cast(sum(queued_events) as text) as value
    from operations) as b
order by b.name;
