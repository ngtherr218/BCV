import test from 'node:test';
import assert from 'node:assert/strict';
import { date } from '../src/api.js';

test('PostgreSQL JSON timezone offsets are preserved rather than suffixed with Z', () => {
  assert.equal(date('2026-11-06T10:30:00+00:00'), date('2026-11-06T10:30:00Z'));
  assert.equal(date('2026-11-06T10:30:00+07:00'), date('2026-11-06T03:30:00Z'));
  assert.equal(date('2026-11-06T10:30:00-05:00'), date('2026-11-06T15:30:00Z'));
  assert.notEqual(date('2026-11-06T10:30:00.123456+00:00'), '—');
});
test('naive API timestamps remain UTC and PostgreSQL space/short-offset formats work', () => {
  assert.equal(date('2026-11-06T10:30:00'), date('2026-11-06T10:30:00Z'));
  assert.equal(date('2026-11-06 10:30:00+00'), date('2026-11-06T10:30:00Z'));
  assert.equal(date('2026-11-06T10:30:00+0700'), date('2026-11-06T03:30:00Z'));
  assert.equal(date('2026-11-06'), date('2026-11-06T00:00:00Z'));
});
test('missing, invalid or unsupported dates cannot crash a list render', () => {
  for (const value of [null, undefined, '', '  ', 'invalid', 'infinity', '2026-11-06T10:30:00+00:00Z', new Date(NaN), NaN, Infinity, {}, []]) {
    assert.doesNotThrow(() => date(value));
    assert.equal(date(value), '—');
  }
});
test('Date instances and millisecond timestamps are supported, including epoch zero', () => {
  assert.equal(date(new Date('2026-11-06T10:30:00Z')), date('2026-11-06T10:30:00Z'));
  assert.equal(date(0), date('1970-01-01T00:00:00Z'));
});
