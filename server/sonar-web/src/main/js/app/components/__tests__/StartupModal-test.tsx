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
import { differenceInDays } from 'date-fns';
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { showLicense } from '../../../api/editions';
import { toShortISO8601String } from '../../../helpers/dates';
import { hasMessage } from '../../../helpers/l10n';
import { mockLicense } from '../../../helpers/mocks/editions';
import { get, save } from '../../../helpers/storage';
import { mockAppState, mockLocation, mockRouter } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
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

jest.mock('../../../helpers/l10n', () => ({
  hasMessage: jest.fn().mockReturnValue(true),
}));

jest.mock('../../../helpers/dates', () => ({
  parseDate: jest.fn().mockReturnValue('parsed-date'),
  toShortISO8601String: jest.fn().mockReturnValue('short-not-iso-date'),
}));

jest.mock('date-fns', () => ({ differenceInDays: jest.fn().mockReturnValue(1) }));

const LOGGED_IN_USER: LoggedInUser = {
  groups: [],
  isLoggedIn: true,
  login: 'luke',
  name: 'Skywalker',
  scmAccounts: [],
  dismissedNotices: {},
};

beforeEach(() => {
  jest.mocked(differenceInDays).mockClear();
  jest.mocked(hasMessage).mockClear();
  jest.mocked(get).mockClear();
  jest.mocked(save).mockClear();
  jest.mocked(showLicense).mockClear();
  jest.mocked(toShortISO8601String).mockClear();
});

it('should render only the children', async () => {
  const wrapper = getWrapper({ appState: mockAppState({ edition: EditionKey.community }) });
  await shouldNotHaveModals(wrapper);
  expect(showLicense).toHaveBeenCalledTimes(0);
  expect(wrapper.find('div').exists()).toBe(true);

  await shouldNotHaveModals(getWrapper({ appState: mockAppState({ canAdmin: false }) }));

  jest.mocked(hasMessage).mockReturnValueOnce(false);
  await shouldNotHaveModals(getWrapper());

  jest.mocked(showLicense).mockResolvedValueOnce(mockLicense({ isValidEdition: true }));
  await shouldNotHaveModals(getWrapper());

  jest.mocked(get).mockReturnValueOnce('date');
  jest.mocked(differenceInDays).mockReturnValueOnce(0);
  await shouldNotHaveModals(getWrapper());

  await shouldNotHaveModals(
    getWrapper({
      appState: mockAppState({ canAdmin: false }),
      currentUser: { ...LOGGED_IN_USER },
      location: mockLocation({ pathname: '/create-organization' }),
    })
  );
});

it('should render license prompt', async () => {
  await shouldDisplayLicense(getWrapper());
  expect(save).toHaveBeenCalledWith('sonarqube.license.prompt', 'short-not-iso-date', 'luke');

  jest.mocked(get).mockReturnValueOnce('date');
  jest.mocked(differenceInDays).mockReturnValueOnce(1);
  await shouldDisplayLicense(getWrapper());

  jest.mocked(showLicense).mockResolvedValueOnce(mockLicense({ isValidEdition: false }));
  await shouldDisplayLicense(getWrapper());
});

async function shouldNotHaveModals(wrapper: ShallowWrapper) {
  await waitAndUpdate(wrapper);
  expect(wrapper.find('LicensePromptModal').exists()).toBe(false);
}

async function shouldDisplayLicense(wrapper: ShallowWrapper) {
  await waitAndUpdate(wrapper);
  expect(wrapper.find('LicensePromptModal').exists()).toBe(true);
}

function getWrapper(props: Partial<StartupModal['props']> = {}) {
  return shallow<StartupModal>(
    <StartupModal
      appState={mockAppState({ edition: EditionKey.enterprise, canAdmin: true })}
      currentUser={LOGGED_IN_USER}
      location={mockLocation({ pathname: 'foo/bar' })}
      router={mockRouter()}
      {...props}
    >
      <div />
    </StartupModal>
  );
}
