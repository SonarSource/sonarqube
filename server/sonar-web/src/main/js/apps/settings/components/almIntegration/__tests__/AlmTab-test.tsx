/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { mockAzureBindingDefinition } from '../../../../../helpers/mocks/alm-settings';
import { waitAndUpdate } from '../../../../../helpers/testUtils';
import { AlmKeys } from '../../../../../types/alm-settings';
import AlmTab from '../AlmTab';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle cancel', async () => {
  const wrapper = shallowRender();

  wrapper.setState({
    editedDefinition: mockAzureBindingDefinition(),
  });

  wrapper.instance().handleCancel();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should handle edit', async () => {
  const config = mockAzureBindingDefinition();
  const wrapper = shallowRender({ definitions: [config] });

  wrapper.instance().handleEdit(config.key);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().editedDefinition).toEqual(config);
});

it('should handle create', async () => {
  const wrapper = shallowRender();

  wrapper.instance().handleCreate();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().editedDefinition).toBeUndefined();
});

it('should handle afterSubmit', async () => {
  const onUpdateDefinitions = jest.fn();
  const onCheck = jest.fn();
  const binding = mockAzureBindingDefinition();

  const wrapper = shallowRender({ onUpdateDefinitions, onCheck });

  wrapper.instance().handleAfterSubmit(binding);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().editedDefinition).toBeUndefined();
  expect(onUpdateDefinitions).toHaveBeenCalled();
  expect(onCheck).toHaveBeenCalledWith(binding.key);
});

function shallowRender(props: Partial<AlmTab['props']> = {}) {
  return shallow<AlmTab>(
    <AlmTab
      almTab={AlmKeys.Azure}
      branchesEnabled={true}
      definitions={[mockAzureBindingDefinition()]}
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
