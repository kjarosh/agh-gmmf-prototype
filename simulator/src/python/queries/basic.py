from app import zone_id
from rest import graph_query, query


@graph_query('is_adj')
def adjacent(graph, src, dst):
    src_owner = graph.get_vertex_owner(src)
    if src_owner != zone_id:
        return query(src_owner, 'is_adj', src=src, dst=dst)['result']

    return dst in [e.v_to for e in graph.get_edges_by_source(src)]


@graph_query('list_adj')
def list_adjacent(graph, of):
    owner = graph.get_vertex_owner(of)
    if owner != zone_id:
        return query(owner, 'list_adj', of=of)['result']

    return [e.v_to for e in graph.get_edges_by_source(of)]


@graph_query('list_adj_rev')
def list_adjacent_reverse(graph, of):
    owner = graph.get_vertex_owner(of)
    if owner != zone_id:
        return query(owner, 'list_adj_rev', of=of)['result']

    return [e.v_from for e in graph.get_edges_by_destination(of)]


@graph_query('perms')
def permissions(graph, src, dst):
    src_owner = graph.get_vertex_owner(src)
    if src_owner != zone_id:
        return query(src_owner, 'perms', src=src, dst=dst)['result']

    for e in graph.get_edges_by_source(src):
        if e.v_to == dst:
            return e.permissions

    return None
