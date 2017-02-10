/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { configureTestStore } from '../../utils/configureStore';
import analyses, { getAnalysis } from '../analyses';
import { receiveProjectActivity, addEvent, deleteEvent, deleteAnalysis } from '../duck';

const PROJECT = 'project-foo';

const ANALYSES = [
  {
    key: 'AVgGkRvCrrTJiPpCD-rG',
    date: '2016-10-27T16:33:50+0200',
    events: [
      {
        key: 'AVjUDBiSiXOcXjpycvde',
        category: 'VERSION',
        name: '2.18-SNAPSHOT'
      }
    ]
  },
  {
    key: 'AVgFqeOSKpGuA48ADATE',
    date: '2016-10-27T12:21:15+0200',
    events: []
  },
  {
    key: 'AVgAgC1Vdo07z3PUnnkt',
    date: '2016-10-26T12:17:29+0200',
    events: [
      {
        key: 'AVkWNYNYr4pSN7TrXcjY',
        category: 'OTHER',
        name: 'foo'
      }
    ]
  }
];

const PAGING = {
  total: 3,
  pageIndex: 1,
  pageSize: 100
};

const NEW_EVENT = {
  key: 'AVkWcQ8Hr4pSN7TrXcjZ',
  category: 'OTHER',
  name: 'custom'
};

it('reducer', () => {
  const store = configureTestStore(analyses);
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(receiveProjectActivity(PROJECT, ANALYSES, PAGING));
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(addEvent(ANALYSES[0].key, NEW_EVENT));
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(deleteEvent(ANALYSES[0].key, NEW_EVENT.key));
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(deleteAnalysis(PROJECT, ANALYSES[1].key));
  expect(store.getState()).toMatchSnapshot();
});

it('selector `getAnalysis`', () => {
  const analysis = ANALYSES[0];
  const store = configureTestStore(analyses, { [analysis.key]: analysis });
  expect(getAnalysis(store.getState(), analysis.key)).toBe(analysis);
  expect(getAnalysis(store.getState(), 'random')).toBeFalsy();
});
