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
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { showLicense } from '../../../api/editions';
import { getEdition } from '../../../helpers/editions';
import { save } from '../../../helpers/storage';
import { mockAppState, mockCurrentUser } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { EditionKey } from '../../../types/editions';
import { LoggedInUser } from '../../../types/users';
import { StartupModal } from '../StartupModal';

jest.mock('../../../api/editions', () => ({
  showLicense: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('../../../helpers/storage', () => ({
  get: jest.fn(),
  save: jest.fn(),
}));

jest.mock('../../../helpers/dates', () => ({
  parseDate: jest.fn().mockReturnValue('parsed-date'),
  toShortISO8601String: jest.fn().mockReturnValue('short-not-iso-date'),
}));

jest.mock('date-fns', () => ({
  ...jest.requireActual('date-fns'),
  differenceInDays: jest.fn().mockReturnValue(1),
}));

const LOGGED_IN_USER: LoggedInUser = {
  groups: [],
  isLoggedIn: true,
  login: 'luke',
  name: 'Skywalker',
  scmAccounts: [],
  dismissedNotices: {},
};

beforeEach(() => {
  jest.clearAllMocks();
});

it('should check license and open on its own', async () => {
  const user = userEvent.setup();
  renderStartupModal();

  expect(await ui.modalHeader.find()).toBeInTheDocument();
  expect(save).toHaveBeenCalled();

  await user.click(ui.licensePageLink.get());

  expect(byText('/admin/extension/license/app').get()).toBeInTheDocument();
});

it.each([
  [
    getEdition(EditionKey.community).name,
    { appState: mockAppState({ canAdmin: true, edition: EditionKey.community }) },
  ],
  ['Cannot admin', { appState: mockAppState({ canAdmin: false, edition: EditionKey.enterprise }) }],
  ['User is not logged in', { currentUser: mockCurrentUser() }],
])('should not open when not necessary: %s', (_, props: Partial<StartupModal['props']>) => {
  renderStartupModal(props);

  expect(showLicense).not.toHaveBeenCalled();

  expect(save).not.toHaveBeenCalled();
  expect(ui.modalHeader.query()).not.toBeInTheDocument();
});

function renderStartupModal(props: Partial<StartupModal['props']> = {}) {
  return renderApp(
    '/',
    <StartupModal
      appState={mockAppState({ edition: EditionKey.enterprise, canAdmin: true })}
      currentUser={LOGGED_IN_USER}
      {...props}
    >
      <div />
    </StartupModal>,
  );
}

const ui = {
  modalHeader: byRole('heading', { name: 'license.prompt.title' }),
  licensePageLink: byRole('link', { name: 'license.prompt.link' }),
  modalCancelButton: byRole('button', { name: 'cancel' }),
};
