/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { render, screen } from '@testing-library/react';
import * as React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { searchProjects } from '../../../../api/components';
import { useLocation } from '../../../../components/hoc/withRouter';
import { get } from '../../../../helpers/storage';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { hasGlobalPermission } from '../../../../helpers/users';
import { CurrentUser } from '../../../../types/users';
import { DefaultPageSelector } from '../DefaultPageSelector';

jest.mock(
  '../AllProjects',
  () =>
    // eslint-disable-next-line
    function AllProjects() {
      return <div>All Projects</div>;
    },
);

jest.mock('../../../../helpers/storage', () => ({
  get: jest.fn().mockReturnValue(undefined),
}));

jest.mock('../../../../helpers/users', () => ({
  hasGlobalPermission: jest.fn().mockReturnValue(false),
  isLoggedIn: jest.fn((u: CurrentUser) => u.isLoggedIn),
}));

jest.mock('../../../../api/components', () => ({
  searchProjects: jest.fn().mockResolvedValue({ paging: { total: 0 } }),
}));

beforeEach(jest.clearAllMocks);

it("1.1 doesn't redirect for anonymous users", async () => {
  renderDefaultPageSelector({ currentUser: mockCurrentUser() });

  expect(await screen.findByText('All Projects')).toBeInTheDocument();
});

it("1.2 doesn't redirect if there's an existing filter in location", async () => {
  renderDefaultPageSelector({ path: '/projects?size=1' });

  expect(await screen.findByText('All Projects')).toBeInTheDocument();
});

it("1.3 doesn't redirect if the user previously used the 'all' filter", async () => {
  (get as jest.Mock).mockReturnValueOnce('all');
  renderDefaultPageSelector();

  expect(await screen.findByText('All Projects')).toBeInTheDocument();
});

it('2.1 redirects to favorites if the user previously used the "favorites" filter', async () => {
  (get as jest.Mock).mockReturnValueOnce('favorite');
  renderDefaultPageSelector();

  expect(await screen.findByText('/projects/favorite')).toBeInTheDocument();
});

it('2.2 redirects to favorites if the user has starred projects', async () => {
  (searchProjects as jest.Mock).mockResolvedValueOnce({ paging: { total: 3 } });
  renderDefaultPageSelector();

  expect(searchProjects).toHaveBeenLastCalledWith({ filter: 'isFavorite', ps: 1 });
  expect(await screen.findByText('/projects/favorite')).toBeInTheDocument();
});

it('3.1 redirects to create project page, if user has correct permissions AND there are 0 projects', async () => {
  (hasGlobalPermission as jest.Mock).mockReturnValueOnce(true);
  renderDefaultPageSelector();

  expect(await screen.findByText('/projects/create')).toBeInTheDocument();
});

it("3.1 doesn't redirect to create project page, if user has no permissions", async () => {
  renderDefaultPageSelector();

  expect(await screen.findByText('All Projects')).toBeInTheDocument();
});

it("3.1 doesn't redirect to create project page, if there's existing projects", async () => {
  (searchProjects as jest.Mock)
    .mockResolvedValueOnce({ paging: { total: 0 } }) // no favorites
    .mockResolvedValueOnce({ paging: { total: 3 } }); // existing projects
  renderDefaultPageSelector();

  expect(await screen.findByText('All Projects')).toBeInTheDocument();
});

function RouteDisplayer() {
  const location = useLocation();
  return <div>{location.pathname}</div>;
}

function renderDefaultPageSelector({
  path = '/projects',
  currentUser = mockLoggedInUser(),
}: {
  path?: string;
  currentUser?: CurrentUser;
} = {}) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="projects">
          <Route index element={<DefaultPageSelector currentUser={currentUser} />} />
          <Route path="*" element={<RouteDisplayer />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}
