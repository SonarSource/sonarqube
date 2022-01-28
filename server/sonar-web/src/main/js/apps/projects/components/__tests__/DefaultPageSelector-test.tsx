/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { shallow } from 'enzyme';
import * as React from 'react';
import { searchProjects } from '../../../../api/components';
import { get } from '../../../../helpers/storage';
import {
  mockCurrentUser,
  mockLocation,
  mockLoggedInUser,
  mockRouter
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { hasGlobalPermission } from '../../../../helpers/users';
import { CurrentUser } from '../../../../types/types';
import { DefaultPageSelector } from '../DefaultPageSelector';

jest.mock(
  '../AllProjectsContainer',
  () =>
    // eslint-disable-next-line
    function AllProjectsContainer() {
      return null;
    }
);

jest.mock('../../../../helpers/storage', () => ({
  get: jest.fn().mockReturnValue(undefined)
}));

jest.mock('../../../../helpers/users', () => ({
  hasGlobalPermission: jest.fn().mockReturnValue(false),
  isLoggedIn: jest.fn((u: CurrentUser) => u.isLoggedIn)
}));

jest.mock('../../../../api/components', () => ({
  searchProjects: jest.fn().mockResolvedValue({ paging: { total: 0 } })
}));

beforeEach(jest.clearAllMocks);

it('renders correctly', () => {
  expect(shallowRender({ currentUser: mockLoggedInUser() }).type()).toBeNull(); // checking
  expect(shallowRender({ currentUser: mockCurrentUser() })).toMatchSnapshot('default');
});

it("1.1 doesn't redirect for anonymous users", async () => {
  const replace = jest.fn();
  const wrapper = shallowRender({
    currentUser: mockCurrentUser(),
    router: mockRouter({ replace })
  });
  await waitAndUpdate(wrapper);
  expect(replace).not.toBeCalled();
});

it("1.2 doesn't redirect if there's an existing filter in location", async () => {
  const replace = jest.fn();
  const wrapper = shallowRender({
    location: mockLocation({ query: { size: '1' } }),
    router: mockRouter({ replace })
  });

  await waitAndUpdate(wrapper);

  expect(replace).not.toBeCalled();
});

it("1.3 doesn't redirect if the user previously used the 'all' filter", async () => {
  (get as jest.Mock).mockReturnValueOnce('all');
  const replace = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ replace }) });

  await waitAndUpdate(wrapper);

  expect(replace).not.toBeCalled();
});

it('2.1 redirects to favorites if the user previously used the "favorites" filter', async () => {
  (get as jest.Mock).mockReturnValueOnce('favorite');
  const replace = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ replace }) });

  await waitAndUpdate(wrapper);

  expect(replace).toBeCalledWith('/projects/favorite');
});

it('2.2 redirects to favorites if the user has starred projects', async () => {
  (searchProjects as jest.Mock).mockResolvedValueOnce({ paging: { total: 3 } });
  const replace = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ replace }) });

  await waitAndUpdate(wrapper);

  expect(searchProjects).toHaveBeenLastCalledWith({ filter: 'isFavorite', ps: 1 });
  expect(replace).toBeCalledWith('/projects/favorite');
});

it('3.1 redirects to create project page, if user has correct permissions AND there are 0 projects', async () => {
  (hasGlobalPermission as jest.Mock).mockReturnValueOnce(true);
  const replace = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ replace }) });

  await waitAndUpdate(wrapper);

  expect(replace).toBeCalledWith('/projects/create');
});

it("3.1 doesn't redirect to create project page, if user has no permissions", async () => {
  const replace = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ replace }) });

  await waitAndUpdate(wrapper);

  expect(replace).not.toBeCalled();
});

it("3.1 doesn't redirect to create project page, if there's existing projects", async () => {
  (searchProjects as jest.Mock)
    .mockResolvedValueOnce({ paging: { total: 0 } }) // no favorites
    .mockResolvedValueOnce({ paging: { total: 3 } }); // existing projects
  const replace = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ replace }) });

  await waitAndUpdate(wrapper);

  expect(replace).not.toBeCalled();
});

function shallowRender(props: Partial<DefaultPageSelector['props']> = {}) {
  return shallow<DefaultPageSelector>(
    <DefaultPageSelector
      currentUser={mockLoggedInUser()}
      location={mockLocation({ pathname: '/projects' })}
      router={mockRouter()}
      {...props}
    />
  );
}
