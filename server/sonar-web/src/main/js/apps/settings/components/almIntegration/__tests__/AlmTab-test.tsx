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
import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { mockAzureDefinition } from '../../../../../helpers/mocks/alm-settings';
import { AlmKeys, AzureBindingDefinition } from '../../../../../types/alm-settings';
import AlmTab from '../AlmTab';

const DEFAULT_BINDING = {
  key: '',
  personalAccessToken: ''
};

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle cancel', async () => {
  const wrapper = shallowRender();

  wrapper.setState({
    editedDefinition: mockAzureDefinition()
  });

  wrapper.instance().handleCancel();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should handle edit', async () => {
  const config = mockAzureDefinition();
  const wrapper = shallowRender({ definitions: [config] });
  wrapper.instance().handleEdit(config.key);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().editedDefinition).toEqual(config);
});

it('should create config', async () => {
  const onUpdateDefinitions = jest.fn();
  const createConfiguration = jest.fn(() => Promise.resolve());
  const config = mockAzureDefinition();
  const wrapper = shallowRender({ createConfiguration, onUpdateDefinitions });

  wrapper.instance().handleCreate();
  expect(wrapper.state().editedDefinition).toBe(DEFAULT_BINDING);

  wrapper.setState({ editedDefinition: config });
  await wrapper.instance().handleSubmit(config, '');

  expect(createConfiguration).toBeCalledWith(config);
  expect(onUpdateDefinitions).toBeCalled();
  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should update config', async () => {
  const onUpdateDefinitions = jest.fn();
  const updateConfiguration = jest.fn(() => Promise.resolve());
  const config = mockAzureDefinition();
  const wrapper = shallowRender({ onUpdateDefinitions, updateConfiguration });
  wrapper.setState({ editedDefinition: config });

  await wrapper.instance().handleSubmit(config, 'originalKey');

  expect(updateConfiguration).toBeCalledWith({
    newKey: 'key',
    ...config,
    key: 'originalKey'
  });
  expect(onUpdateDefinitions).toBeCalled();
  expect(wrapper.state().editedDefinition).toBeUndefined();
});

function shallowRender(props: Partial<AlmTab<AzureBindingDefinition>['props']> = {}) {
  return shallow<AlmTab<AzureBindingDefinition>>(
    <AlmTab
      alm={AlmKeys.Azure}
      createConfiguration={jest.fn()}
      defaultBinding={DEFAULT_BINDING}
      definitions={[mockAzureDefinition()]}
      form={jest.fn()}
      loading={false}
      multipleAlmEnabled={true}
      onDelete={jest.fn()}
      onUpdateDefinitions={jest.fn()}
      updateConfiguration={jest.fn()}
      {...props}
    />
  );
}
