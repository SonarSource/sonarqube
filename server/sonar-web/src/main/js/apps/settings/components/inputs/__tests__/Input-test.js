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
import React from 'react';
import { shallow } from 'enzyme';
import Input from '../Input';
import PrimitiveInput from '../PrimitiveInput';
import MultiValueInput from '../MultiValueInput';
import PropertySetInput from '../PropertySetInput';
import { TYPE_STRING, TYPE_PROPERTY_SET } from '../../../constants';

it('should render PrimitiveInput', () => {
  const setting = { definition: { key: 'example', type: TYPE_STRING } };
  const onChange = jest.fn();
  const input = shallow(<Input setting={setting} value="foo" onChange={onChange} />).find(
    PrimitiveInput
  );
  expect(input.length).toBe(1);
  expect(input.prop('setting')).toBe(setting);
  expect(input.prop('value')).toBe('foo');
  expect(input.prop('onChange')).toBe(onChange);
});

it('should render MultiValueInput', () => {
  const setting = { definition: { key: 'example', type: TYPE_STRING, multiValues: true } };
  const value = ['foo', 'bar'];
  const onChange = jest.fn();
  const input = shallow(<Input setting={setting} value={value} onChange={onChange} />).find(
    MultiValueInput
  );
  expect(input.length).toBe(1);
  expect(input.prop('setting')).toBe(setting);
  expect(input.prop('value')).toBe(value);
  expect(input.prop('onChange')).toBe(onChange);
});

it('should render PropertySetInput', () => {
  const setting = { definition: { key: 'example', type: TYPE_PROPERTY_SET, fields: [] } };
  const value = [{ foo: 'bar' }];
  const onChange = jest.fn();
  const input = shallow(<Input setting={setting} value={value} onChange={onChange} />).find(
    PropertySetInput
  );
  expect(input.length).toBe(1);
  expect(input.prop('setting')).toBe(setting);
  expect(input.prop('value')).toBe(value);
  expect(input.prop('onChange')).toBe(onChange);
});
