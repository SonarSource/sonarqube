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
import { IntlProvider } from 'react-intl';
import { deactivateUser } from '../../../../api/users';
import { mockUser } from '../../../../helpers/testMocks';
import { UserActive } from '../../../../types/users';
import DeactivateForm from '../DeactivateForm';

jest.mock('../../../../api/users', () => ({
  deactivateUser: jest.fn().mockResolvedValue({}),
}));

it('should deactivate user with anonymize set to true', () => {
  const user = mockUser() as UserActive;
  renderDeactivateForm(user);

  screen.getByRole('checkbox').click();
  expect(screen.getByRole('alert')).toBeInTheDocument();

  screen.getByRole('button', { name: 'users.deactivate' }).click();
  expect(deactivateUser).toHaveBeenCalledWith({ login: user.login, anonymize: true });
});

function renderDeactivateForm(user: UserActive) {
  return render(
    <IntlProvider defaultLocale="en" locale="en">
      <DeactivateForm onClose={jest.fn()} onUpdateUsers={jest.fn()} user={user} />
    </IntlProvider>
  );
}
