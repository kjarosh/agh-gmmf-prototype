import os

from flask import Flask

from graph import load_graph

app = Flask(__name__)

graph = load_graph(os.getenv('GRAPH_PATH', 'graph.dat'), os.getenv('ZONE_ID'))
