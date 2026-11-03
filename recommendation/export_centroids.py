"""Reconstruct fixed Lloyd centroids from the reviewed, labelled training snapshot.

Keep all 315 source rows (including aliases), matching the fitted model's training
population. Verify every label before exporting the backend inference resource.
"""
import csv
import json
import math
from pathlib import Path

root = Path(__file__).resolve().parent
with (root / 'service/data/features.csv').open(encoding='utf-8-sig') as source:
    reader = csv.DictReader(source)
    features = reader.fieldnames[4:]
    rows = list(reader)

def normalize(values):
    result = list(values)
    for prefix in ('kind__', 'landform__', 'ecosystem__', 'theme__'):
        indices = [i for i, code in enumerate(features) if code.startswith(prefix)]
        norm = math.sqrt(sum(values[i] ** 2 for i in indices))
        for i in indices:
            result[i] = values[i] / norm * .5 if norm else 0
    return result

vectors = [normalize([float(row[code]) for code in features]) for row in rows]
clusters = sorted({int(row['ClusterID']) for row in rows})
centers = []
for cluster in clusters:
    members = [v for row, v in zip(rows, vectors) if int(row['ClusterID']) == cluster]
    centers.append([sum(v[i] for v in members) / len(members) for i in range(len(features))])
for row, vector in zip(rows, vectors):
    predicted = min(range(len(centers)), key=lambda c: sum((a-b)**2 for a,b in zip(vector, centers[c])))
    assert clusters[predicted] == int(row['ClusterID']), row['PlaceID']
target = root.parent / 'backend/Bcv.Api/Modules/Discovery/Domain/recommendation-centroids.json'
target.write_text(json.dumps({'Features': features, 'Clusters': clusters, 'Centers': centers}, indent=2) + '\n', encoding='utf-8')
print(f'Exported {len(centers)} centroids; verified {len(rows)} labels.')
