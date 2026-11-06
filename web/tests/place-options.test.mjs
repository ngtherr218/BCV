import test from 'node:test';
import assert from 'node:assert/strict';
import { showMapPhotos,availableCardPlaces } from '../src/placeOptions.js';
test('overview uses dots; photos start at zoom 11',()=>{assert.equal(showMapPhotos(4.7),false);assert.equal(showMapPhotos(10.99),false);assert.equal(showMapPhotos(11),true);assert.equal(showMapPhotos(15),true);});
test('new card offers only places without any card, regardless of card status',()=>{assert.deepEqual(availableCardPlaces([{id:1,card_id:10},{id:2,card_id:null},{id:3,card_id:11}]).map(p=>p.id),[2]);});
test('editing a card retains its current place but excludes other occupied places',()=>{assert.deepEqual(availableCardPlaces([{id:1,card_id:10},{id:2,card_id:null},{id:3,card_id:11}],'1').map(p=>p.id),[1,2]);});
