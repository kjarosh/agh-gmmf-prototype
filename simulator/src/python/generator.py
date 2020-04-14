import random
from typing import Dict, List, Set

import slug as slug
import toml
from faker import Faker

from graph import Graph, Vertex, Edge, GraphOperation, GraphOperationType


def choose_category(shares_by_category):
    shares = [s for (_, s) in shares_by_category]
    categories = [c for (c, _) in shares_by_category]
    val = random.random()
    probabilities = [s / sum(shares) for s in shares]

    for i, p in enumerate(probabilities):
        if val < p:
            return categories[i]
        else:
            val -= p

    return categories[-1]


def generate_permissions():
    return ''.join(['0' if random.random() < 0.5 else '1' for _ in range(5)])


def random_zone(zone_count):
    return 'zone_{}'.format(random.randrange(0, zone_count))


class EntityGenerator:
    def __init__(self, config) -> None:
        self.config = config
        self.fake = Faker()
        self.generated_providers = {}
        self.generated_spaces = {}
        self.generated_groups = {}
        self.generated_users = {}

    def gen_entity_type(self):
        return choose_category([
            ('provider', self.config['type']['provider']['share']),
            ('space', self.config['type']['space']['share']),
            ('group', self.config['type']['group']['share']),
            ('user', self.config['type']['user']['share'])
        ])

    def gen_entity_name(self, entity_type):
        if entity_type == 'provider':
            return self.__gen_provider_name()
        elif entity_type == 'space':
            return self.__gen_space_name()
        elif entity_type == 'group':
            return self.__gen_group_name()
        else:
            return self.__gen_user_name()

    def __assert_unique(self, generated, pool):
        if generated in pool:
            pool[generated] += 1
            count = pool[generated]
            return '{}-{}'.format(generated, count)
        else:
            pool[generated] = 1
            return generated

    def __gen_provider_name(self):
        generated = slug.slug(self.fake.city())
        return self.__assert_unique(generated, self.generated_providers)

    def __gen_space_name(self):
        generated = slug.slug(self.fake.street_name())
        return self.__assert_unique(generated, self.generated_spaces)

    def __gen_group_name(self):
        generated = self.fake.slug()
        return self.__assert_unique(generated, self.generated_groups)

    def __gen_user_name(self):
        generated = slug.slug(self.fake.user_name())
        return self.__assert_unique(generated, self.generated_users)


class RelationGenerator:
    def __init__(self, config) -> None:
        self.config = config
        self.generated_edges = set({})

    def generate_edge(self, vertices_by_type):
        edge = self.__generate_edge0(vertices_by_type)
        if edge is None:
            return self.generate_edge(vertices_by_type)

        fingerprint = (edge.v_from, edge.v_to)
        if fingerprint[0] == fingerprint[1] or fingerprint in self.generated_edges:
            return self.generate_edge(vertices_by_type)
        self.generated_edges.add(fingerprint)

        return edge

    def __generate_edge0(self, vertices):
        vertices_by_zone_type: Dict[str, Dict[str, List[Vertex]]] = {}
        vertices_by_type: Dict[str, List[Vertex]] = {
            'provider': [],
            'space': [],
            'group': [],
            'user': []
        }
        for vertex in vertices:
            zone = vertex.zone
            v_type = vertex.type
            if zone not in vertices_by_zone_type:
                vertices_by_zone_type[zone] = {
                    'provider': [],
                    'space': [],
                    'group': [],
                    'user': []
                }

            vertices_by_type[v_type].append(vertex)
            vertices_by_zone_type[zone][v_type].append(vertex)

        relation_type = choose_category([
            (('space', 'provider'), self.config['relation']['space_provider']['share']),
            (('group', 'space'), self.config['relation']['group_space']['share']),
            (('group', 'group'), self.config['relation']['group_group']['share']),
            (('user', 'group'), self.config['relation']['user_group']['share']),
            (('user', 'space'), self.config['relation']['user_space']['share']),
        ])

        inter_zone = random.random() < self.config['relation']['inter_zone']['prob']
        zone_a = random_zone(self.config['count']['zone'])
        zone_b = random_zone(self.config['count']['zone']) if inter_zone else zone_a

        # if entity types are different,
        # it's not possible to make a cycle
        if relation_type[0] != relation_type[1]:
            edge = Edge()
            from_vx = vertices_by_zone_type[zone_a][relation_type[0]]
            to_vx = vertices_by_zone_type[zone_b][relation_type[1]]
            if len(from_vx) == 0 or len(to_vx) == 0:
                return None

            edge.v_from = from_vx[random.randrange(0, len(from_vx))].name
            edge.v_to = to_vx[random.randrange(0, len(to_vx))].name
            if relation_type[1] != 'provider':
                edge.permissions = generate_permissions()
            return edge

        vx = vertices_by_type[relation_type[0]]
        ix = sorted([random.randrange(0, len(vx)), random.randrange(0, len(vx))])
        edge = Edge()
        edge.v_from = vx[ix[0]].name
        edge.v_to = vx[ix[1]].name
        edge.permissions = generate_permissions()
        return edge


class Generator:
    def __init__(self, config) -> None:
        self.config = config
        self.entity_generator = EntityGenerator(config)
        self.relation_generator = RelationGenerator(config)

    def gen_is_relation_inter_zone(self):
        prob = self.config['feature']['inter_zone_relation']['prob']
        return random.random() < prob

    def generate_graph(self):
        vertices = []
        edges = []

        vertex_count = self.config['count']['entity']
        edge_count = self.config['count']['relation']

        vertices.append(self.__generate_vertex_type('provider'))
        vertices.append(self.__generate_vertex_type('space'))
        vertices.append(self.__generate_vertex_type('group'))
        vertices.append(self.__generate_vertex_type('user'))
        for i in range(vertex_count - 4):
            v = self.__generate_vertex()
            vertices.append(v)

        for i in range(edge_count):
            e = self.relation_generator.generate_edge(vertices)
            edges.append(e)

        return Graph(vertices, edges)

    def __generate_vertex(self):
        return self.__generate_vertex_type(self.entity_generator.gen_entity_type())

    def __generate_vertex_type(self, v_type):
        v = Vertex()
        v.type = v_type
        v.name = self.entity_generator.gen_entity_name(v.type)
        v.zone = random_zone(self.config['count']['zone'])
        return v


class GraphOperationGenerator:
    def __init__(self, graph: Graph, config) -> None:
        self.graph = graph
        self.config = config

    def generate_edge_operations(self):
        ops = []
        current_edges: Set[Edge] = set()
        remaining_edges: Set[Edge] = set(self.graph.get_all_edges())
        deletion_prob = self.config['relation']['deletion']['prob']

        while len(current_edges) != self.graph.edge_count():
            is_deletion = len(current_edges) > 0 and \
                          deletion_prob > random.random()

            if is_deletion:
                to_delete: Edge = random.choice(list(current_edges))
                op = GraphOperation()
                op.type = GraphOperationType.DELETE_RELATION
                op.arg = to_delete
                current_edges.remove(to_delete)
                remaining_edges.add(to_delete)
                ops.append(op)
            else:
                to_add: Edge = random.choice(list(remaining_edges))
                op = GraphOperation()
                op.type = GraphOperationType.ADD_RELATION
                op.arg = to_add
                current_edges.add(to_add)
                remaining_edges.remove(to_add)
                ops.append(op)

        return ops


def main():
    config = toml.load('config.toml')

    g = Generator(config)
    graph = g.generate_graph()

    with open("generated_graph.gml", "w+") as f:
        f.write(graph.to_gml())

    with open("generated_graph.dat", "w+") as f:
        f.write(graph.to_dat())

    op_gen = GraphOperationGenerator(graph, config)
    edge_ops = op_gen.generate_edge_operations()
    with open("generated_edge_operations.dat", "w+") as f:
        for edge_op in edge_ops:
            f.write('{} {}\n'.format(edge_op.type.name, str(edge_op.arg)))


if __name__ == '__main__':
    main()
