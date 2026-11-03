"""Integration check. ONLY run against a disposable local database; creates fixtures."""
import copy
import json
import os
import uuid
from pathlib import Path
from urllib.error import HTTPError
from service.exchange import request, prepare

base = 'http://127.0.0.1:5183'
password = os.environ['BCV_AI_TEST_ADMIN_PASSWORD']
admin = request(base, '/api/admin/login', body={'email': 'ai-test@bcv.local', 'password': password})['token']

def expect_status(status, action):
    try:
        action()
    except HTTPError as error:
        assert error.code == status, (error.code, error.read().decode())
        return
    raise AssertionError(f'Expected HTTP {status}')

suffix = uuid.uuid4().hex[:12]
old_id = None
user = None
try:
    expect_status(401, lambda: request(base, '/api/admin/recommendations/dataset'))
    dataset = request(base, '/api/admin/recommendations/dataset', admin)
    old_id = dataset['config']['recommendation_config_id']
    assert len(dataset['places']) == 311
    assert all(len(p['features']) == 35 for p in dataset['places'])
    fixture = {'email': f'ai-{suffix}@example.test', 'password': 'FixtureOnly2026!', 'confirmPassword': 'FixtureOnly2026!'}
    request(base, '/api/account/register', body=fixture)
    user = request(base, '/api/account/login', body=fixture)['token']
    expect_status(403, lambda: request(base, '/api/admin/recommendations/dataset', user))
    # PUT endpoints use explicit method below because exchange defaults to POST.
    from urllib.request import Request, urlopen
    def put(path, body):
        req = Request(base + path, data=json.dumps(body).encode(), method='PUT',
                      headers={'Content-Type': 'application/json', 'Authorization': 'Bearer '+user})
        with urlopen(req) as response:
            return json.loads(response.read())
    interests = request(base, '/api/interests', user)
    assert len(interests['placeTypes']) == 6
    expect_status(400, lambda: put('/api/interests', {'placeTypeIds': [101]}))
    put('/api/interests', {'placeTypeIds': list(range(201, 207))})
    put('/api/activity-level', {'activityLevel': 'MEDIUM'})
    put('/api/starting-point', {'source': 'SUGGESTED'})
    recommendations = request(base, '/api/explore/recommendations', user)
    assert len(recommendations['places']) == 3
    assert all(p['reasons'] for p in recommendations['places'])
    assert recommendations['configuration']['mappingVersion'] == 'six-interests-v1'
    manifest = prepare(dataset, 'integration-'+suffix, Path('reports/canonical-places.csv'), Path('service/data/model.joblib'))
    bad = copy.deepcopy(manifest)
    bad['places'][0]['features'].pop(next(iter(bad['places'][0]['features'])))
    expect_status(400, lambda: request(base, '/api/admin/recommendations/models', admin, bad))
    created = request(base, '/api/admin/recommendations/models', admin, manifest)
    assert created['status'] == 'DRAFT'
    assert request(base, '/api/admin/recommendations/dataset', admin)['config']['recommendation_config_id'] == old_id
    expect_status(409, lambda: request(base, '/api/admin/recommendations/models', admin, manifest))
    request(base, f"/api/admin/recommendations/models/{created['configId']}/activate", admin, {})
    after = request(base, '/api/explore/recommendations', user)
    assert after['configVersion'] == manifest['version']
    assert [p['placeId'] for p in after['places']] == [p['placeId'] for p in recommendations['places']]
    print('PASS: 311x35 export; auth 401/403; six interests; Top 3 reasons; invalid import; DRAFT isolation; duplicate rejection; activation and stable ranking.')
finally:
    if old_id is not None:
        request(base, f'/api/admin/recommendations/models/{old_id}/activate', admin, {})
    if user:
        request(base, '/api/account/logout', user, {})
    request(base, '/api/account/logout', admin, {})
