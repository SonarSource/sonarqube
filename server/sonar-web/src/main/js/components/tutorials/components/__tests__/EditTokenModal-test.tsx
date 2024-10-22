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
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockUserToken } from '../../../../helpers/mocks/token';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { computeTokenExpirationDate } from '../../../../helpers/tokens';
import { Permissions } from '../../../../types/permissions';
import { TokenType } from '../../../../types/token';
import EditTokenModal from '../EditTokenModal';

jest.mock('../../../../api/settings', () => ({
  getAllValues: jest.fn().mockResolvedValue([]),
}));

let tokenMock: UserTokensMock;

beforeAll(() => {
  tokenMock = new UserTokensMock();
  tokenMock.tokens.push(mockUserToken({ name: 'Analyze "Foo"' }));
});

afterEach(() => {
  tokenMock.reset();
});

it('should behave correctly', async () => {
  renderEditTokenModal();
  const user = userEvent.setup();

  expect(
    screen.getByRole('heading', { name: 'onboarding.token.generate.PROJECT_ANALYSIS_TOKEN' }),
  ).toBeInTheDocument();
  expect(screen.getByText('onboarding.token.text.PROJECT_ANALYSIS_TOKEN')).toBeInTheDocument();

  // Renders form correctly.
  await screen.findByLabelText('onboarding.token.name.label');
  // Should be getByLabelText(), but this is due to a limitation with React Select.
  expect(screen.getByText('users.tokens.expires_in')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'continue' })).toBeInTheDocument();

  // Sets a default token name.
  const tokenNameInput = screen.getByLabelText('onboarding.token.name.label');
  expect(tokenNameInput).toHaveValue('Analyze "Foo" 1');

  // Change name and expiration date.
  await typeInField(user, tokenNameInput, 'new token name');
  await user.click(screen.getByText('users.tokens.expiration.30'));
  await user.click(screen.getByText('users.tokens.expiration.365'));

  // Generate token.
  await clickButton(user, 'onboarding.token.generate');
  let lastToken = tokenMock.getLastToken();
  if (lastToken === undefined) {
    throw new Error("Couldn't find the latest generated token.");
  }

  expect(lastToken.type).toBe(TokenType.Project);
  expect(lastToken.expirationDate).toBe(computeTokenExpirationDate(365));
  expect(screen.getByText(`users.tokens.new_token_created.${lastToken.token}`)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'copy_to_clipboard' })).toBeInTheDocument();

  // Revoke token.
  await clickButton(user, 'onboarding.token.delete');
  expect(tokenMock.tokens.map((t) => t.name)).not.toContain(lastToken.name);

  // Generate a new token.
  await typeInField(
    user,
    screen.getByLabelText('onboarding.token.name.label'),
    'another token name',
  );
  await clickButton(user, 'onboarding.token.generate');

  lastToken = tokenMock.getLastToken();
  if (lastToken === undefined) {
    throw new Error("Couldn't find the latest generated token.");
  }
  expect(lastToken.type).toBe(TokenType.Project);
  expect(lastToken.expirationDate).toBe(computeTokenExpirationDate(365));
  expect(screen.getByText(`users.tokens.new_token_created.${lastToken.token}`)).toBeInTheDocument();
});

it('should allow setting a preferred token type', async () => {
  renderEditTokenModal({
    preferredTokenType: TokenType.Global,
    currentUser: mockLoggedInUser({ permissions: { global: [Permissions.Scan] } }),
  });
  const user = userEvent.setup();

  await screen.findByLabelText('onboarding.token.name.label');

  await clickButton(user, 'onboarding.token.generate');
  const lastToken = tokenMock.getLastToken();
  if (lastToken === undefined) {
    throw new Error("Couldn't find the latest generated token.");
  }
  expect(lastToken.type).toBe(TokenType.Global);
});

it('should fallback to project tokens if the user cannot generate global tokens', async () => {
  renderEditTokenModal({
    preferredTokenType: TokenType.Global,
  });
  const user = userEvent.setup();

  await screen.findByLabelText('onboarding.token.name.label');

  await clickButton(user, 'onboarding.token.generate');
  const lastToken = tokenMock.getLastToken();
  if (lastToken === undefined) {
    throw new Error("Couldn't find the latest generated token.");
  }
  expect(lastToken.type).toBe(TokenType.Project);
});

function renderEditTokenModal(props: Partial<EditTokenModal['props']> = {}) {
  return renderComponent(
    <EditTokenModal
      component={mockComponent({ name: 'Foo' })}
      currentUser={mockLoggedInUser()}
      onClose={jest.fn()}
      {...props}
    />,
  );
}

async function clickButton(user: UserEvent, name: string) {
  await user.click(screen.getByRole('button', { name }));
}

async function typeInField(user: UserEvent, input: HTMLElement, value: string) {
  await user.clear(input);
  await user.type(input, value);
}
