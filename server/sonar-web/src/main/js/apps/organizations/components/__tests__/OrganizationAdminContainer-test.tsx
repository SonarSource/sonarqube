/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Location } from 'history';
import { OrganizationAdmin } from '../OrganizationAdminContainer';
import { Visibility } from '../../../../app/types';

jest.mock('../../../../app/utils/handleRequiredAuthorization', () => ({ default: jest.fn() }));

const locationMock = {} as Location;

it('should render children', () => {
  const organization = {
    canAdmin: true,
    key: 'foo',
    name: 'Foo',
    projectVisibility: Visibility.Public
  };
  expect(
    shallow(
      <OrganizationAdmin organization={organization} location={locationMock}>
        <div>hello</div>
      </OrganizationAdmin>
    )
  ).toMatchSnapshot();
});

it('should not render anything', () => {
  const organization = {
    canAdmin: false,
    key: 'foo',
    name: 'Foo',
    projectVisibility: Visibility.Public
  };
  expect(
    shallow(
      <OrganizationAdmin organization={organization} location={locationMock}>
        <div>hello</div>
      </OrganizationAdmin>
    ).type()
  ).toBeNull();
});
