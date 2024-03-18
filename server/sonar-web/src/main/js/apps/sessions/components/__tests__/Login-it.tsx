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

import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { addGlobalErrorMessage } from 'design-system';
import * as React from 'react';
import { getLoginMessage } from '../../../../api/settings';
import { getIdentityProviders } from '../../../../api/users';
import { mockLocation } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../../helpers/testSelector';
import { LoginContainer } from '../LoginContainer';
import { getBaseUrl } from '../../../../helpers/system';

jest.mock('../../../../helpers/system', () => ({
  getBaseUrl: jest.fn().mockReturnValue(''),
}));

jest.mock('../../../../api/users', () => {
  const { mockIdentityProvider } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getIdentityProviders: jest
      .fn()
      .mockResolvedValue({ identityProviders: [mockIdentityProvider()] }),
  };
});

jest.mock('../../../../api/auth', () => ({
  logIn: jest.fn((_id, password) => (password === 'valid' ? Promise.resolve() : Promise.reject())),
}));

jest.mock('../../../../api/settings', () => ({
  getLoginMessage: jest.fn().mockResolvedValue({ message: '' }),
}));

jest.mock('design-system', () => ({
  ...jest.requireActual('design-system'),
  addGlobalErrorMessage: jest.fn(),
}));

const originalLocation = window.location;
const replace = jest.fn();
const customLoginMessage = 'Welcome to SQ! Please use your Skynet credentials';

beforeAll(() => {
  const location = {
    ...window.location,
    replace,
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

beforeEach(jest.clearAllMocks);

it('should behave correctly', async () => {
  const user = userEvent.setup();

  renderLoginContainer();

  expect(await ui.header.find()).toBeInTheDocument();

  // OAuth provider.
  expect(ui.githubButton.get()).toBeInTheDocument();
  expect(ui.githubImage.get()).toHaveAttribute('src', '/path/icon.svg');

  // Login form collapsed by default.
  expect(ui.loginInput.query()).not.toBeInTheDocument();

  // Open login form, log in.
  await user.click(ui.loginOptionsButton.get());

  const cancelLink = await ui.backLink.find();
  expect(cancelLink).toBeInTheDocument();
  expect(cancelLink).toHaveAttribute('href', '/');

  const loginField = ui.loginInput.get();
  const passwordField = ui.passwordInput.get();
  const submitButton = ui.submitButton.get();

  // Incorrect login.
  await user.type(loginField, 'janedoe');
  await user.type(passwordField, 'invalid');

  // We are not waiting for async handler to be done, as we assert that button is disabled immediately after
  user.click(submitButton);
  await waitFor(() => {
    expect(submitButton).toBeDisabled();
  });

  await waitFor(() => {
    expect(addGlobalErrorMessage).toHaveBeenCalledWith('login.authentication_failed');
  });

  // Correct login.
  await user.clear(passwordField);
  await user.type(passwordField, 'valid');
  await user.click(submitButton);
  expect(addGlobalErrorMessage).toHaveBeenCalledTimes(1);
  expect(replace).toHaveBeenCalledWith('/some/path');
});

it('should have correct image URL with different baseURL', async () => {
  jest.mocked(getBaseUrl).mockReturnValue('/context');

  renderLoginContainer();

  expect(await ui.header.find()).toBeInTheDocument();
  expect(ui.githubImage.get()).toHaveAttribute('src', '/context/path/icon.svg');
});

it('should not show any OAuth providers if none are configured', async () => {
  jest.mocked(getIdentityProviders).mockResolvedValueOnce({ identityProviders: [] });
  renderLoginContainer();

  expect(await ui.header.find()).toBeInTheDocument();

  // No OAuth providers, login form display by default.
  expect(ui.loginOAuthLink.query()).not.toBeInTheDocument();
  expect(ui.loginInput.get()).toBeInTheDocument();
});

it("should show a warning if there's an authorization error", async () => {
  renderLoginContainer({
    location: mockLocation({ query: { authorizationError: 'true' } }),
  });

  expect(await ui.header.find()).toBeInTheDocument();

  expect(ui.unauthorizedAccessText.get()).toBeInTheDocument();
});

it('should display a login message if enabled & provided', async () => {
  jest.mocked(getLoginMessage).mockResolvedValueOnce({ message: customLoginMessage });
  renderLoginContainer({});

  expect(await ui.customLoginText.find()).toBeInTheDocument();
});

it('should handle errors', async () => {
  jest.mocked(getLoginMessage).mockRejectedValueOnce('nope');
  renderLoginContainer({});

  expect(await ui.header.find()).toBeInTheDocument();
});

function renderLoginContainer(props: Partial<LoginContainer['props']> = {}) {
  return renderComponent(
    <LoginContainer location={mockLocation({ query: { return_to: '/some/path' } })} {...props} />,
  );
}

const ui = {
  customLoginText: byText(customLoginMessage),
  header: byRole('heading', { name: 'login.login_to_sonarqube' }),
  loginInput: byLabelText(/login/),
  passwordInput: byLabelText(/password/),
  backLink: byRole('link', { name: 'go_back' }),
  githubImage: byRole('img', { name: 'Github' }),
  githubButton: byRole('button', { name: 'Github login.login_with_x.Github' }),
  loginOAuthLink: byRole('link', { name: 'login.login_with_x' }),
  loginOptionsButton: byRole('button', { name: 'login.more_options' }),
  submitButton: byRole('button', { name: 'sessions.log_in' }),
  unauthorizedAccessText: byText('login.unauthorized_access_alert'),
};
