/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
/* eslint-disable sonarjs/no-duplicate-string */
import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  checkPersonalAccessTokenIsValid,
  setAlmPersonalAccessToken
} from '../../../../api/alm-integrations';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { mockLocation } from '../../../../helpers/testMocks';
import { AlmKeys } from '../../../../types/alm-settings';
import AzureProjectCreate from '../AzureProjectCreate';

jest.mock('../../../../api/alm-integrations', () => {
  return {
    checkPersonalAccessTokenIsValid: jest.fn().mockResolvedValue(true),
    setAlmPersonalAccessToken: jest.fn().mockResolvedValue(null)
  };
});

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should correctly fetch binding info on mount', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalledWith('foo');
});

it('should correctly handle a valid PAT', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(true);
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalled();
  expect(wrapper.state().patIsValid).toBe(true);
});

it('should correctly handle an invalid PAT', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(false);
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalled();
  expect(wrapper.state().patIsValid).toBe(false);
});

it('should correctly handle setting a new PAT', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handlePersonalAccessTokenCreate('token');
  expect(setAlmPersonalAccessToken).toBeCalledWith('foo', 'token');
  expect(wrapper.state().submittingToken).toBe(true);

  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(false);
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalled();
  expect(wrapper.state().submittingToken).toBe(false);
  expect(wrapper.state().tokenValidationFailed).toBe(true);
});

function shallowRender(overrides: Partial<AzureProjectCreate['props']> = {}) {
  return shallow<AzureProjectCreate>(
    <AzureProjectCreate
      canAdmin={true}
      loadingBindings={false}
      location={mockLocation()}
      onProjectCreate={jest.fn()}
      settings={[mockAlmSettingsInstance({ alm: AlmKeys.Azure, key: 'foo' })]}
      {...overrides}
    />
  );
}
