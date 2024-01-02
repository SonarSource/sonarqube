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
import UserTokensMock from '../../../api/mocks/UserTokensMock';
import handleRequiredAuthentication from '../../../helpers/handleRequiredAuthentication';
import { sendUserToken } from '../../../helpers/sonarlint';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { CurrentUser } from '../../../types/users';
import SonarLintConnection from '../SonarLintConnection';

jest.mock('../../../helpers/handleRequiredAuthentication', () => ({
  __esModule: true,
  default: jest.fn(),
}));

jest.mock('../../../helpers/sonarlint', () => {
  const original = jest.requireActual('../../../helpers/sonarlint');
  return { ...original, sendUserToken: jest.fn() };
});

let tokenMock: UserTokensMock;

beforeAll(() => {
  tokenMock = new UserTokensMock();
});

afterEach(() => {
  tokenMock.reset();
});

beforeEach(() => {
  jest.clearAllMocks();
});

jest.mock('../../../api/settings', () => {
  const { SettingsKey } = jest.requireActual('../../../types/settings');
  return {
    ...jest.requireActual('../../../api/settings'),
    getAllValues: jest.fn().mockResolvedValue([
      {
        key: SettingsKey.TokenMaxAllowedLifetime,
        value: 'No expiration',
      },
    ]),
  };
});

it('should allow the user to accept the binding request', async () => {
  (sendUserToken as jest.Mock).mockReturnValueOnce({});

  const user = userEvent.setup();
  renderSonarLintConnection();

  expect(
    await screen.findByRole('heading', { name: 'sonarlint-connection.request.title' }),
  ).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'sonarlint-connection.request.action' }));

  expect(
    await screen.findByText('sonarlint-connection.success.description', { exact: false }),
  ).toBeInTheDocument();
});

it('should handle token generation errors', async () => {
  tokenMock.failNextTokenGeneration();

  const user = userEvent.setup();
  renderSonarLintConnection();

  await user.click(
    await screen.findByRole('button', { name: 'sonarlint-connection.request.action' }),
  );

  expect(
    await screen.findByText('sonarlint-connection.token-error.description'),
  ).toBeInTheDocument();
});

it('should handle connection errors', async () => {
  (sendUserToken as jest.Mock).mockRejectedValueOnce(new Error(''));

  const user = userEvent.setup();
  renderSonarLintConnection();

  await user.click(
    await screen.findByRole('button', { name: 'sonarlint-connection.request.action' }),
  );

  expect(
    await screen.findByText('sonarlint-connection.connection-error.description'),
  ).toBeInTheDocument();

  const tokenValue = tokenMock.getLastToken()?.token ?? '';
  expect(await screen.findByText(tokenValue)).toBeInTheDocument();
});

it('should require authentication if user is not logged in', () => {
  renderSonarLintConnection({ currentUser: mockCurrentUser() });

  expect(handleRequiredAuthentication).toHaveBeenCalled();
});

it('should let the user copy the token if the port is not valid', async () => {
  const user = userEvent.setup();

  renderSonarLintConnection({ port: '' });

  await user.click(
    await screen.findByRole('button', { name: 'sonarlint-connection.request.action' }),
  );

  expect(
    await screen.findByText('sonarlint-connection.connection-error.description'),
  ).toBeInTheDocument();
});

function renderSonarLintConnection(overrides: { currentUser?: CurrentUser; port?: string } = {}) {
  const { currentUser, port } = {
    currentUser: mockLoggedInUser(),
    port: '64120',
    ...overrides,
  };

  const searchParams = new URLSearchParams();

  if (port) {
    searchParams.set('port', port);
  }

  renderApp('sonarlint/auth', <SonarLintConnection />, {
    currentUser,
    navigateTo: `sonarlint/auth?${searchParams.toString()}`,
  });
}
