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
import { mockDefinition, mockSetting } from '../../../../../helpers/mocks/settings';
import { Setting, SettingType } from '../../../../../types/settings';
import { DefaultInputProps } from '../../../utils';
import Input from '../Input';
import InputForSecured from '../InputForSecured';
import MultiValueInput from '../MultiValueInput';
import PrimitiveInput from '../PrimitiveInput';
import PropertySetInput from '../PropertySetInput';

it('should render PrimitiveInput', () => {
  const onChange = jest.fn();
  const input = shallowRender({ onChange }).find(PrimitiveInput);
  expect(input.length).toBe(1);
  expect(input.prop('value')).toBe('foo');
  expect(input.prop('onChange')).toBe(onChange);
});

it('should render Secured input', () => {
  const setting: Setting = mockSetting({
    key: 'foo.secured',
    definition: mockDefinition({ key: 'foo.secured', type: SettingType.PROPERTY_SET }),
  });
  const onChange = jest.fn();
  const input = shallowRender({ onChange, setting }).find(InputForSecured);
  expect(input.length).toBe(1);
  expect(input.prop('value')).toBe('foo');
  expect(input.prop('onChange')).toBe(onChange);
});

it('should render MultiValueInput', () => {
  const setting = mockSetting({
    definition: mockDefinition({ multiValues: true }),
  });
  const onChange = jest.fn();
  const value = ['foo', 'bar'];
  const input = shallowRender({ onChange, setting, value }).find(MultiValueInput);
  expect(input.length).toBe(1);
  expect(input.prop('setting')).toBe(setting);
  expect(input.prop('value')).toBe(value);
  expect(input.prop('onChange')).toBe(onChange);
});

it('should render PropertySetInput', () => {
  const setting: Setting = mockSetting({
    definition: mockDefinition({ type: SettingType.PROPERTY_SET }),
  });

  const onChange = jest.fn();
  const value = [{ foo: 'bar' }];
  const input = shallowRender({ onChange, setting, value }).find(PropertySetInput);
  expect(input.length).toBe(1);
  expect(input.prop('setting')).toBe(setting);
  expect(input.prop('value')).toBe(value);
  expect(input.prop('onChange')).toBe(onChange);
});

function shallowRender(props: Partial<DefaultInputProps> = {}) {
  return shallow(<Input onChange={jest.fn()} setting={mockSetting()} value="foo" {...props} />);
}
