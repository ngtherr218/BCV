"""Offline model exchange via authenticated backend API (no direct database writes)."""
import argparse
import csv
import getpass
import hashlib
import json
from pathlib import Path
from urllib.parse import urlparse
from urllib.request import Request, build_opener, HTTPRedirectHandler


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        raise ValueError('Redirect refused: verify the backend URL before sending credentials.')


def request(base, path, token=None, body=None):
    parsed = urlparse(base)
    if parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ValueError('Use a clean backend origin URL')
    if parsed.scheme != 'https' and not (parsed.scheme == 'http' and parsed.hostname in ('localhost', '127.0.0.1')):
        raise ValueError('HTTPS required outside localhost')
    data = None if body is None else json.dumps(body).encode()
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    with build_opener(NoRedirect()).open(Request(base.rstrip('/') + path, data=data, headers=headers), timeout=90) as response:
        raw = response.read()
        return json.loads(raw) if raw else None


def prepare(dataset, version, canonical, model):
    payload = {'version': version, 'clusterCount': 15,
               'distanceLimit': dataset['config']['distance_limit'],
               'settings': dict(dataset['config']['settings']), 'weights': dataset['weights'], 'places': []}
    payload['settings']['datasetVersion'] = 'sha256:' + hashlib.sha256(canonical.read_bytes()).hexdigest()
    payload['settings']['modelSha256'] = hashlib.sha256(model.read_bytes()).hexdigest()
    payload['settings']['modelVersion'] = 'kmeans-k15-seed42'
    with canonical.open(encoding='utf-8-sig', newline='') as stream:
        for row in csv.DictReader(stream):
            features = {key: int(value) for key, value in row.items() if '__' in key}
            if len(features) != 35 or any(value not in (0, 1) for value in features.values()):
                raise ValueError('Canonical data must contain all 35 binary features')
            payload['places'].append({'placeId': int(row['DatabasePlaceID']),
                                      'cluster': int(row['ClusterID']), 'features': features})
    if len(payload['places']) != 311:
        raise ValueError('Expected 311 reviewed places; retrain/review new datasets before changing this guard.')
    return payload


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['export', 'prepare', 'import', 'activate'])
    parser.add_argument('--url')
    parser.add_argument('--email', default='admin@bcv.local')
    parser.add_argument('--input')
    parser.add_argument('--output')
    parser.add_argument('--version')
    parser.add_argument('--config-id', type=int)
    parser.add_argument('--canonical', default='reports/canonical-places.csv')
    parser.add_argument('--model', default='service/data/model.joblib')
    args = parser.parse_args()
    if args.command == 'prepare':
        if not all((args.input, args.output, args.version)):
            parser.error('prepare requires --input dataset.json --output model.json --version VERSION')
        payload = prepare(json.loads(Path(args.input).read_text(encoding='utf-8-sig')), args.version,
                          Path(args.canonical), Path(args.model))
        Path(args.output).write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
        return
    if not args.url:
        parser.error('--url is required')
    if args.command == 'export' and not args.output:
        parser.error('export requires --output')
    if args.command == 'import' and not args.input:
        parser.error('import requires --input')
    if args.command == 'activate' and not args.config_id:
        parser.error('activate requires --config-id')
    session = request(args.url, '/api/admin/login', body={'email': args.email, 'password': getpass.getpass('Admin password: ')})
    token = session['token']
    try:
        if args.command == 'export':
            result = request(args.url, '/api/admin/recommendations/dataset', token)
            Path(args.output).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
            print(f"Exported {len(result['places'])} places.")
        elif args.command == 'import':
            payload = json.loads(Path(args.input).read_text(encoding='utf-8-sig'))
            print(json.dumps(request(args.url, '/api/admin/recommendations/models', token, payload)))
        else:
            print(json.dumps(request(args.url, f'/api/admin/recommendations/models/{args.config_id}/activate', token, {})))
    finally:
        request(args.url, '/api/account/logout', token, {})


if __name__ == '__main__':
    main()
