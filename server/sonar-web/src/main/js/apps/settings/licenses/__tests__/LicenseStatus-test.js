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
import React from 'react';
import { shallow } from 'enzyme';
import LicenseStatus from '../LicenseStatus';

it('should render nothing when no value', () => {
  const status = shallow(<LicenseStatus license={{}}/>);
  expect(status.node).toBeNull();
});

it('should render ok', () => {
  const status = shallow(<LicenseStatus license={{ value: 'foo' }}/>);
  expect(status.is('.icon-check')).toBe(true);
});

it('should render error when invalid product', () => {
  const status = shallow(<LicenseStatus license={{ value: 'foo', invalidProduct: true }}/>);
  expect(status.is('.icon-check')).toBe(false);
  expect(status.is('.icon-alert-error')).toBe(true);
});

it('should render error when invalid expiration', () => {
  const status = shallow(<LicenseStatus license={{ value: 'foo', invalidExpiration: true }}/>);
  expect(status.is('.icon-check')).toBe(false);
  expect(status.is('.icon-alert-error')).toBe(true);
});

it('should render error when invalid server id', () => {
  const status = shallow(<LicenseStatus license={{ value: 'foo', invalidServerId: true }}/>);
  expect(status.is('.icon-check')).toBe(false);
  expect(status.is('.icon-alert-error')).toBe(true);
});
