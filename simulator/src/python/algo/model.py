from typing import List


class IndexedVertex:
    effective_perms: str
    intermediate_vertices: List[str]

    def __init__(self) -> None:
        self.effective_perms = ''
        self.intermediate_vertices = []


