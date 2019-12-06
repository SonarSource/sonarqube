/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  createBitbucketConfiguration,
  updateBitbucketConfiguration
} from '../../../../../api/almSettings';
import { mockBitbucketDefinition } from '../../../../../helpers/mocks/alm-settings';
import BitbucketTab from '../BitbucketTab';

jest.mock('../../../../../api/almSettings', () => ({
  countBindedProjects: jest.fn().mockResolvedValue(2),
  createBitbucketConfiguration: jest.fn().mockResolvedValue({}),
  deleteConfiguration: jest.fn().mockResolvedValue({}),
  updateBitbucketConfiguration: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle cancel', async () => {
  const wrapper = shallowRender();

  wrapper.setState({
    editedDefinition: mockBitbucketDefinition()
  });

  wrapper.instance().handleCancel();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should create config', async () => {
  const onUpdateDefinitions = jest.fn();
  const config = mockBitbucketDefinition();
  const wrapper = shallowRender({ onUpdateDefinitions });
  wrapper.setState({ editedDefinition: config });

  await wrapper.instance().handleSubmit(config, '');

  expect(createBitbucketConfiguration).toBeCalledWith(config);
  expect(onUpdateDefinitions).toBeCalled();
  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should update config', async () => {
  const onUpdateDefinitions = jest.fn();
  const config = mockBitbucketDefinition();
  const wrapper = shallowRender({ onUpdateDefinitions });
  wrapper.setState({ editedDefinition: config });

  await wrapper.instance().handleSubmit(config, 'originalKey');

  expect(updateBitbucketConfiguration).toBeCalledWith({
    newKey: 'key',
    ...config,
    key: 'originalKey'
  });
  expect(onUpdateDefinitions).toBeCalled();
  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should handle create', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handleCreate();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().editedDefinition).toEqual({ key: '', url: '', personalAccessToken: '' });
});

it('should handle edit', async () => {
  const config = {
    key: 'key',
    url: 'url',
    personalAccessToken: 'PAT'
  };
  const wrapper = shallowRender({ definitions: [config] });
  wrapper.instance().handleEdit(config.key);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().editedDefinition).toEqual(config);
});

function shallowRender(props: Partial<BitbucketTab['props']> = {}) {
  return shallow<BitbucketTab>(
    <BitbucketTab
      definitions={[]}
      loading={false}
      onDelete={jest.fn()}
      onUpdateDefinitions={jest.fn()}
      {...props}
    />
  );
}
