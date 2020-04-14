from app import zone_id
from graph import combine_permissions
from queries.basic import adjacent
from rest import graph_query, query


@graph_query('n_reaches')
def reaches(graph, src, dst):
    src_owner = graph.get_vertex_owner(src)
    if src_owner != zone_id:
        return query(src_owner, 'n_reaches', src=src, dst=dst)['result']

    if adjacent(graph, src, dst):
        return True

    for edge in graph.get_edges_by_source(src):
        if reaches(graph, edge.v_to, dst):
            return True

    return False


@graph_query('n_members')
def members(graph, of):
    owner = graph.get_vertex_owner(of)
    if owner != zone_id:
        return query(owner, 'n_members', of=of)['result']

    result = set()
    for edge in graph.get_edges_by_destination(of):
        v_from = graph.get_vertex(edge.v_from)
        if v_from.type == 'user':
            result.add(v_from.name)
        else:
            result = result.union(members(graph, v_from.name))

    return list(result)


@graph_query('n_eperms')
def effective_permissions(graph, src, dst):
    src_owner = graph.get_vertex_owner(src)
    if src_owner != zone_id:
        return query(src_owner, 'n_eperms', src=src, dst=dst)['result']

    permissions = None
    for edge in graph.get_edges_by_source(src):
        if edge.v_to == dst:
            permissions = combine_permissions(
                permissions, edge.permissions)
        else:
            permissions = combine_permissions(
                permissions, effective_permissions(graph, edge.v_to, dst))

    return permissions
