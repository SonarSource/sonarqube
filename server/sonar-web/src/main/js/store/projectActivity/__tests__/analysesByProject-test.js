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
import analysesByProject from '../analysesByProject';
import { receiveProjectActivity, deleteAnalysis } from '../duck';

const PROJECT_FOO = 'project-foo';
const PROJECT_BAR = 'project-bar';

const ANALYSES_FOO = [
  {
    key: 'AVgFqeOSKpGuA48ADATE',
    date: '2016-10-27T12:21:15+0200',
    events: []
  },
  {
    key: 'AVgAgC1Vdo07z3PUnnkt',
    date: '2016-10-26T12:17:29+0200',
    events: []
  }
];

const ANALYSES_FOO_2 = [
  {
    key: 'AVgFqeOSKpGuA48ADATX',
    date: '2016-10-27T12:21:15+0200',
    events: []
  }
];

const ANALYSES_BAR = [
  {
    key: 'AVgGkRvCrrTJiPpCD-rG',
    date: '2016-10-27T16:33:50+0200',
    events: []
  }
];

const PAGING = {
  total: 3,
  pageIndex: 1,
  pageSize: 100
};

it('reducer', () => {
  const store = configureTestStore(analysesByProject);
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(receiveProjectActivity(PROJECT_FOO, ANALYSES_FOO, PAGING));
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(receiveProjectActivity(PROJECT_FOO, ANALYSES_FOO_2, { pageIndex: 2 }));
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(receiveProjectActivity(PROJECT_BAR, ANALYSES_BAR, PAGING));
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(deleteAnalysis(PROJECT_FOO, 'AVgFqeOSKpGuA48ADATE'));
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(deleteAnalysis(PROJECT_BAR, 'AVgGkRvCrrTJiPpCD-rG'));
  expect(store.getState()).toMatchSnapshot();
});
