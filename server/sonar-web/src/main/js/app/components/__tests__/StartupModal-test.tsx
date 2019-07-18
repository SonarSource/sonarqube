/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as differenceInDays from 'date-fns/difference_in_days';
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { toShortNotSoISOString } from 'sonar-ui-common/helpers/dates';
import { hasMessage } from 'sonar-ui-common/helpers/l10n';
import { get, save } from 'sonar-ui-common/helpers/storage';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { showLicense } from '../../../api/marketplace';
import { EditionKey } from '../../../apps/marketplace/utils';
import { mockOrganization, mockRouter } from '../../../helpers/testMocks';
import { ModalKey, StartupModal } from '../StartupModal';

jest.mock('../../../api/marketplace', () => ({
  showLicense: jest.fn().mockResolvedValue(undefined)
}));

jest.mock('sonar-ui-common/helpers/storage', () => ({
  get: jest.fn(),
  save: jest.fn()
}));

jest.mock('sonar-ui-common/helpers/l10n', () => ({
  hasMessage: jest.fn().mockReturnValue(true)
}));

jest.mock('sonar-ui-common/helpers/dates', () => ({
  parseDate: jest.fn().mockReturnValue('parsed-date'),
  toShortNotSoISOString: jest.fn().mockReturnValue('short-not-iso-date')
}));

jest.mock('date-fns/difference_in_days', () => jest.fn().mockReturnValue(1));

const LOGGED_IN_USER: T.LoggedInUser = {
  groups: [],
  isLoggedIn: true,
  login: 'luke',
  name: 'Skywalker',
  scmAccounts: []
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
  const wrapper = getWrapper({ currentEdition: EditionKey.community });
  await shouldNotHaveModals(wrapper);
  expect(showLicense).toHaveBeenCalledTimes(0);
  expect(wrapper.find('div').exists()).toBeTruthy();

  await shouldNotHaveModals(getWrapper({ canAdmin: false }));

  (hasMessage as jest.Mock<any>).mockReturnValueOnce(false);
  await shouldNotHaveModals(getWrapper());

  (showLicense as jest.Mock<any>).mockResolvedValueOnce({ isValidEdition: true });
  await shouldNotHaveModals(getWrapper());

  (get as jest.Mock<any>).mockReturnValueOnce('date');
  (differenceInDays as jest.Mock<any>).mockReturnValueOnce(0);
  await shouldNotHaveModals(getWrapper());

  await shouldNotHaveModals(
    getWrapper({
      canAdmin: false,
      currentUser: { ...LOGGED_IN_USER, showOnboardingTutorial: true },
      location: { pathname: '/documentation/' }
    })
  );

  await shouldNotHaveModals(
    getWrapper({
      canAdmin: false,
      currentUser: { ...LOGGED_IN_USER, showOnboardingTutorial: true },
      location: { pathname: '/create-organization' }
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

describe('closeOnboarding', () => {
  it('should set state and skip onboarding', () => {
    const skipOnboarding = jest.fn();
    const wrapper = getWrapper({ skipOnboarding });

    wrapper.setState({ modal: ModalKey.onboarding });
    wrapper.instance().closeOnboarding();

    expect(wrapper.state('modal')).toBe(undefined);

    expect(skipOnboarding).toHaveBeenCalledTimes(1);
  });
});

describe('openProjectOnboarding', () => {
  it('should set state and redirect', () => {
    const push = jest.fn();
    const wrapper = getWrapper({ router: mockRouter({ push }) });

    wrapper.instance().openProjectOnboarding(mockOrganization());

    expect(wrapper.state('modal')).toBe(undefined);

    expect(push).toHaveBeenCalledWith({
      pathname: `/projects/create`,
      state: { organization: 'foo', tab: 'manual' }
    });
  });
});

async function shouldNotHaveModals(wrapper: ShallowWrapper) {
  await waitAndUpdate(wrapper);
  expect(wrapper.find('LicensePromptModal').exists()).toBeFalsy();
}

async function shouldDisplayLicense(wrapper: ShallowWrapper) {
  await waitAndUpdate(wrapper);
  expect(wrapper.find('LicensePromptModal').exists()).toBeTruthy();
}

function getWrapper(props: Partial<StartupModal['props']> = {}) {
  return shallow<StartupModal>(
    <StartupModal
      canAdmin={true}
      currentEdition={EditionKey.enterprise}
      currentUser={LOGGED_IN_USER}
      location={{ pathname: 'foo/bar' }}
      router={{ push: jest.fn() }}
      skipOnboarding={jest.fn()}
      {...props}>
      <div />
    </StartupModal>
  );
}
