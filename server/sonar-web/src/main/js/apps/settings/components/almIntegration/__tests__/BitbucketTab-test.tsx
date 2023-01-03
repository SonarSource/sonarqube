/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  createBitbucketCloudConfiguration,
  createBitbucketConfiguration,
  updateBitbucketCloudConfiguration,
  updateBitbucketConfiguration
} from '../../../../../api/alm-settings';
import {
  mockBitbucketBindingDefinition,
  mockBitbucketCloudBindingDefinition
} from '../../../../../helpers/mocks/alm-settings';
import { AlmKeys } from '../../../../../types/alm-settings';
import BitbucketTab, { DEFAULT_CLOUD_BINDING, DEFAULT_SERVER_BINDING } from '../BitbucketTab';

jest.mock('../../../../../api/alm-settings', () => ({
  createBitbucketConfiguration: jest.fn().mockResolvedValue(null),
  createBitbucketCloudConfiguration: jest.fn().mockResolvedValue(null),
  updateBitbucketConfiguration: jest.fn().mockResolvedValue(null),
  updateBitbucketCloudConfiguration: jest.fn().mockResolvedValue(null)
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle cancel', async () => {
  const wrapper = shallowRender();

  wrapper.setState({
    editedDefinition: mockBitbucketBindingDefinition()
  });

  wrapper.instance().handleCancel();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should handle edit', async () => {
  const config = mockBitbucketBindingDefinition();
  const wrapper = shallowRender({ definitions: [config] });
  wrapper.instance().handleEdit(config.key);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().editedDefinition).toEqual(config);
});

it('should create config for Bitbucket Server', async () => {
  const onUpdateDefinitions = jest.fn();
  const config = mockBitbucketBindingDefinition();
  const wrapper = shallowRender({ onUpdateDefinitions });

  wrapper.instance().handleCreate();
  wrapper.instance().handleSelectVariant(AlmKeys.BitbucketServer);
  expect(wrapper.state().editedDefinition).toBe(DEFAULT_SERVER_BINDING);

  wrapper.setState({ editedDefinition: config });
  await wrapper.instance().handleSubmit(config, '');

  expect(createBitbucketConfiguration).toBeCalledWith(config);
  expect(onUpdateDefinitions).toBeCalled();
  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should create config for Bitbucket Cloud', async () => {
  const onUpdateDefinitions = jest.fn();
  const config = mockBitbucketCloudBindingDefinition();
  const wrapper = shallowRender({ onUpdateDefinitions });

  wrapper.instance().handleCreate();
  wrapper.instance().handleSelectVariant(AlmKeys.BitbucketCloud);
  expect(wrapper.state().editedDefinition).toBe(DEFAULT_CLOUD_BINDING);

  wrapper.setState({ editedDefinition: config });
  await wrapper.instance().handleSubmit(config, '');

  expect(createBitbucketCloudConfiguration).toBeCalledWith(config);
  expect(onUpdateDefinitions).toBeCalled();
  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should update config for Bitbucket Server', async () => {
  const onUpdateDefinitions = jest.fn();
  const config = mockBitbucketBindingDefinition();
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

it('should update config for Bitbucket Cloud', async () => {
  const onUpdateDefinitions = jest.fn();
  const config = mockBitbucketCloudBindingDefinition();
  const wrapper = shallowRender({ onUpdateDefinitions });
  wrapper.setState({ editedDefinition: config });

  await wrapper.instance().handleSubmit(config, 'originalKey');

  expect(updateBitbucketCloudConfiguration).toBeCalledWith({
    newKey: 'key',
    ...config,
    key: 'originalKey'
  });
  expect(onUpdateDefinitions).toBeCalled();
  expect(wrapper.state().editedDefinition).toBeUndefined();
});

function shallowRender(props: Partial<BitbucketTab['props']> = {}) {
  return shallow<BitbucketTab>(
    <BitbucketTab
      branchesEnabled={true}
      definitions={[mockBitbucketBindingDefinition()]}
      definitionStatus={{}}
      loadingAlmDefinitions={false}
      loadingProjectCount={false}
      multipleAlmEnabled={true}
      onCheck={jest.fn()}
      onDelete={jest.fn()}
      onUpdateDefinitions={jest.fn()}
      {...props}
    />
  );
}
