import test from 'node:test';
import assert from 'node:assert/strict';
import { suggestedFeatures, changePlaceType } from '../src/placeFeatureDefaults.js';

test('custom types use mappings returned by the backend',()=>{
  assert.deepEqual(suggestedFeatures('432',[{id:432,feature_codes:['kind__museum','theme__history']}],[{code:'kind__museum'},{code:'theme__history'}]),['kind__museum','theme__history']);
});
test('a standard type uses its edited mapping rather than hardcoded defaults',()=>{
  assert.deepEqual(suggestedFeatures(101,[{id:101,feature_codes:['kind__museum']}],[{code:'kind__museum'}]),['kind__museum']);
});
test('missing mappings remain empty so the form can require configuration',()=>{
  assert.deepEqual(suggestedFeatures(999,[{id:999}],[]),[]);
});
test('changing type resets a custom place to inheritance',()=>{
  const previous={name:'A',feature_source:'CUSTOM',feature_codes:['kind__museum']};
  assert.deepEqual(changePlaceType(previous,432,['kind__religious']),{name:'A',place_type_id:432,feature_source:'TYPE',feature_codes:['kind__religious']});
  assert.equal(previous.feature_source,'CUSTOM');
});
