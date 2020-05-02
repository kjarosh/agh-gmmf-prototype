from typing import Dict

from algo.model import IndexedVertex
from app import zone_id
from rest import graph_query, query


@graph_query('reaches')
def reaches(graph, src, dst):
    return effective_permissions(graph, src, dst) is not None


@graph_query('members')
def members(graph, of):
    owner = graph.get_vertex_owner(of)
    if owner != zone_id:
        return query(owner, 'members', of=of)['result']

    raise Exception('Not implemented')


@graph_query('eperms')
def effective_permissions(graph, src, dst):
    src_owner = graph.get_vertex_owner(src)
    if src_owner != zone_id:
        return query(src_owner, 'eperms', src=src, dst=dst)['result']

    index: Dict[str, IndexedVertex] = graph.get_vertex(src).ix

    if dst in index:
        return index[dst]

    return None
