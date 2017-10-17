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
import { click } from '../../../../helpers/testUtils';
import LicenseEditionForm from '../LicenseEditionForm';

jest.mock('../../../../app/utils/getStore', () => {
  const dispatch = jest.fn();
  return { default: () => ({ dispatch }) };
});
jest.mock('../../../../api/marketplace', () => ({
  applyLicense: jest.fn(() =>
    Promise.resolve({ nextEditionKey: 'foo', installationStatus: 'AUTOMATIC_IN_PROGRESS' })
  )
}));

const applyLicense = require('../../../../api/marketplace').applyLicense as jest.Mock<any>;
const getStore = require('../../../../app/utils/getStore').default as jest.Mock<any>;

const DEFAULT_EDITION = {
  key: 'foo',
  name: 'Foo',
  desc: 'Foo desc',
  download_link: 'download_url',
  more_link: 'more_url',
  request_license_link: 'license_url'
};

beforeEach(() => {
  applyLicense.mockClear();
  getStore().dispatch.mockClear();
});

it('should display correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should correctly change the button based on the status', () => {
  const wrapper = getWrapper();
  (wrapper.instance() as LicenseEditionForm).mounted = true;
  wrapper.setState({ status: 'NO_INSTALL' });
  expect(wrapper.find('button')).toMatchSnapshot();
  wrapper.setState({ status: 'AUTOMATIC_INSTALL' });
  expect(wrapper.find('button')).toMatchSnapshot();
  wrapper.setState({ status: 'MANUAL_INSTALL' });
  expect(wrapper.find('button').exists()).toBeFalsy();
});

it('should update the edition status after install', async () => {
  const wrapper = getWrapper();
  const form = wrapper.instance() as LicenseEditionForm;
  form.mounted = true;
  form.handleLicenseChange('mylicense', 'AUTOMATIC_INSTALL');
  click(wrapper.find('button'));
  expect(applyLicense).toHaveBeenCalledWith({ license: 'mylicense' });
  await new Promise(setImmediate);
  expect(getStore().dispatch).toHaveBeenCalled();
});

function getWrapper(props = {}) {
  return shallow(<LicenseEditionForm edition={DEFAULT_EDITION} onClose={jest.fn()} {...props} />);
}
