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
import InputForSingleSelectList from '../InputForSingleSelectList';

it('should render Select', () => {
  const onChange = jest.fn();
  const select = shallow(
    <InputForSingleSelectList
      name="foo"
      value="bar"
      options={['foo', 'bar', 'baz']}
      isDefault={false}
      onChange={onChange}
    />
  ).find('Select');
  expect(select.length).toBe(1);
  expect(select.prop('name')).toBe('foo');
  expect(select.prop('value')).toBe('bar');
  expect(select.prop('options')).toEqual([
    { value: 'foo', label: 'foo' },
    { value: 'bar', label: 'bar' },
    { value: 'baz', label: 'baz' }
  ]);
  expect(select.prop('onChange')).toBeTruthy();
});

it('should call onChange', () => {
  const onChange = jest.fn();
  const select = shallow(
    <InputForSingleSelectList
      name="foo"
      value="bar"
      options={['foo', 'bar', 'baz']}
      isDefault={false}
      onChange={onChange}
    />
  ).find('Select');
  expect(select.length).toBe(1);
  expect(select.prop('onChange')).toBeTruthy();

  select.prop('onChange')({ value: 'baz', label: 'baz' });
  expect(onChange).toBeCalledWith('baz');
});
