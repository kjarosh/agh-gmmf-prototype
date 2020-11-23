select
    trace,
    sum(forkchildren) + 1 as all_events,
    count(id) filter (where type = 'fail') as failed_events,
    count(id) filter (where type = 'end') as ended_events,
    min(time) as started,
    max(time) as finished,
    (max(time) - min(time)) as duration
from dbnotification
group by trace
