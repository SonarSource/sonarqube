/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import OrganizationNavigation from '../OrganizationNavigation';
import { Visibility } from '../../../../app/types';

jest.mock('../../../issues/utils', () => ({
  isMySet: () => false
}));

const organization = {
  key: 'foo',
  name: 'Foo',
  canAdmin: false,
  canDelete: false,
  projectVisibility: Visibility.Public
};

it('regular user', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('admin', () => {
  expect(
    getWrapper({ organization: { ...organization, canAdmin: true, canDelete: true } })
  ).toMatchSnapshot();
});

it('undeletable org', () => {
  expect(
    getWrapper({ organization: { ...organization, canAdmin: true, canDelete: false } })
  ).toMatchSnapshot();
});

function getWrapper(props = {}) {
  return shallow(
    <OrganizationNavigation
      location={{ pathname: '/organizations/foo' }}
      organization={organization}
      {...props}
    />
  );
}
