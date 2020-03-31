from queries.basic import adjacent
from query import graph_query


def reaches(graph, from_name, to_name):
    if adjacent(graph, from_name, to_name):
        return True

    for edge in graph.get_edges_by_source(from_name):
        if reaches(graph, edge.v_to, to_name):
            return True

    return False


@graph_query('n_reaches')
def handle_query_naive_reaches(graph, args):
    return reaches(graph, args.get('from'), args.get('to'))


def members(graph, of):
    result = set()
    for edge in graph.get_edges_by_destination(of):
        v_from = graph.get_vertex(edge.v_from)
        if v_from.type == 'user':
            result.add(v_from.name)
        else:
            result = result.union(members(graph, v_from.name))

    return result


@graph_query('n_members')
def handle_query_naive_members(graph, args):
    return list(members(graph, args.get('of')))
