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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { save } from '../../../../helpers/storage';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { FavoriteFilter } from '../FavoriteFilter';

jest.mock('../../../../helpers/storage', () => ({
  save: jest.fn()
}));

beforeEach(() => {
  (save as jest.Mock<any>).mockClear();
});

it('renders for logged in user', () => {
  renderFavoriteFilter();
  expect(screen.queryByText('my_favorites')).toBeInTheDocument();
  expect(screen.queryByText('all')).toBeInTheDocument();
});

it('saves last selection', async () => {
  const user = userEvent.setup();

  renderFavoriteFilter();

  await user.click(screen.getByText('my_favorites'));
  expect(save).toBeCalledWith('sonarqube.projects.default', 'favorite');
  await user.click(screen.getByText('all'));
  expect(save).toBeCalledWith('sonarqube.projects.default', 'all');
});

it('does not render for anonymous', () => {
  renderFavoriteFilter({ currentUser: mockCurrentUser() });
  expect(screen.queryByText('my_favorites')).not.toBeInTheDocument();
});

function renderFavoriteFilter({
  currentUser = mockLoggedInUser(),
  query = { size: 1 }
}: Partial<FavoriteFilter['props']> = {}) {
  renderComponent(<FavoriteFilter currentUser={currentUser} query={query} />);
}
