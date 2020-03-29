import os

from flask import json

import cli
import tester
from rest import app

companies = [{"id": 1, "name": "Company One"}, {"id": 2, "name": "Company Two"}]


@app.route('/companies', methods=['GET'])
def get_companies():
    return json.dumps(companies)


def main():
    tested_service = os.getenv('TESTED_SERVICE')
    client_mode = os.getenv('CLIENT_MODE')
    if client_mode:
        print("Client mode")
        cli.run_cli()
    elif tested_service:
        print("Running tests...")
        tester.run_tests(tested_service)
    else:
        print("Starting REST application...")
        app.run(host='0.0.0.0', port=80)


if __name__ == '__main__':
    main()
