import requests
from flask import request

from rest import app


class QueryResponse:
    time: str
    result: object


@app.route('/query', methods=['GET'])
def get_companies():
    q = request.query_string
    print(q)


def query(service, q):
    url = 'http://{}/query?{}'.format(service, q)
    return requests.get(url)
