from query import query

test_passed = True


def assert_equal(a, b):
    if a != b:
        fail_tests('Expected {} to be equal to {}'.format(a, b))

    print("Passed")


def assert_true(a):
    if a != True:
        fail_tests('Expected {} to be true'.format(a))

    print("Passed")


def assert_false(a):
    if a != False:
        fail_tests('Expected {} to be false'.format(a))

    print("Passed")


def fail_tests(msg):
    global test_passed
    test_passed = False
    print('! {}'.format(msg))


def run_tests(zone_id):
    global test_passed
    test_passed = True

    test_is_adj(zone_id)
    test_list_adj(zone_id)
    test_perms(zone_id)

    test_n_reaches(zone_id)
    test_n_members(zone_id)
    test_n_eperms(zone_id)

    if not test_passed:
        raise AssertionError('Failed tests')


def test_is_adj(zone_id):
    assert_true(query(zone_id, 'is_adj from=bob to=datahub')['result'])
    assert_false(query(zone_id, 'is_adj from=bob to=alice')['result'])


def test_list_adj(zone_id):
    assert_equal(set(query(zone_id, 'list_adj of=uber_admins')['result']),
                 {'admins'})
    assert_equal(set(query(zone_id, 'list_adj of=anne')['result']),
                 {'ceric', 'audit', 'members'})
    assert_equal(set(query(zone_id, 'list_adj of=krakow')['result']),
                 set())

    assert_equal(set(query(zone_id, 'list_adj_rev of=anne')['result']),
                 set())
    assert_equal(set(query(zone_id, 'list_adj_rev of=paris')['result']),
                 {'datahub', 'eo_data', 'eosc'})


def test_perms(zone_id):
    assert_equal(query(zone_id, 'perms from=alice to=bob')['result'],
                 None)
    assert_equal(query(zone_id, 'perms from=alice to=ebi')['result'],
                 '11000')
    assert_equal(query(zone_id, 'perms from=audit to=cyfnet')['result'],
                 '11001')
    assert_equal(query(zone_id, 'perms from=audit to=eosc')['result'],
                 None)


def test_n_reaches(zone_id):
    assert_true(query(zone_id, 'n_reaches from=bob to=datahub')['result'])
    assert_false(query(zone_id, 'n_reaches from=bob to=alice')['result'])
    assert_true(query(zone_id, 'n_reaches from=bob to=dhub_members')['result'])
    assert_true(query(zone_id, 'n_reaches from=luke to=krakow')['result'])
    assert_false(query(zone_id, 'n_reaches from=anne to=lisbon')['result'])
    assert_true(query(zone_id, 'n_reaches from=luke to=dhub_members')['result'])


def test_n_members(zone_id):
    assert_equal(set(query(zone_id, 'n_members of=admins')['result']),
                 {'luke'})
    assert_equal(set(query(zone_id, 'n_members of=eo_data')['result']),
                 {'luke', 'bob', 'alice'})


def test_n_eperms(zone_id):
    assert_equal(query(zone_id, 'n_eperms from=alice to=bob')['result'],
                 None)
    assert_equal(query(zone_id, 'n_eperms from=alice to=ebi')['result'],
                 '11000')
    assert_equal(query(zone_id, 'n_eperms from=audit to=cyfnet')['result'],
                 '11001')
    assert_equal(query(zone_id, 'n_eperms from=audit to=eosc')['result'],
                 '11001')
    assert_equal(query(zone_id, 'n_eperms from=tom to=primage')['result'],
                 '11011')
