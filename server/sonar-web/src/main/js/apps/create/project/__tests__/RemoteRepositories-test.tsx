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
import { shallow } from 'enzyme';
import { times } from 'lodash';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getRepositories } from '../../../../api/alm-integration';
import {
  mockOrganizationWithAdminActions,
  mockOrganizationWithAlm
} from '../../../../helpers/testMocks';
import RemoteRepositories from '../RemoteRepositories';

jest.mock('../../../../api/alm-integration', () => ({
  getRepositories: jest.fn().mockResolvedValue({
    repositories: [
      {
        label: 'Cool Project',
        installationKey: 'github/cool'
      },
      { label: 'Awesome Project', installationKey: 'github/awesome' }
    ]
  })
}));

const almApplication = {
  backgroundColor: 'blue',
  iconPath: 'icon/path',
  installationUrl: 'https://alm.installation.url',
  key: 'github',
  name: 'GitHub'
};

const organization: T.Organization = mockOrganizationWithAlm({ subscription: 'FREE' });

beforeEach(() => {
  (getRepositories as jest.Mock<any>).mockClear();
});

it('should display the list of repositories', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(getRepositories).toHaveBeenCalledWith({ organization: 'foo' });
});

it('should display the organization upgrade box', async () => {
  (getRepositories as jest.Mock<any>).mockResolvedValueOnce({
    repositories: [{ label: 'Foo Project', installationKey: 'github/foo', private: true }]
  });
  const wrapper = shallowRender({ organization: { ...organization, actions: { admin: true } } });
  await waitAndUpdate(wrapper);
  expect(wrapper.find('UpgradeOrganizationBox')).toMatchSnapshot();
  wrapper.find('UpgradeOrganizationBox').prop<Function>('onOrganizationUpgrade')();
  expect(wrapper.find('Alert[variant="success"]').exists()).toBe(true);
});

it('should not display the organization upgrade box', () => {
  (getRepositories as jest.Mock<any>).mockResolvedValueOnce({
    repositories: [{ label: 'Bar Project', installationKey: 'github/bar', private: true }]
  });
  const wrapper = shallowRender({
    organization: mockOrganizationWithAdminActions(
      mockOrganizationWithAlm({ subscription: 'PAID' })
    )
  });

  expect(wrapper.find('UpgradeOrganizationBox').exists()).toBe(false);
});

it('should display a search box to filter repositories', async () => {
  (getRepositories as jest.Mock<any>).mockResolvedValueOnce({
    repositories: times(6, i => ({ label: `Project ${i}`, installationKey: `key-${i}` }))
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.find('SearchBox').exists()).toBe(true);
  expect(wrapper.find('AlmRepositoryItem')).toHaveLength(6);
  wrapper.find('SearchBox').prop<Function>('onChange')('3');
  expect(wrapper.find('AlmRepositoryItem')).toHaveLength(1);
});

it('should allow to select all repositories', async () => {
  (getRepositories as jest.Mock<any>).mockResolvedValueOnce({
    repositories: times(6, i => ({ label: `Project ${i}`, installationKey: `key-${i}` }))
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.find('Checkbox')).toHaveLength(1);
  expect(wrapper.state('checkAllRepositories')).toBe(false);
  expect(wrapper.state('selectedRepositories')).toEqual({});
});

it('should select all repositories', async () => {
  (getRepositories as jest.Mock<any>).mockResolvedValueOnce({
    repositories: [
      { label: 'Project 1', installationKey: 'key-1' },
      { label: 'Project 2', installationKey: 'key-2', linkedProjectKey: 'key-2' }
    ]
  });

  const wrapper = shallowRender();
  const instance = wrapper.instance() as RemoteRepositories;
  await waitAndUpdate(wrapper);

  instance.onCheckAllRepositories();
  await waitAndUpdate(wrapper);

  expect(wrapper.state('checkAllRepositories')).toBe(true);
  expect(wrapper.state('selectedRepositories')).toMatchSnapshot();

  instance.onCheckAllRepositories();
  await waitAndUpdate(wrapper);

  expect(wrapper.state('checkAllRepositories')).toBe(false);
  expect(wrapper.state('selectedRepositories')).toEqual({});
});

function shallowRender(props: Partial<RemoteRepositories['props']> = {}) {
  return shallow(
    <RemoteRepositories
      almApplication={almApplication}
      onOrganizationUpgrade={jest.fn()}
      onProjectCreate={jest.fn()}
      organization={organization}
      {...props}
    />
  );
}
