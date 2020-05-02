import json

import requests

from algo.model import IndexedVertex
from app import app
from graph import Graph


@app.route('index', methods=['POST'])
def handle_index(graph: Graph):
    to_index = set(graph.get_all_vertices())

    while len(to_index) > 0:
        x = list(to_index)[0]
        to_index.remove(x)
        propagate(graph, x, set())

    raise Exception('Not implemented')


def index(zone):
    url = 'http://{}/index'.format(zone)
    print('Indexing {}'.format(url))
    response = requests.post(url)
    if response.status_code == 200:
        return json.loads(response.text)
    else:
        raise Exception('Query failed: {}'.format(response.text))


def propagate(graph, source, trail: set):
    outgoing = graph.get_edges_by_source(source)
    for edge in outgoing:
        v_to = graph.get_vertex(edge.v_to)
        found_new = False
        for other in trail:
            if other not in v_to.ix:
                v_to.ix[other] = IndexedVertex()

            indexed_vertex: IndexedVertex = v_to.ix[other]
            intermediate_vertices = indexed_vertex.intermediate_vertices
            if edge.v_to not in intermediate_vertices:
                intermediate_vertices.append(edge.v_to)
                found_new = True

        if not found_new:
            continue

        new_trail = set(trail)
        new_trail.add(edge.v_to)
        propagate(graph, edge.v_to, new_trail)
