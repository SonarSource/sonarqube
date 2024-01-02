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
import { shallow } from 'enzyme';
import * as React from 'react';
import { EditionKey } from '../../../types/editions';
import SystemUpgradeItem, { SystemUpgradeItemProps } from '../SystemUpgradeItem';

it('should display correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ isLTSVersion: true })).toMatchSnapshot();
  expect(shallowRender({ isPatch: true })).toMatchSnapshot();
  expect(shallowRender({ edition: EditionKey.developer })).toMatchSnapshot();
  expect(shallowRender({ edition: EditionKey.enterprise })).toMatchSnapshot();
  expect(shallowRender({ edition: EditionKey.datacenter })).toMatchSnapshot();
  // Fallback to Community.
  expect(
    shallowRender({
      systemUpgrades: [
        {
          version: '5.6.7',
          description: 'Version 5.6.7 description',
          releaseDate: '2017-03-01',
          changeLogUrl: 'http://changelog.url/',
          downloadUrl: 'http://download.url/community',
        },
      ],
    })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<SystemUpgradeItemProps> = {}) {
  return shallow<SystemUpgradeItemProps>(
    <SystemUpgradeItem
      edition={EditionKey.community}
      systemUpgrades={[
        {
          version: '5.6.7',
          description: 'Version 5.6.7 description',
          releaseDate: '2017-03-01',
          changeLogUrl: 'http://changelog.url/',
          downloadUrl: 'http://download.url/community',
          downloadDeveloperUrl: 'http://download.url/developer',
          downloadEnterpriseUrl: 'http://download.url/enterprise',
          downloadDatacenterUrl: 'http://download.url/datacenter',
        },
        {
          version: '5.6.6',
          description: 'Version 5.6.6 description',
          releaseDate: '2017-04-02',
          changeLogUrl: 'http://changelog.url/',
          downloadUrl: 'http://download.url/community',
          downloadDeveloperUrl: 'http://download.url/developer',
        },
        {
          version: '5.6.5',
          description: 'Version 5.6.5 description',
          releaseDate: '2017-03-01',
          changeLogUrl: 'http://changelog.url/',
          downloadUrl: 'http://download.url/community',
          downloadDeveloperUrl: 'http://download.url/developer',
        },
      ]}
      isPatch={false}
      isLTSVersion={false}
      {...props}
    />
  );
}
