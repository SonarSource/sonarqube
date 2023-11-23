/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { getLoginMessage } from '../../../../api/settings';
import { getIdentityProviders } from '../../../../api/users';
import { addGlobalErrorMessage } from '../../../../helpers/globalMessages';
import { mockLocation } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { LoginContainer } from '../LoginContainer';

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

jest.mock('../../../../helpers/globalMessages', () => ({
  addGlobalErrorMessage: jest.fn(),
}));

const originalLocation = window.location;
const replace = jest.fn();

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

  const heading = await screen.findByRole('heading', { name: 'login.login_to_sonarqube' });
  expect(heading).toBeInTheDocument();

  // OAuth provider.
  const link = screen.getByRole('link', { name: 'Github login.login_with_x.Github' });
  expect(link).toBeInTheDocument();
  expect(link).toHaveAttribute('href', '/sessions/init/github?return_to=%2Fsome%2Fpath');
  expect(link).toMatchSnapshot('OAuthProvider link');

  // Login form collapsed by default.
  expect(screen.queryByLabelText('login')).not.toBeInTheDocument();

  // Open login form, log in.
  await user.click(screen.getByRole('button', { name: 'login.more_options' }));

  const cancelLink = await screen.findByRole('link', { name: 'cancel' });
  expect(cancelLink).toBeInTheDocument();
  expect(cancelLink).toHaveAttribute('href', '/');

  const loginField = screen.getByLabelText('login');
  const passwordField = screen.getByLabelText('password');
  const submitButton = screen.getByRole('button', { name: 'sessions.log_in' });

  // Incorrect login.
  await user.type(loginField, 'janedoe');
  await user.type(passwordField, 'invalid');
  // Don't use userEvent.click() here. This allows us to more easily see the loading state changes.
  submitButton.click();
  await waitFor(() => expect(submitButton).toBeDisabled()); // Loading.
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

it('should not show any OAuth providers if none are configured', async () => {
  (getIdentityProviders as jest.Mock).mockResolvedValueOnce({ identityProviders: [] });
  renderLoginContainer();

  const heading = await screen.findByRole('heading', { name: 'login.login_to_sonarqube' });
  expect(heading).toBeInTheDocument();

  // No OAuth providers, login form display by default.
  expect(
    screen.queryByRole('link', { name: 'login.login_with_x', exact: false }),
  ).not.toBeInTheDocument();
  expect(screen.getByLabelText('login')).toBeInTheDocument();
});

it("should show a warning if there's an authorization error", async () => {
  renderLoginContainer({
    location: mockLocation({ query: { authorizationError: 'true' } }),
  });

  const heading = await screen.findByRole('heading', { name: 'login.login_to_sonarqube' });
  expect(heading).toBeInTheDocument();

  expect(screen.getByText('login.unauthorized_access_alert')).toBeInTheDocument();
});

it('should display a login message if enabled & provided', async () => {
  const message = 'Welcome to SQ! Please use your Skynet credentials';
  (getLoginMessage as jest.Mock).mockResolvedValueOnce({ message });
  renderLoginContainer({});

  expect(await screen.findByText(message)).toBeInTheDocument();
});

it('should handle errors', async () => {
  (getLoginMessage as jest.Mock).mockRejectedValueOnce('nope');
  renderLoginContainer({});

  const heading = await screen.findByRole('heading', { name: 'login.login_to_sonarqube' });
  expect(heading).toBeInTheDocument();
});

function renderLoginContainer(props: Partial<LoginContainer['props']> = {}) {
  return renderComponent(
    <LoginContainer location={mockLocation({ query: { return_to: '/some/path' } })} {...props} />,
  );
}
