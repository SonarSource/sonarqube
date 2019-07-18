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
import AlmRepositoryItem from '../AlmRepositoryItem';

const identityProviders = {
  backgroundColor: 'blue',
  iconPath: 'icon/path',
  key: 'foo',
  name: 'Foo Provider'
};

const repositories = [
  {
    label: 'Cool Project',
    installationKey: 'github/cool',
    linkedProjectKey: 'proj_cool',
    linkedProjectName: 'Proj Cool'
  },
  {
    label: 'Awesome Project',
    installationKey: 'github/awesome'
  }
];

it('should render correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should render selected', () => {
  expect(getWrapper({ selected: true })).toMatchSnapshot();
});

it('should render imported', () => {
  expect(getWrapper({ repository: repositories[0] })).toMatchSnapshot();
});

it('should render disabed', () => {
  expect(getWrapper({ disabled: true, repository: repositories[1] })).toMatchSnapshot();
});

it('should render private repositories', () => {
  expect(getWrapper({ repository: { ...repositories[1], private: true } })).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <AlmRepositoryItem
      disabled={false}
      highlightUpgradeBox={jest.fn()}
      identityProvider={identityProviders}
      repository={repositories[1]}
      selected={false}
      toggleRepository={jest.fn()}
      {...props}
    />
  );
}
