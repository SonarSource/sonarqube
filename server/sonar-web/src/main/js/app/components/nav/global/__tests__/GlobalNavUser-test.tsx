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
import { mockCurrentUser, mockLoggedInUser, mockRouter } from '../../../../../helpers/testMocks';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { GlobalNavUser } from '../GlobalNavUser';

it('should render the right interface for anonymous user', () => {
  renderGlobalNavUser({ currentUser: mockCurrentUser() });
  expect(screen.getByText('layout.login')).toBeInTheDocument();
});

it('should render the right interface for logged in user', async () => {
  const user = userEvent.setup();
  renderGlobalNavUser();
  await user.click(screen.getByRole('link'));

  expect(screen.getByRole('link', { name: 'my_account.page' })).toHaveFocus();
});

function renderGlobalNavUser(overrides: Partial<GlobalNavUser['props']> = {}) {
  return renderComponent(
    <GlobalNavUser currentUser={mockLoggedInUser()} router={mockRouter()} {...overrides} />
  );
}
