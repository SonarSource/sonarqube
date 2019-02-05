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
import AutoProjectCreate from '../AutoProjectCreate';
import {
  mockOrganizationWithAdminActions,
  mockOrganizationWithAlm
} from '../../../../helpers/testMocks';

const almApplication = {
  backgroundColor: 'blue',
  iconPath: 'icon/path',
  installationUrl: 'https://alm.installation.url',
  key: 'github',
  name: 'GitHub'
};

const boundOrganizations: T.Organization[] = [
  mockOrganizationWithAdminActions(mockOrganizationWithAlm({ subscription: 'FREE' })),
  mockOrganizationWithAlm({ key: 'bar', name: 'Bar', subscription: 'FREE' })
];

it('should display the provider app install button', () => {
  expect(shallowRender({ boundOrganizations: [] })).toMatchSnapshot();
});

it('should display the bound organizations dropdown with the remote repositories', () => {
  expect(shallowRender({ organization: 'foo' })).toMatchSnapshot();
});

function shallowRender(props: Partial<AutoProjectCreate['props']> = {}) {
  return shallow(
    <AutoProjectCreate
      almApplication={almApplication}
      boundOrganizations={boundOrganizations}
      onOrganizationUpgrade={jest.fn()}
      onProjectCreate={jest.fn()}
      organization=""
      {...props}
    />
  );
}
