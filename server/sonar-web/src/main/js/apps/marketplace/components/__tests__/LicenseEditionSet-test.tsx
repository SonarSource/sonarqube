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
/* eslint-disable import/order */
import * as React from 'react';
import { shallow } from 'enzyme';
import { change, waitAndUpdate } from '../../../../helpers/testUtils';
import LicenseEditionSet from '../LicenseEditionSet';

jest.mock('../../../../api/marketplace', () => ({
  getLicensePreview: jest.fn(() =>
    Promise.resolve({ nextEditionKey: 'foo', previewStatus: 'NO_INSTALL' })
  ),
  getFormData: jest.fn(() => Promise.resolve({ serverId: 'foo', ncloc: 1000 }))
}));

jest.mock('lodash', () => {
  const lodash = require.requireActual('lodash');
  lodash.debounce = (fn: Function) => (...args: any[]) => fn(...args);
  return lodash;
});

const getLicensePreview = require('../../../../api/marketplace').getLicensePreview as jest.Mock<
  any
>;

const DEFAULT_EDITION = {
  key: 'foo',
  name: 'Foo',
  textDescription: 'Foo desc',
  downloadUrl: 'download_url',
  homeUrl: 'more_url',
  licenseRequestUrl: 'license_url'
};

beforeEach(() => {
  getLicensePreview.mockClear();
});

it('should display correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should display the get license link with parameters', async () => {
  const wrapper = getWrapper();
  await waitAndUpdate(wrapper);
  expect(wrapper.find('a')).toMatchSnapshot();
});

it('should correctly display status message after checking license', async () => {
  let wrapper = await testLicenseStatus('NO_INSTALL', jest.fn());
  expect(wrapper.find('p.alert')).toMatchSnapshot();
  wrapper = await testLicenseStatus('AUTOMATIC_INSTALL', jest.fn());
  expect(wrapper.find('p.alert')).toMatchSnapshot();
  wrapper = await testLicenseStatus('MANUAL_INSTALL', jest.fn());
  expect(wrapper.find('p.alert')).toMatchSnapshot();
});

it('should display terms of license checkbox', async () => {
  let updateLicense = jest.fn();
  let wrapper = await testLicenseStatus('NO_INSTALL', updateLicense);
  expect(wrapper.find('.js-edition-tos').exists()).toBeFalsy();
  expect(updateLicense).toHaveBeenCalledWith('mylicense', 'NO_INSTALL');

  updateLicense = jest.fn();
  wrapper = await testLicenseStatus('AUTOMATIC_INSTALL', updateLicense);
  const tosCheckbox = wrapper.find('.js-edition-tos');
  expect(tosCheckbox.find('a').exists()).toBeTruthy();
  expect(updateLicense).toHaveBeenLastCalledWith(undefined, 'AUTOMATIC_INSTALL');
  (tosCheckbox.find('Checkbox').prop('onCheck') as Function)(true);
  expect(updateLicense).toHaveBeenLastCalledWith('mylicense', 'AUTOMATIC_INSTALL');

  updateLicense = jest.fn();
  wrapper = await testLicenseStatus('MANUAL_INSTALL', updateLicense);
  expect(wrapper.find('.js-edition-tos').exists()).toBeTruthy();
  expect(updateLicense).toHaveBeenLastCalledWith(undefined, 'MANUAL_INSTALL');
});

function getWrapper(props = {}) {
  return shallow(
    <LicenseEditionSet
      edition={DEFAULT_EDITION}
      editions={[DEFAULT_EDITION]}
      updateLicense={jest.fn()}
      {...props}
    />
  );
}

async function testLicenseStatus(status: string, updateLicense: jest.Mock<any>) {
  getLicensePreview.mockImplementation(() =>
    Promise.resolve({ nextEditionKey: 'foo', previewStatus: status })
  );
  const wrapper = getWrapper({ updateLicense });
  change(wrapper.find('textarea'), 'mylicense');
  expect(getLicensePreview).toHaveBeenCalled();
  await new Promise(setImmediate);
  expect(updateLicense).toHaveBeenCalled();
  wrapper.update();
  return wrapper;
}
