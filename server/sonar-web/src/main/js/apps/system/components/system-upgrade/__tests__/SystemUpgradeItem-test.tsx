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
import * as React from 'react';
import SystemUpgradeItem from '../SystemUpgradeItem';

const UPGRADES = [
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
];

it('should display correctly', () => {
  const wrapper = shallow(<SystemUpgradeItem systemUpgrades={UPGRADES} />);
  expect(wrapper).toMatchSnapshot();
});

it('should display a badge', () => {
  const wrapper = shallow(<SystemUpgradeItem systemUpgrades={UPGRADES} type="LTS Version" />);
  expect(wrapper.find('h1').exists()).toBeTruthy();
});
