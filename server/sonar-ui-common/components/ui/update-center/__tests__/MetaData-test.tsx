/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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

import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import MetaData from '../MetaData';
import { mockMetaDataInformation } from '../mocks/update-center-metadata';
import { MetaDataInformation } from '../update-center-metadata';
import { HttpStatus } from '../../../../helpers/request';

beforeAll(() => {
  window.fetch = jest.fn();
});

beforeEach(() => {
  jest.resetAllMocks();
});

it('should render correctly', async () => {
  const metaDataInfo = mockMetaDataInformation();
  mockFetchReturnValue(metaDataInfo);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly with organization', async () => {
  const metaDataInfo = mockMetaDataInformation({
    organization: { name: 'test-org', url: 'test-org-url' },
  });
  mockFetchReturnValue(metaDataInfo);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should not render anything if call for metadata fails', async () => {
  const metaDataInfo = mockMetaDataInformation();
  mockFetchReturnValue(metaDataInfo, HttpStatus.NotFound);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.type()).toBeNull();
});

it('should fetch metadata again if the update center key if modified', async () => {
  const metaDataInfo = mockMetaDataInformation();
  mockFetchReturnValue(metaDataInfo);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(window.fetch).toHaveBeenCalledTimes(1);

  mockFetchReturnValue(metaDataInfo);
  wrapper.setProps({ updateCenterKey: 'abap' });

  expect(window.fetch).toHaveBeenCalledTimes(2);
});

function shallowRender(props?: Partial<MetaData['props']>) {
  return shallow<MetaData>(<MetaData updateCenterKey="apex" {...props} />);
}

function mockFetchReturnValue(metaDataInfo: MetaDataInformation, status = HttpStatus.Ok) {
  (window.fetch as jest.Mock).mockResolvedValueOnce({ status, json: () => metaDataInfo });
}
