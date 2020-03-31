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


def adjacent(graph, from_name, to_name):
    return to_name in [e.v_to for e in graph.get_edges_by_source(from_name)]


@graph_query('n_adj')
def handle_query_naive_adjacent(graph, args):
    return adjacent(graph, args.get('from'), args.get('to'))
