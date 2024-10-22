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
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { changePassword } from '../../../api/users';
import { mockAppState } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { AppState } from '../../../types/appstate';
import ChangeAdminPasswordApp from '../ChangeAdminPasswordApp';
import { DEFAULT_ADMIN_PASSWORD } from '../constants';

jest.mock('../../../api/users', () => ({
  changePassword: jest.fn().mockResolvedValue({}),
}));

const ui = {
  updateButton: byRole('button', { name: 'update_verb' }),
  passwordInput: byLabelText(/^password/),
  confirmInput: byLabelText(/confirm_password\*/i),
  unauthorizedMessage: byText('unauthorized.message'),
};

it('should disallow change when not an admin', () => {
  renderChangeAdminPasswordApp(mockAppState({ instanceUsesDefaultAdminCredentials: true }));
  expect(ui.unauthorizedMessage.get()).toBeInTheDocument();
});

it('should allow changing password when using the default admin password', async () => {
  const user = userEvent.setup();
  renderChangeAdminPasswordApp(
    mockAppState({ instanceUsesDefaultAdminCredentials: true, canAdmin: true }),
  );
  expect(ui.updateButton.get()).toBeDisabled();
  await user.type(ui.passwordInput.get(), 'passworD$123');

  expect(ui.updateButton.get()).toBeDisabled();
  await user.type(ui.confirmInput.get(), 'pass');
  expect(ui.updateButton.get()).toBeDisabled();
  await user.keyboard('worD$123');
  expect(ui.updateButton.get()).toBeEnabled();
  await user.click(ui.updateButton.get());
  expect(changePassword).toHaveBeenCalledWith({
    login: 'admin',
    password: 'passworD$123',
  });
});

it('should not allow to submit the default password', async () => {
  const user = userEvent.setup();
  renderChangeAdminPasswordApp(
    mockAppState({ instanceUsesDefaultAdminCredentials: true, canAdmin: true }),
  );

  await user.type(ui.passwordInput.get(), DEFAULT_ADMIN_PASSWORD);
  await user.type(ui.confirmInput.get(), DEFAULT_ADMIN_PASSWORD);

  expect(ui.updateButton.get()).toBeDisabled();
});

function renderChangeAdminPasswordApp(appState?: AppState) {
  return renderApp('admin/change_admin_password', <ChangeAdminPasswordApp />, { appState });
}
