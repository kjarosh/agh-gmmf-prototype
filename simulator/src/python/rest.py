import inspect
import json
import time
import urllib

import requests
from flask import request

from app import get_graph, app, zone_id
from graph import Vertex, Edge


class QueryResponse:
    success: bool
    error: str
    time: str
    result: object

    def to_json(self):
        return json.dumps(self.__dict__)


queries = {}


def graph_query(query_type):
    def inner(func):
        queries[query_type] = func
        return func

    return inner


@app.route('/query', methods=['POST'])
def handle_query():
    query_type = request.args.get('type')

    if query_type not in queries:
        response = QueryResponse()
        response.success = False
        response.result = None
        response.error = 'Unknown query type'
        return response.to_json()

    start = time.time()
    func = queries[query_type]
    argspec = inspect.getfullargspec(func)
    arg_names = argspec[0][1:]
    result = func(get_graph(), *[request.args.get(a) for a in arg_names])
    end = time.time()

    response = QueryResponse()
    response.success = True
    response.result = result
    response.time = end - start
    return response.to_json()


@app.route('/entities', methods=['POST'])
def handle_add_entity():
    v_name = request.args.get('name')
    v_type = request.args.get('type')

    if v_type not in {'provider', 'user', 'group', 'space'}:
        raise Exception('Wrong type: {}'.format(v_type))

    v = Vertex()
    v.zone = zone_id
    v.name = v_name
    v.type = v_type
    get_graph().add_vertex(v)


@app.route('/relations', methods=['POST'])
def handle_add_relation():
    e_from = request.args.get('from')
    e_to = request.args.get('to')
    e_perms = request.args.get('permissions')

    if get_graph().get_vertex(e_from) is None:
        raise Exception('Unknown vertex: {}'.format(e_from))

    if get_graph().get_vertex_owner(e_to) is None:
        raise Exception('Unknown vertex owner: {}'.format(e_to))

    e = Edge()
    e.v_from = e_from
    e.v_to = e_to
    e.permissions = e_perms
    get_graph().add_edge(e)


def _parse_params(params_str):
    params = {}
    for param_pair in params_str.split(' '):
        if param_pair:
            param_pair = param_pair.split('=', 1)
            params[param_pair[0]] = param_pair[1]
    return params


def query(zone, qtype, **other_params) -> dict:
    url_params = urllib.parse.urlencode(other_params)
    url = 'http://{}/query?type={}&{}'.format(zone, qtype.strip(), url_params)
    print('Querying {}'.format(url))
    response = requests.post(url)
    if response.status_code == 200:
        return json.loads(response.text)
    else:
        raise Exception('Query failed: {}'.format(response.text))


def add_entity(v_zone, v_name, v_type):
    url = 'http://{}/entity?name={}&type={}'.format(v_zone, v_name, v_type)
    response = requests.post(url)
    if response.status_code == 200:
        return json.loads(response.text)
    else:
        raise Exception('Query failed: {}'.format(response.text))


# noinspection PyUnresolvedReferences
from queries import basic
# noinspection PyUnresolvedReferences
from queries import naive
# noinspection PyUnresolvedReferences
from algo import queries as algo_queries
