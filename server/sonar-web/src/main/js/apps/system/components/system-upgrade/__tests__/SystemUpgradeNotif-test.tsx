/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as React from 'react';
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getSystemUpgrades } from '../../../../../api/system';
import SystemUpgradeNotif from '../SystemUpgradeNotif';

jest.mock('../../../../../api/system', () => ({
  getSystemUpgrades: jest.fn(() =>
    Promise.resolve({
      updateCenterRefresh: '',
      upgrades: [
        {
          version: '5.6.7',
          description: 'Version 5.6.7 description',
          releaseDate: '2017-03-01',
          changeLogUrl: 'changelogurl',
          downloadUrl: 'downloadurl',
          plugins: {}
        },
        {
          version: '5.6.5',
          description: 'Version 5.6.5 description',
          releaseDate: '2017-03-01',
          changeLogUrl: 'changelogurl',
          downloadUrl: 'downloadurl',
          plugins: {}
        },
        {
          version: '6.3',
          description: 'Version 6.3 description',
          releaseDate: '2017-05-02',
          changeLogUrl: 'changelogurl',
          downloadUrl: 'downloadurl',
          plugins: {}
        },
        {
          version: '5.6.6',
          description: 'Version 5.6.6 description',
          releaseDate: '2017-04-02',
          changeLogUrl: 'changelogurl',
          downloadUrl: 'downloadurl',
          plugins: {}
        },
        {
          version: '6.4',
          description: 'Version 6.4 description',
          releaseDate: '2017-06-02',
          changeLogUrl: 'changelogurl',
          downloadUrl: 'downloadurl',
          plugins: {}
        }
      ]
    })
  )
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getSystemUpgrades).toHaveBeenCalled();

  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('Button'));
  expect(wrapper).toMatchSnapshot();
});

it('should display nothing', async () => {
  (getSystemUpgrades as jest.Mock).mockImplementationOnce(() => {
    return Promise.resolve({ updateCenterRefresh: '', upgrades: [] });
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.type()).toBeNull();
});

function shallowRender() {
  return shallow<SystemUpgradeNotif>(<SystemUpgradeNotif />);
}
