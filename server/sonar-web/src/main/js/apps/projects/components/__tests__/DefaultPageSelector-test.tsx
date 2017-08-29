/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
jest.mock('../AllProjectsContainer', () => ({
  default: function AllProjectsContainer() {
    return null;
  }
}));

jest.mock('../../../../helpers/storage', () => ({
  isFavoriteSet: jest.fn(),
  isAllSet: jest.fn()
}));

jest.mock('../../../../api/components', () => ({
  searchProjects: jest.fn()
}));

import * as React from 'react';
import { mount } from 'enzyme';
import { UnconnectedDefaultPageSelector } from '../DefaultPageSelector';
import { doAsync } from '../../../../helpers/testUtils';

const isFavoriteSet = require('../../../../helpers/storage').isFavoriteSet as jest.Mock<any>;
const isAllSet = require('../../../../helpers/storage').isAllSet as jest.Mock<any>;
const searchProjects = require('../../../../api/components').searchProjects as jest.Mock<any>;

beforeEach(() => {
  isFavoriteSet.mockImplementation(() => false).mockClear();
  isAllSet.mockImplementation(() => false).mockClear();
});

it('shows all projects with existing filter', () => {
  const replace = jest.fn();
  mountRender(undefined, { size: '1' }, replace);
  expect(replace).not.toBeCalled();
});

it('shows all projects sorted by analysis date for anonymous', () => {
  const replace = jest.fn();
  mountRender({ isLoggedIn: false }, undefined, replace);
  expect(replace).lastCalledWith({ query: { sort: '-analysis_date' } });
});

it('shows favorite projects', () => {
  isFavoriteSet.mockImplementation(() => true);
  const replace = jest.fn();
  mountRender(undefined, undefined, replace);
  expect(replace).lastCalledWith({ pathname: '/projects/favorite', query: {} });
});

it('shows all projects', () => {
  isAllSet.mockImplementation(() => true);
  const replace = jest.fn();
  mountRender(undefined, undefined, replace);
  expect(replace).not.toBeCalled();
});

it('fetches favorites', () => {
  searchProjects.mockImplementation(() => Promise.resolve({ paging: { total: 3 } }));
  const replace = jest.fn();
  mountRender(undefined, undefined, replace);
  return doAsync().then(() => {
    expect(searchProjects).toHaveBeenLastCalledWith({ filter: 'isFavorite', ps: 1 });
    expect(replace).toBeCalledWith({ pathname: '/projects/favorite', query: {} });
  });
});

function mountRender(user: any = { isLoggedIn: true }, query: any = {}, replace: any = jest.fn()) {
  return mount(<UnconnectedDefaultPageSelector currentUser={user} location={{ query }} />, {
    context: { router: { replace } }
  });
}
