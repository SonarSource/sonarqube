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
import SystemUpgradeForm from '../SystemUpgradeForm';

const UPGRADES = [
  [
    {
      version: '6.4',
      description: 'Version 6.4 description',
      releaseDate: '2017-06-02',
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
    }
  ],
  [
    {
      version: '5.6.7',
      description: 'Version 5.6.7 description',
      releaseDate: '2017-03-01',
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
      version: '5.6.5',
      description: 'Version 5.6.5 description',
      releaseDate: '2017-03-01',
      changeLogUrl: 'changelogurl',
      downloadUrl: 'downloadurl',
      plugins: {}
    }
  ]
];

it('should display correctly', () => {
  expect(
    shallow(<SystemUpgradeForm onClose={jest.fn()} systemUpgrades={UPGRADES} />)
  ).toMatchSnapshot();
});
