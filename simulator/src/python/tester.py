import inspect

from query import query

test_failed = 0


def assert_equal(a, b):
    if a != b:
        fail_test('Expected {} to be equal to {}'.format(a, b))
    else:
        pass_test()


def assert_true(a):
    if a != True:
        fail_test('Expected {} to be true'.format(a))
    else:
        pass_test()


def assert_false(a):
    if a != False:
        fail_test('Expected {} to be false'.format(a))
    else:
        pass_test()


def fail_test(msg):
    global test_failed
    test_failed += 1
    print('{}: \t! {}'.format(__get_caller_line_no(), msg))


def pass_test():
    print('{}: \tPassed'.format(__get_caller_line_no()))


def __get_caller_line_no():
    frame = inspect.stack()[3][0]
    info = inspect.getframeinfo(frame)
    return info.lineno


def run_tests(zone_id):
    global test_failed
    test_failed = 0

    test_is_adj(zone_id)
    test_list_adj(zone_id)
    test_perms(zone_id)

    test_n_reaches(zone_id)
    test_n_members(zone_id)
    test_n_eperms(zone_id)

    if test_failed > 0:
        raise AssertionError('Failed tests : {}'.format(test_failed))


def test_is_adj(zone_id):
    assert_true(query(zone_id, 'is_adj src=bob dst=datahub')['result'])
    assert_false(query(zone_id, 'is_adj src=bob dst=alice')['result'])


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
    assert_equal(query(zone_id, 'perms src=alice dst=bob')['result'],
                 None)
    assert_equal(query(zone_id, 'perms src=alice dst=ebi')['result'],
                 '11000')
    assert_equal(query(zone_id, 'perms src=audit dst=cyfnet')['result'],
                 '11001')
    assert_equal(query(zone_id, 'perms src=audit dst=eosc')['result'],
                 None)


def test_n_reaches(zone_id):
    assert_true(query(zone_id, 'n_reaches src=bob dst=datahub')['result'])
    assert_false(query(zone_id, 'n_reaches src=bob dst=alice')['result'])
    assert_true(query(zone_id, 'n_reaches src=bob dst=dhub_members')['result'])
    assert_true(query(zone_id, 'n_reaches src=luke dst=krakow')['result'])
    assert_false(query(zone_id, 'n_reaches src=anne dst=lisbon')['result'])
    assert_true(query(zone_id, 'n_reaches src=luke dst=dhub_members')['result'])


def test_n_members(zone_id):
    assert_equal(set(query(zone_id, 'n_members of=admins')['result']),
                 {'luke'})
    assert_equal(set(query(zone_id, 'n_members of=eo_data')['result']),
                 {'luke', 'bob', 'alice'})


def test_n_eperms(zone_id):
    assert_equal(query(zone_id, 'n_eperms src=alice dst=bob')['result'],
                 None)
    assert_equal(query(zone_id, 'n_eperms src=alice dst=ebi')['result'],
                 '11000')
    assert_equal(query(zone_id, 'n_eperms src=audit dst=cyfnet')['result'],
                 '11001')
    assert_equal(query(zone_id, 'n_eperms src=audit dst=eosc')['result'],
                 '11001')
    assert_equal(query(zone_id, 'n_eperms src=tom dst=primage')['result'],
                 '11011')
