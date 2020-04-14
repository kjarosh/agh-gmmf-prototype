import os

from flask import Flask

from graph import load_graph

app = Flask(__name__)

zone_id = os.getenv('ZONE_ID')

__graph = None


def get_graph():
    global __graph
    if __graph is None:
        __graph = load_graph(os.getenv('GRAPH_PATH', 'graph.dat'), zone_id)
    return __graph
