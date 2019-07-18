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
import { Definition } from '../Definition';

const setting: T.Setting = {
  key: 'foo',
  value: '42',
  inherited: true,
  definition: {
    key: 'foo',
    name: 'Foo setting',
    description: 'When Foo then Bar',
    type: 'INTEGER',
    category: 'general',
    subCategory: 'SubFoo',
    defaultValue: '42',
    options: [],
    fields: []
  }
};

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should correctly handle change of value', () => {
  const changeValue = jest.fn();
  const checkValue = jest.fn();
  const wrapper = shallowRender({ changeValue, checkValue });
  wrapper.find('Input').prop<Function>('onChange')(5);
  expect(changeValue).toHaveBeenCalledWith(setting.definition.key, 5);
  expect(checkValue).toHaveBeenCalledWith(setting.definition.key);
});

it('should correctly cancel value change', () => {
  const cancelChange = jest.fn();
  const passValidation = jest.fn();
  const wrapper = shallowRender({ cancelChange, passValidation });
  wrapper.find('Input').prop<Function>('onCancel')();
  expect(cancelChange).toHaveBeenCalledWith(setting.definition.key);
  expect(passValidation).toHaveBeenCalledWith(setting.definition.key);
});

it('should correctly save value change', async () => {
  const saveValue = jest.fn().mockResolvedValue({});
  const wrapper = shallowRender({ changedValue: 10, saveValue });
  wrapper.find('DefinitionActions').prop<Function>('onSave')();
  await waitAndUpdate(wrapper);
  expect(saveValue).toHaveBeenCalledWith(setting.definition.key, undefined);
  expect(wrapper.find('AlertSuccessIcon').exists()).toBe(true);
});

function shallowRender(props: Partial<Definition['props']> = {}) {
  return shallow(
    <Definition
      cancelChange={jest.fn()}
      changeValue={jest.fn()}
      changedValue={null}
      checkValue={jest.fn()}
      loading={false}
      passValidation={jest.fn()}
      resetValue={jest.fn().mockResolvedValue({})}
      saveValue={jest.fn().mockResolvedValue({})}
      setting={setting}
      {...props}
    />
  );
}
