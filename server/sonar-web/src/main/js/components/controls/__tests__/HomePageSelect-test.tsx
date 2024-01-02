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
import { setHomePage } from '../../../api/users';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { DEFAULT_HOMEPAGE, HomePageSelect } from '../HomePageSelect';

jest.mock('../../../api/users', () => ({
  setHomePage: jest.fn().mockResolvedValue(null),
}));

it('renders and behaves correctly', async () => {
  const user = userEvent.setup();
  const updateCurrentUserHomepage = jest.fn();
  renderHomePageSelect({ updateCurrentUserHomepage });
  const button = screen.getByRole('button');
  expect(button).toBeInTheDocument();

  await user.click(button);
  await new Promise(setImmediate);
  expect(setHomePage).toHaveBeenCalledWith({ type: 'MY_PROJECTS' });
  expect(updateCurrentUserHomepage).toHaveBeenCalled();
  expect(button).toHaveFocus();
});

it('renders correctly if user is on the homepage', async () => {
  const user = userEvent.setup();

  renderHomePageSelect({ currentUser: mockLoggedInUser({ homepage: { type: 'MY_PROJECTS' } }) });
  const button = screen.getByRole('button');
  expect(button).toBeInTheDocument();

  await user.click(button);
  await new Promise(setImmediate);
  expect(setHomePage).toHaveBeenCalledWith(DEFAULT_HOMEPAGE);
  expect(button).toHaveFocus();
});

function renderHomePageSelect(props: Partial<HomePageSelect['props']> = {}) {
  return renderComponent(
    <HomePageSelect
      currentPage={{ type: 'MY_PROJECTS' }}
      currentUser={mockLoggedInUser()}
      updateCurrentUserHomepage={jest.fn()}
      {...props}
    />,
  );
}
