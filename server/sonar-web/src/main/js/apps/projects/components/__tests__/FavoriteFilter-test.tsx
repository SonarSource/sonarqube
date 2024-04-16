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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { save } from '../../../../helpers/storage';
import {
  mockCurrentUser,
  mockLocation,
  mockLoggedInUser,
  mockRouter,
} from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ALL_PATHNAME, FAVORITE_PATHNAME, FavoriteFilter } from '../FavoriteFilter';

jest.mock('../../../../helpers/storage', () => ({
  save: jest.fn(),
}));

beforeEach(() => {
  (save as jest.Mock<any>).mockClear();
});

it('renders for logged in user', () => {
  renderFavoriteFilter();
  expect(screen.getByText('my_favorites')).toBeInTheDocument();
  expect(screen.getByText('all')).toBeInTheDocument();
});

it.each([
  ['my_favorites', 'favorite', ALL_PATHNAME],
  ['all', 'all', FAVORITE_PATHNAME],
])(
  'saves last selection',
  async (optionTranslationId: string, localStorageValue: string, initialPathName: string) => {
    const user = userEvent.setup();

    renderFavoriteFilter({ location: mockLocation({ pathname: initialPathName }) });

    await user.click(screen.getByText(optionTranslationId));
    expect(save).toHaveBeenLastCalledWith('sonarqube.projects.default', localStorageValue);
  },
);

it('does not render for anonymous', () => {
  renderFavoriteFilter({ currentUser: mockCurrentUser() });
  expect(screen.queryByText('my_favorites')).not.toBeInTheDocument();
});

function renderFavoriteFilter({
  currentUser = mockLoggedInUser(),
  location = mockLocation(),
}: Partial<FavoriteFilter['props']> = {}) {
  renderComponent(
    <FavoriteFilter
      currentUser={currentUser}
      location={location}
      router={mockRouter()}
      params={{}}
    />,
  );
}
