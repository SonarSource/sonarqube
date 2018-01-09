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
import { click } from '../../../../helpers/testUtils';
import LicenseEditionForm from '../LicenseEditionForm';

jest.mock('../../../../api/marketplace', () => ({
  applyLicense: jest.fn(() =>
    Promise.resolve({ nextEditionKey: 'foo', installationStatus: 'AUTOMATIC_IN_PROGRESS' })
  )
}));

const applyLicense = require('../../../../api/marketplace').applyLicense as jest.Mock<any>;

const DEFAULT_EDITION = {
  key: 'foo',
  name: 'Foo',
  textDescription: 'Foo desc',
  downloadUrl: 'download_url',
  homeUrl: 'more_url',
  licenseRequestUrl: 'license_url'
};

beforeEach(() => {
  applyLicense.mockClear();
});

it('should display correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should correctly change the button based on the status and license', () => {
  const wrapper = getWrapper();
  let button;
  (wrapper.instance() as LicenseEditionForm).mounted = true;

  wrapper.setState({ license: 'mylicense', status: 'NO_INSTALL' });
  button = wrapper.find('button');
  expect(button.text()).toBe('save');
  expect(button.prop('disabled')).toBeFalsy();

  wrapper.setState({ license: undefined, status: 'MANUAL_INSTALL' });
  button = wrapper.find('button');
  expect(button.text()).toBe('save');
  expect(button.prop('disabled')).toBeTruthy();

  wrapper.setState({ status: 'AUTOMATIC_INSTALL' });
  button = wrapper.find('button');
  expect(button.text()).toContain('install');
  expect(button.prop('disabled')).toBeTruthy();

  wrapper.setState({ license: 'mylicense' });
  expect(wrapper.find('button').prop('disabled')).toBeFalsy();
});

it('should update the edition status after install', async () => {
  const updateEditionStatus = jest.fn();
  const wrapper = getWrapper({ updateEditionStatus });
  const form = wrapper.instance() as LicenseEditionForm;
  form.handleLicenseChange('mylicense', 'AUTOMATIC_INSTALL');
  wrapper.update();
  click(wrapper.find('button'));
  expect(applyLicense).toHaveBeenCalledWith({ license: 'mylicense' });
  await new Promise(setImmediate);
  expect(updateEditionStatus).toHaveBeenCalledWith({
    nextEditionKey: 'foo',
    installationStatus: 'AUTOMATIC_IN_PROGRESS'
  });
});

function getWrapper(props = {}) {
  return shallow(
    <LicenseEditionForm
      edition={DEFAULT_EDITION}
      editions={[DEFAULT_EDITION]}
      isDowngrade={false}
      onClose={jest.fn()}
      updateEditionStatus={jest.fn()}
      {...props}
    />
  );
}
