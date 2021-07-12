#!/usr/bin/env python
import json
import sys

if __name__ == '__main__':
    path = sys.argv[1]

    if not path:
        raise Exception("Path expected as first argument")

    graph = json.load(open(path))

    print(f'Number of vertices: {len(graph["vertices"])}')
    print(f'Number of edges: {len(graph["edges"])}')
