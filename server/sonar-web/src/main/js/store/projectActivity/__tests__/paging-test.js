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
import paging from '../paging';
import { receiveProjectActivity } from '../duck';

const PROJECT = 'project-foo';

const ANALYSES = [];

const PAGING_1 = {
  total: 3,
  pageIndex: 1,
  pageSize: 100
};

const PAGING_2 = {
  total: 5,
  pageIndex: 2,
  pageSize: 30
};

it('reducer', () => {
  const store = configureTestStore(paging);
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(receiveProjectActivity(PROJECT, ANALYSES, PAGING_1));
  expect(store.getState()).toMatchSnapshot();

  store.dispatch(receiveProjectActivity(PROJECT, ANALYSES, PAGING_2));
  expect(store.getState()).toMatchSnapshot();
});
