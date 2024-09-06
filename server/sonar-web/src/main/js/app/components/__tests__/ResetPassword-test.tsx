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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byLabelText, byRole, byTestId } from '~sonar-aligned/helpers/testSelector';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { ResetPassword, ResetPasswordProps } from '../ResetPassword';

jest.mock('../../../helpers/system', () => ({
  getBaseUrl: jest.fn().mockReturnValue('/context'),
}));

jest.mock('../../../api/users', () => ({
  changePassword: jest.fn().mockResolvedValue(true),
}));

const originalLocation = window.location;

beforeAll(() => {
  const location = {
    ...window.location,
    href: null,
  };
  Object.defineProperty(window, 'location', {
    writable: true,
    value: location,
  });
});

afterAll(() => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: originalLocation,
  });
});

/*
 * Note: the form itself is also used in the context of the account page
 * and is tested there as well (i.e. Account-it.tsx)
 */
it('should navigate to the homepage after submission', async () => {
  const user = userEvent.setup();
  renderResetPassword();

  // Make password strong
  await user.type(ui.oldPasswordInput.get(), '1234');
  await user.type(ui.passwordInput.get(), 'P@ssword12345');
  await user.type(ui.passwordConfirmationInput.get(), 'P@ssword12345');

  await user.click(ui.submitButton.get());

  expect(window.location.href).toBe('/context/');
});

function renderResetPassword(props: Partial<ResetPasswordProps> = {}) {
  return renderComponent(<ResetPassword currentUser={mockLoggedInUser()} {...props} />);
}

const ui = {
  oldPasswordInput: byLabelText(/my_profile\.password\.old/),
  passwordInput: byTestId('create-password'),
  passwordConfirmationInput: byLabelText(/confirm_password\*/i),
  submitButton: byRole('button', { name: 'update_verb' }),
};
