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
import SimpleInput from '../SimpleInput';
import { change } from '../../../../../helpers/testUtils';

it('should render input', () => {
  const onChange = jest.fn();
  const input = shallow(
    <SimpleInput
      type="text"
      className="input-large"
      name="foo"
      value="bar"
      isDefault={false}
      onChange={onChange}
    />
  ).find('input');
  expect(input.length).toBe(1);
  expect(input.prop('type')).toBe('text');
  expect(input.prop('className')).toContain('input-large');
  expect(input.prop('name')).toBe('foo');
  expect(input.prop('value')).toBe('bar');
  expect(input.prop('onChange')).toBeTruthy();
});

it('should call onChange', () => {
  const onChange = jest.fn();
  const input = shallow(
    <SimpleInput
      type="text"
      className="input-large"
      name="foo"
      value="bar"
      isDefault={false}
      onChange={onChange}
    />
  ).find('input');
  expect(input.length).toBe(1);
  expect(input.prop('onChange')).toBeTruthy();

  change(input, 'qux');

  expect(onChange).toBeCalledWith('qux');
});
