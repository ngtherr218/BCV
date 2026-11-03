import unittest
from service.evaluation import score


class EvaluationTests(unittest.TestCase):
    def fixture(self):
        data = {'datasetSha256': 'fixture', 'configVersion': 'test', 'cases': [
            {'caseId': 'c1', 'participantId': 'p1', 'candidates': [{'placeId': i} for i in range(1, 5)],
             'rankings': {'perfect': [{'placeId': 1}, {'placeId': 2}, {'placeId': 3}],
                          'reversed': [{'placeId': 4}, {'placeId': 3}, {'placeId': 2}]}}]}
        labels = [{'case_id': 'c1', 'participant_id': 'p1', 'place_id': str(i), 'relevance': str(4-i)}
                  for i in range(1, 5)]
        return data, labels

    def test_known_metrics(self):
        data, labels = self.fixture()
        report = score(data, labels)
        self.assertEqual(report['averages']['perfect']['ndcgAt3'], 1)
        self.assertAlmostEqual(report['averages']['perfect']['precisionAt3'], 2/3)
        self.assertLess(report['averages']['reversed']['ndcgAt3'], 1)

    def test_missing_labels_rejected(self):
        data, labels = self.fixture()
        with self.assertRaises(ValueError):
            score(data, labels[:-1])

    def test_duplicate_labels_rejected(self):
        data, labels = self.fixture()
        with self.assertRaises(ValueError):
            score(data, labels + labels[:1])

    def test_blank_labels_are_not_zero(self):
        data, labels = self.fixture()
        labels[0]['relevance'] = ''
        with self.assertRaises(ValueError):
            score(data, labels)

    def test_no_relevant_items_is_defined(self):
        data, labels = self.fixture()
        for row in labels:
            row['relevance'] = '0'
        self.assertEqual(score(data, labels)['averages']['perfect']['ndcgAt3'], 0)


if __name__ == '__main__':
    unittest.main()
