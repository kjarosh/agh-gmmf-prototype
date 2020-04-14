import traceback
from cmd import Cmd

from rest import query, add_entity


class CliContext:
    used_service: str

    def __init__(self) -> None:
        self.used_service = 'unknown'


class _DynamicStr:
    def __init__(self, func) -> None:
        self.func = func

    def __str__(self) -> str:
        return self.func()


def __parse_cli_params(line):
    params = {}
    for param_pair in line.split(' '):
        if param_pair:
            param_pair = param_pair.split('=', 1)
            params[param_pair[0]] = param_pair[1]
    return params


class CmdHandler(Cmd):
    def __init__(self, context):
        super().__init__()
        self.context = context
        self.prompt = _DynamicStr(lambda: self.context.used_service + ' > ')

    def do_query(self, line):
        try:
            cmd, line = (line + ' ').split(' ')
            params = __parse_cli_params(line)
            response = query(self.context.used_service, cmd, **params)
            print(response)
        except KeyboardInterrupt:
            print("Interrupted")
        except Exception:
            traceback.print_exc()
            print("Exception occurred")

    def do_add_entity(self, line):
        try:
            params = __parse_cli_params(line)
            response = add_entity(self.context.used_service, params['name'], params['type'])
            print(response)
        except KeyboardInterrupt:
            print("Interrupted")
        except Exception:
            traceback.print_exc()
            print("Exception occurred")

    def do_use(self, line):
        self.context.used_service = line.strip()

    def do_print_graph(self, line):
        from app import get_graph
        print(get_graph())


def run_cli():
    context = CliContext()
    CmdHandler(context).cmdloop()


if __name__ == '__main__':
    run_cli()
