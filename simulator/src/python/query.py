import inspect
import json
import time
import urllib

import requests
from flask import request

from app import graph, app


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
    result = func(graph, *[request.args.get(a) for a in arg_names])
    end = time.time()

    response = QueryResponse()
    response.success = True
    response.result = result
    response.time = end - start
    return response.to_json()


def query(zone_id, q):
    qtype, q = (q + ' ').split(' ', 1)
    q = q.strip()

    params = {}
    for param_pair in q.split(' '):
        if param_pair:
            param_pair = param_pair.split('=', 1)
            params[param_pair[0]] = param_pair[1]

    url_params = urllib.parse.urlencode(params)
    url = 'http://{}/query?type={}&{}'.format(zone_id, qtype.strip(), url_params)
    response = requests.post(url)
    if response.status_code == 200:
        return json.loads(response.text)
    else:
        raise Exception("Query failed: {}".format(response.text))


# noinspection PyUnresolvedReferences
from queries import basic
# noinspection PyUnresolvedReferences
from queries import naive
