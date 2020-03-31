from query import query


def run_tests(service):
    assert query(service, 'n_adj from=bob to=datahub')['result'] == True
    assert query(service, 'n_adj from=bob to=alice')['result'] == False

    assert query(service, 'n_reaches from=bob to=datahub')['result'] == True
    assert query(service, 'n_reaches from=bob to=alice')['result'] == False
    assert query(service, 'n_reaches from=bob to=dhub_members')['result'] == True
    assert query(service, 'n_reaches from=luke to=krakow')['result'] == True
    assert query(service, 'n_reaches from=anne to=lisbon')['result'] == False
