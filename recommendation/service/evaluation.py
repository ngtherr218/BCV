"""Blind human judgments and reproducible Precision@3/NDCG@3. Never invent labels."""
import argparse
import csv
import hashlib
import json
import math
from pathlib import Path


def template(rankings, destination):
    """All eligible candidates, not just retrieved items: permits a genuine IDCG."""
    with Path(destination).open('w', encoding='utf-8-sig', newline='') as stream:
        writer = csv.writer(stream)
        writer.writerow(['case_id', 'participant_id', 'place_id', 'place_name', 'region',
                         'distance_meters', 'required_steps', 'relevance', 'comment'])
        for case in rankings['cases']:
            # Order by a deterministic hash; do not reveal ranking/algorithm to assessors.
            candidates = sorted(case['candidates'], key=lambda p: hashlib.sha256(
                f"{case['caseId']}:{p['placeId']}".encode()).hexdigest())
            for p in candidates:
                writer.writerow([case['caseId'], case['participantId'], p['placeId'], p['name'],
                                 p['regionName'], p['distanceMeters'], p['requiredSteps'], '', ''])


def score(rankings, judgments):
    expected = {(c['caseId'], str(p['placeId'])): c['participantId']
                for c in rankings['cases'] for p in c['candidates']}
    labels = {}
    for row in judgments:
        key = (row['case_id'], row['place_id'])
        if key not in expected or row['participant_id'] != expected[key] or key in labels:
            raise ValueError(f'Unknown/mismatched/duplicate judgment: {key}')
        if row['relevance'] not in ('0', '1', '2', '3'):
            raise ValueError(f'Missing or invalid human relevance (0..3): {key}')
        labels[key] = int(row['relevance'])
    if labels.keys() != expected.keys():
        raise ValueError('Incomplete judgments: every eligible candidate must be assessed before scoring.')
    if not expected:
        raise ValueError('No eligible candidates to evaluate.')
    rows = []
    excluded = []
    for case in rankings['cases']:
        if not case['candidates']:
            excluded.append(case['caseId'])
            continue
        grades = {int(p['placeId']): labels[(case['caseId'], str(p['placeId']))]
                  for p in case['candidates']}
        ideal = dcg(sorted(grades.values(), reverse=True)[:3])
        for method, retrieved in case['rankings'].items():
            ids = [p['placeId'] for p in retrieved]
            if len(ids) > 3 or len(set(ids)) != len(ids) or any(i not in grades for i in ids):
                raise ValueError('Invalid ranking')
            relevance = [grades[i] for i in ids]
            rows.append({'caseId': case['caseId'], 'participantId': case['participantId'],
                         'method': method, 'precisionAt3': sum(g >= 2 for g in relevance) / 3,
                         'ndcgAt3': dcg(relevance) / ideal if ideal else 0.0,
                         'returned': len(ids), 'noRelevantGroundTruth': ideal == 0})
    methods = sorted({r['method'] for r in rows})
    averages = {m: {metric: sum(r[metric] for r in rows if r['method'] == m) /
                   sum(r['method'] == m for r in rows) for metric in ('precisionAt3', 'ndcgAt3')}
                for m in methods}
    return {'status': 'human_judgments_scored', 'datasetSha256': rankings['datasetSha256'],
            'configVersion': rankings['configVersion'], 'evaluatedCases': len(rows)//len(methods),
            'participants': len({r['participantId'] for r in rows}), 'excludedEmptyCases': excluded,
            'protocol': 'P@3: relevance>=2, denominator 3; NDCG gains 2^grade-1, full eligible IDCG; IDCG=0 yields 0.',
            'averages': averages, 'perCase': rows}


def dcg(grades):
    return sum((2 ** grade - 1) / math.log2(rank + 2) for rank, grade in enumerate(grades))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['template', 'score'])
    parser.add_argument('--rankings', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--judgments')
    args = parser.parse_args()
    rankings = json.loads(Path(args.rankings).read_text(encoding='utf-8-sig'))
    if args.command == 'template':
        template(rankings, args.output)
        print('Blank human assessment template created. No quality results inferred.')
    else:
        if not args.judgments:
            parser.error('--judgments is required for score')
        with Path(args.judgments).open(encoding='utf-8-sig', newline='') as stream:
            report = score(rankings, csv.DictReader(stream))
        Path(args.output).write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')


if __name__ == '__main__':
    main()
