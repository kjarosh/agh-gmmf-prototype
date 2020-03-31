from query import query


def assert_equal(a, b):
    if a != b:
        raise AssertionError('Expected {} to be equal to {}'.format(a, b))

    print("Passed")


def assert_true(a):
    if a != True:
        raise AssertionError('Expected {} to be true'.format(a))

    print("Passed")


def assert_false(a):
    if a != False:
        raise AssertionError('Expected {} to be false'.format(a))

    print("Passed")


def run_tests(service):
    assert_true(query(service, 'is_adj from=bob to=datahub')['result'])
    assert_false(query(service, 'is_adj from=bob to=alice')['result'])

    assert_equal(set(query(service, 'list_adj of=uber_admins')['result']),
                 {'admins'})
    assert_equal(set(query(service, 'list_adj of=anne')['result']),
                 {'ceric', 'audit', 'members'})
    assert_equal(set(query(service, 'list_adj of=krakow')['result']),
                 set())

    assert_equal(set(query(service, 'list_adj_rev of=anne')['result']),
                 set())
    assert_equal(set(query(service, 'list_adj_rev of=paris')['result']),
                 {'datahub', 'eo_data', 'eosc'})

    assert_true(query(service, 'n_reaches from=bob to=datahub')['result'])
    assert_false(query(service, 'n_reaches from=bob to=alice')['result'])
    assert_true(query(service, 'n_reaches from=bob to=dhub_members')['result'])
    assert_true(query(service, 'n_reaches from=luke to=krakow')['result'])
    assert_false(query(service, 'n_reaches from=anne to=lisbon')['result'])

    assert_equal(query(service, 'perms from=alice to=bob')['result'],
                 None)
    assert_equal(query(service, 'perms from=alice to=ebi')['result'],
                 '11000')
    assert_equal(query(service, 'perms from=audit to=cyfnet')['result'],
                 '11001')

    assert_equal(set(query(service, 'n_members of=admins')['result']),
                 {'luke'})
    assert_equal(set(query(service, 'n_members of=eo_data')['result']),
                 {'luke', 'bob', 'alice'})
