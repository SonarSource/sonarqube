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
import { differenceInDays } from 'date-fns';
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { showLicense } from '../../../api/editions';
import { toShortNotSoISOString } from '../../../helpers/dates';
import { hasMessage } from '../../../helpers/l10n';
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
  toShortNotSoISOString: jest.fn().mockReturnValue('short-not-iso-date'),
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
  (differenceInDays as jest.Mock<any>).mockClear();
  (hasMessage as jest.Mock<any>).mockClear();
  (get as jest.Mock<any>).mockClear();
  (save as jest.Mock<any>).mockClear();
  (showLicense as jest.Mock<any>).mockClear();
  (toShortNotSoISOString as jest.Mock<any>).mockClear();
});

it('should render only the children', async () => {
  const wrapper = getWrapper({ appState: mockAppState({ edition: EditionKey.community }) });
  await shouldNotHaveModals(wrapper);
  expect(showLicense).toHaveBeenCalledTimes(0);
  expect(wrapper.find('div').exists()).toBe(true);

  await shouldNotHaveModals(getWrapper({ appState: mockAppState({ canAdmin: false }) }));

  (hasMessage as jest.Mock<any>).mockReturnValueOnce(false);
  await shouldNotHaveModals(getWrapper());

  (showLicense as jest.Mock<any>).mockResolvedValueOnce({ isValidEdition: true });
  await shouldNotHaveModals(getWrapper());

  (get as jest.Mock<any>).mockReturnValueOnce('date');
  (differenceInDays as jest.Mock<any>).mockReturnValueOnce(0);
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

  (get as jest.Mock<any>).mockReturnValueOnce('date');
  (differenceInDays as jest.Mock<any>).mockReturnValueOnce(1);
  await shouldDisplayLicense(getWrapper());

  (showLicense as jest.Mock<any>).mockResolvedValueOnce({ isValidEdition: false });
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
