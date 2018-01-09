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
import UninstallEditionForm from '../UninstallEditionForm';

jest.mock('../../../../api/marketplace', () => ({
  uninstallEdition: jest.fn(() => Promise.resolve())
}));

const uninstallEdition = require('../../../../api/marketplace').uninstallEdition as jest.Mock<any>;

const DEFAULT_EDITION = {
  key: 'foo',
  name: 'Foo',
  textDescription: 'Foo desc',
  downloadUrl: 'download_url',
  homeUrl: 'more_url',
  licenseRequestUrl: 'license_url'
};

beforeEach(() => {
  uninstallEdition.mockClear();
});

it('should display correctly', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should update the edition status after uninstall', async () => {
  const updateEditionStatus = jest.fn();
  const wrapper = getWrapper({ updateEditionStatus });
  (wrapper.instance() as UninstallEditionForm).mounted = true;
  click(wrapper.find('button'));
  expect(uninstallEdition).toHaveBeenCalled();
  await new Promise(setImmediate);
  expect(updateEditionStatus).toHaveBeenCalledWith({
    currentEditionKey: undefined,
    installationStatus: 'UNINSTALL_IN_PROGRESS'
  });
});

function getWrapper(props = {}) {
  return shallow(
    <UninstallEditionForm
      edition={DEFAULT_EDITION}
      editionStatus={{ currentEditionKey: 'foo', installationStatus: 'NONE' }}
      onClose={jest.fn()}
      updateEditionStatus={jest.fn()}
      {...props}
    />
  );
}
