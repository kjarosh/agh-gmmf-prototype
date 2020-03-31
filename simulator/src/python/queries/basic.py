from query import graph_query


def adjacent(graph, from_name, to_name):
    return to_name in [e.v_to for e in graph.get_edges_by_source(from_name)]


@graph_query('is_adj')
def handle_query_is_adjacent(graph, args):
    return adjacent(graph, args.get('from'), args.get('to'))


def list_adjacent(graph, name):
    return [e.v_to for e in graph.get_edges_by_source(name)]


@graph_query('list_adj')
def handle_query_list_adjacent(graph, args):
    return list_adjacent(graph, args.get('of'))


def list_adjacent_reverse(graph, name):
    return [e.v_from for e in graph.get_edges_by_destination(name)]


@graph_query('list_adj_rev')
def handle_query_list_adjacent_rev(graph, args):
    return list_adjacent_reverse(graph, args.get('of'))


def permissions(graph, from_name, to_name):
    for e in graph.get_edges_by_source(from_name):
        if e.v_to == to_name:
            return e.permissions

    return None


@graph_query('perms')
def handle_query_permissions(graph, args):
    return permissions(graph, args.get('from'), args.get('to'))
