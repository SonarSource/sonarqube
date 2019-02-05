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
import * as React from 'react';
import { shallow } from 'enzyme';
import { CreateProjectPageSonarCloud } from '../CreateProjectPageSonarCloud';
import { getAlmAppInfo } from '../../../../api/alm-integration';
import {
  mockRouter,
  mockOrganizationWithAdminActions,
  mockOrganizationWithAlm
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/alm-integration', () => ({
  getAlmAppInfo: jest.fn().mockResolvedValue({
    application: {
      backgroundColor: 'blue',
      iconPath: 'icon/path',
      installationUrl: 'https://alm.installation.url',
      key: 'github',
      name: 'GitHub'
    }
  })
}));

const user: T.LoggedInUser = {
  externalProvider: 'github',
  groups: [],
  isLoggedIn: true,
  login: 'foo',
  name: 'Foo',
  scmAccounts: []
};

beforeEach(() => {
  (getAlmAppInfo as jest.Mock<any>).mockClear();
});

it('should render correctly', async () => {
  const wrapper = getWrapper();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render with Manual creation only', () => {
  expect(getWrapper({ currentUser: { ...user, externalProvider: 'microsoft' } })).toMatchSnapshot();
});

it('should switch tabs', async () => {
  const replace = jest.fn();
  const wrapper = getWrapper({ router: { replace } });
  replace.mockImplementation(location => {
    wrapper.setProps({ location }).update();
  });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper.find('Tabs').prop<Function>('onChange')('manual');
  expect(wrapper.find('ManualProjectCreate').exists()).toBeTruthy();
  wrapper.find('Tabs').prop<Function>('onChange')('auto');
  expect(wrapper.find('AutoProjectCreate').exists()).toBeTruthy();
});

function getWrapper(props = {}) {
  return shallow(
    <CreateProjectPageSonarCloud
      addGlobalErrorMessage={jest.fn()}
      currentUser={user}
      // @ts-ignore avoid passing everything from WithRouterProps
      location={{}}
      router={mockRouter()}
      skipOnboarding={jest.fn()}
      userOrganizations={[
        mockOrganizationWithAdminActions({}, { admin: false, provision: true }),
        mockOrganizationWithAdminActions(mockOrganizationWithAlm({ key: 'bar', name: 'Bar' }), {
          admin: false,
          provision: true
        }),
        mockOrganizationWithAdminActions(
          { key: 'baz', name: 'Baz' },
          { admin: false, provision: false }
        )
      ]}
      {...props}
    />
  );
}
