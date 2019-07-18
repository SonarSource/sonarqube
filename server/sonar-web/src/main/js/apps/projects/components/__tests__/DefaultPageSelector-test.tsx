/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { mount } from 'enzyme';
import * as React from 'react';
import { get } from 'sonar-ui-common/helpers/storage';
import { doAsync } from 'sonar-ui-common/helpers/testUtils';
import { searchProjects } from '../../../../api/components';
import { DefaultPageSelector } from '../DefaultPageSelector';

jest.mock('../AllProjectsContainer', () => ({
  // eslint-disable-next-line
  default: function AllProjectsContainer() {
    return null;
  }
}));

jest.mock('sonar-ui-common/helpers/storage', () => ({
  get: jest.fn()
}));

jest.mock('../../../../api/components', () => ({
  searchProjects: jest.fn()
}));

beforeEach(() => {
  (get as jest.Mock).mockImplementation(() => '').mockClear();
});

it('shows all projects with existing filter', () => {
  const replace = jest.fn();
  mountRender(undefined, { size: '1' }, replace);
  expect(replace).not.toBeCalled();
});

it('shows all projects sorted by analysis date for anonymous', () => {
  const replace = jest.fn();
  mountRender({ isLoggedIn: false }, undefined, replace);
  expect(replace).lastCalledWith({ pathname: '/projects', query: { sort: '-analysis_date' } });
});

it('shows favorite projects', () => {
  (get as jest.Mock).mockImplementation(() => 'favorite');
  const replace = jest.fn();
  mountRender(undefined, undefined, replace);
  expect(replace).lastCalledWith({ pathname: '/projects/favorite', query: {} });
});

it('shows all projects', () => {
  (get as jest.Mock).mockImplementation(() => 'all');
  const replace = jest.fn();
  mountRender(undefined, undefined, replace);
  expect(replace).not.toBeCalled();
});

it('fetches favorites', () => {
  (searchProjects as jest.Mock).mockImplementation(() => Promise.resolve({ paging: { total: 3 } }));
  const replace = jest.fn();
  mountRender(undefined, undefined, replace);
  return doAsync().then(() => {
    expect(searchProjects).toHaveBeenLastCalledWith({ filter: 'isFavorite', ps: 1 });
    expect(replace).toBeCalledWith({ pathname: '/projects/favorite', query: {} });
  });
});

function mountRender(
  currentUser: T.CurrentUser = { isLoggedIn: true },
  query: any = {},
  replace: any = jest.fn()
) {
  return mount(
    <DefaultPageSelector
      currentUser={currentUser}
      location={{ pathname: '/projects', query }}
      router={{ replace }}
    />
  );
}
