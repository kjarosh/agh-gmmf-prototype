import os

from flask import json

import cli
import tester
from app import app

companies = [{"id": 1, "name": "Company One"}, {"id": 2, "name": "Company Two"}]


@app.route('/companies', methods=['GET'])
def get_companies():
    return json.dumps(companies)


def main():
    tested_zone = os.getenv('TESTED_ZONE')
    client_mode = os.getenv('CLIENT_MODE')
    if client_mode:
        print("Client mode")
        cli.run_cli()
    elif tested_zone:
        print("Running tests...")
        tester.run_tests(tested_zone)
        print("Tests passed!")
    else:
        print("Starting REST application...")
        app.run(host='0.0.0.0', port=80)


if __name__ == '__main__':
    main()
