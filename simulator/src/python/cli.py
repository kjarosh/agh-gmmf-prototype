from query import query


class CliContext:
    used_service: str

    def __init__(self) -> None:
        self.used_service = 'unknown'


def run_cli():
    context = CliContext()
    try:
        while True:
            query_line = input(context.used_service + " > ").strip()
            if query_line:
                process_query(context, query_line)
    except KeyboardInterrupt:
        print("Interrupted")


def process_query(context, query_line):
    (command, rest) = (query_line + ' ').split(' ', 1)
    rest = rest.strip()

    if command == 'use':
        context.used_service = rest
    elif command == 'query' or command == 'q':
        print(query(context.used_service, rest))
    else:
        print("Unknown command: {}".format(command))
