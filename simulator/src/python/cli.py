import traceback
from cmd import Cmd

from query import query


class CliContext:
    used_service: str

    def __init__(self) -> None:
        self.used_service = 'unknown'


class _DynamicStr:
    def __init__(self, func) -> None:
        self.func = func

    def __str__(self) -> str:
        return self.func()


class CmdHandler(Cmd):
    def __init__(self, context):
        super().__init__()
        self.context = context
        self.prompt = _DynamicStr(lambda: self.context.used_service + ' > ')

    def do_query(self, line):
        try:
            response = query(self.context.used_service, line)
            print(response)
        except KeyboardInterrupt:
            print("Interrupted")
        except Exception:
            traceback.print_exc()
            print("Exception occurred")

    def do_use(self, line):
        self.context.used_service = line.strip()

    def do_print_graph(self, line):
        from main import graph
        print(graph)


def run_cli():
    context = CliContext()
    CmdHandler(context).cmdloop()


def process_query(context, query_line):
    (command, rest) = (query_line + ' ').split(' ', 1)
    rest = rest.strip()

    if command == 'use':
        context.used_service = rest
    elif command == 'query' or command == 'q':
        response = query(context.used_service, rest)
        print(response)
    else:
        print("Unknown command: {}".format(command))


if __name__ == '__main__':
    run_cli()
