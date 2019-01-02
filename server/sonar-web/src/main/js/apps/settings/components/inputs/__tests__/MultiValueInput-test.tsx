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
import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import MultiValueInput from '../MultiValueInput';
import PrimitiveInput from '../PrimitiveInput';
import { click } from '../../../../../helpers/testUtils';
import { DefaultInputProps } from '../../../utils';

const settingValue = {
  key: 'example'
};

const settingDefinition: T.SettingCategoryDefinition = {
  category: 'general',
  fields: [],
  key: 'example',
  multiValues: true,
  options: [],
  subCategory: 'Branches',
  type: 'STRING'
};

const assertValues = (inputs: ShallowWrapper<any>, values: string[]) => {
  values.forEach((value, index) => {
    const input = inputs.at(index);
    expect(input.prop('value')).toBe(value);
  });
};

it('should render one value', () => {
  const multiValueInput = shallowRender();
  const stringInputs = multiValueInput.find(PrimitiveInput);
  expect(stringInputs.length).toBe(1 + 1);
  assertValues(stringInputs, ['foo', '']);
});

it('should render several values', () => {
  const multiValueInput = shallowRender({ value: ['foo', 'bar', 'baz'] });
  const stringInputs = multiValueInput.find(PrimitiveInput);
  expect(stringInputs.length).toBe(3 + 1);
  assertValues(stringInputs, ['foo', 'bar', 'baz', '']);
});

it('should remove value', () => {
  const onChange = jest.fn();
  const multiValueInput = shallowRender({ onChange, value: ['foo', 'bar', 'baz'] });
  click(multiValueInput.find('.js-remove-value').at(1));
  expect(onChange).toBeCalledWith(['foo', 'baz']);
});

it('should change existing value', () => {
  const onChange = jest.fn();
  const multiValueInput = shallowRender({ onChange, value: ['foo', 'bar', 'baz'] });
  multiValueInput
    .find(PrimitiveInput)
    .at(1)
    .prop('onChange')('qux');
  expect(onChange).toBeCalledWith(['foo', 'qux', 'baz']);
});

it('should add new value', () => {
  const onChange = jest.fn();
  const multiValueInput = shallowRender({ onChange });
  multiValueInput
    .find(PrimitiveInput)
    .at(1)
    .prop('onChange')('bar');
  expect(onChange).toBeCalledWith(['foo', 'bar']);
});

function shallowRender(props: Partial<DefaultInputProps> = {}) {
  return shallow(
    <MultiValueInput
      onChange={jest.fn()}
      setting={{ ...settingValue, definition: settingDefinition }}
      value={['foo']}
      {...props}
    />
  );
}
