import re
from typing import Dict, List, Optional

from parse import parse


def combine_permissions(perm_a, perm_b):
    if perm_a is None:
        return perm_b
    if perm_b is None:
        return perm_a
    return ''.join(['1' if a == '1' or b == '1' else '0' for (a, b) in zip(perm_a, perm_b)])


class Vertex:
    name: str
    type: str
    zone: str

    def __str__(self) -> str:
        return '{} : {} : {}'.format(self.name, self.type, self.zone)


class Edge:
    v_from: str
    v_to: str
    permissions: Optional[str]

    def __init__(self) -> None:
        self.v_from = ''
        self.v_to = ''
        self.permissions = None

    def reduce(self, other):
        if other.v_from != self.v_to:
            return None

        reduced = Edge()
        reduced.v_from = self.v_from
        reduced.v_to = other.v_to
        reduced.permissions = combine_permissions(
            self.permissions, other.permissions)

    def __str__(self) -> str:
        return '{} -> {} ({})'.format(self.v_from, self.v_to, self.permissions)


class Graph:
    __vertices: Dict[str, Vertex]
    __edges: [Edge]
    __edges_by_src: Dict[str, List[Edge]]
    __edges_by_dst: Dict[str, List[Edge]]

    def __init__(self, vertices, edges) -> None:
        self.__edges = edges
        self.__vertices = {}
        self.__edges_by_src = {}
        self.__edges_by_dst = {}

        for vertex in vertices:
            self.__vertices[vertex.name] = vertex

        for edge in edges:
            if edge.v_from in self.__edges_by_src:
                self.__edges_by_src[edge.v_from].append(edge)
            else:
                self.__edges_by_src[edge.v_from] = [edge]

            if edge.v_to in self.__edges_by_dst:
                self.__edges_by_dst[edge.v_to].append(edge)
            else:
                self.__edges_by_dst[edge.v_to] = [edge]

    def __str__(self) -> str:
        vx = ''
        eg = ''
        for vertex in self.__vertices.values():
            vx += '  ' + str(vertex) + '\n'
        for edge in self.__edges:
            eg += '  ' + str(edge) + '\n'
        return 'Vertices:\n' + vx + 'Edges:\n' + eg

    def get_vertex(self, name: str) -> Vertex:
        return self.__vertices[name]

    def get_edges_by_source(self, source: str) -> [Vertex]:
        if source in self.__edges_by_src:
            return self.__edges_by_src[source]
        else:
            return []

    def get_edges_by_destination(self, destination: str) -> [Vertex]:
        if destination in self.__edges_by_dst:
            return self.__edges_by_dst[destination]
        else:
            return []

    def to_gml(self):
        nodes = ''
        edges = ''
        for v in self.__vertices.values():
            nodes += '  node [ id "{}" label "{} {}" ]\n'.format(v.name, v.name, v.type)

        for e in self.__edges:
            edges += '  edge [ source "{}" target "{}" label "{}" ]\n'.format(e.v_from, e.v_to, e.permissions)

        return 'graph [\n{}{}]\n'.format(nodes, edges)

    def to_dat(self):
        nodes = ''
        edges = ''

        for v in self.__vertices.values():
            nodes += '{}\n'.format(v)

        for e in self.__edges:
            edges += '{}\n'.format(e)

        return '{}\n------\n\n{}'.format(nodes, edges)


def load_graph(file):
    vertices = []
    edges = []
    with open(file) as f:
        for vertex_line in f:
            if re.match("[-]+", vertex_line): break
            if parse('{}:{}:{}', vertex_line) is None: continue

            v_name, v_type, v_zone = parse('{}:{}:{}', vertex_line)
            v = Vertex()
            v.name = v_name.strip()
            v.type = v_type.strip()
            v.zone = v_zone.strip()
            vertices.append(v)

        for edge_line in f:
            if parse('{}->{}({})', edge_line) is not None:
                (v_from, v_to, permissions) = parse('{}->{}({})', edge_line)
                permissions = permissions.strip()
            elif parse('{}->{}', edge_line) is not None:
                (v_from, v_to) = parse('{}->{}', edge_line)
                permissions = None
            else:
                continue

            e = Edge()
            e.v_from = v_from.strip()
            e.v_to = v_to.strip()
            e.permissions = permissions
            edges.append(e)

    return Graph(vertices, edges)
