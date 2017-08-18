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
import LicenseRow from '../LicenseRow';
import LicenseStatus from '../LicenseStatus';
import LicenseChangeForm from '../LicenseChangeForm';

it('should render status', () => {
  const license = {};
  const licenseStatus = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    LicenseStatus
  );
  expect(licenseStatus.length).toBe(1);
  expect(licenseStatus.prop('license')).toBe(license);
});

it('should render product', () => {
  const license = { name: 'foo' };
  const licenseProduct = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    '.js-product'
  );
  expect(licenseProduct.length).toBe(1);
  expect(licenseProduct.text()).toContain('foo');
});

it('should render invalid product', () => {
  const license = { product: 'foo', invalidProduct: true };
  const licenseProduct = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    '.js-product'
  );
  expect(licenseProduct.find('.text-danger').length).toBe(1);
});

it('should render key when no name', () => {
  const license = { key: 'foo.secured' };
  const licenseProduct = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    '.js-product'
  );
  expect(licenseProduct.length).toBe(1);
  expect(licenseProduct.text()).toContain('foo.secured');
});

it('should render organization', () => {
  const license = { organization: 'org' };
  const licenseOrg = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    '.js-organization'
  );
  expect(licenseOrg.length).toBe(1);
  expect(licenseOrg.text()).toContain('org');
});

it('should render expiration', () => {
  const license = { expiration: '2015-01-01' };
  const licenseExpiration = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    '.js-expiration'
  );
  expect(licenseExpiration.length).toBe(1);
  expect(licenseExpiration.find('DateFormatter')).toHaveLength(1);
});

it('should render invalid expiration', () => {
  const license = { expiration: '2015-01-01', invalidExpiration: true };
  const licenseExpiration = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    '.js-expiration'
  );
  expect(licenseExpiration.find('.text-danger').length).toBe(1);
});

it('should render type', () => {
  const license = { type: 'PRODUCTION' };
  const licenseType = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    '.js-type'
  );
  expect(licenseType.length).toBe(1);
  expect(licenseType.text()).toContain('PRODUCTION');
});

it('should render server id', () => {
  const license = { serverId: 'bar' };
  const licenseServerId = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    '.js-server-id'
  );
  expect(licenseServerId.length).toBe(1);
  expect(licenseServerId.text()).toContain('bar');
});

it('should render invalid server id', () => {
  const license = { serverId: 'bar', invalidServerId: true };
  const licenseServerId = shallow(<LicenseRow license={license} setLicense={jest.fn()} />).find(
    '.js-server-id'
  );
  expect(licenseServerId.find('.text-danger').length).toBe(1);
});

it('should render change form', () => {
  const license = { key: 'foo' };
  const setLicense = jest.fn(() => Promise.resolve());
  const licenseChangeForm = shallow(<LicenseRow license={license} setLicense={setLicense} />).find(
    LicenseChangeForm
  );
  expect(licenseChangeForm.length).toBe(1);
  expect(licenseChangeForm.prop('license')).toBe(license);
  expect(typeof licenseChangeForm.prop('onChange')).toBe('function');

  licenseChangeForm.prop('onChange')('license-hash');
  expect(setLicense).toBeCalledWith('foo', 'license-hash');
});
