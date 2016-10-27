/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import InputForText from '../InputForText';
import { change } from '../../../../../helpers/testUtils';

it('should render textarea', () => {
  const onChange = jest.fn();
  const textarea = shallow(
      <InputForText
          name="foo"
          value="bar"
          isDefault={false}
          onChange={onChange}/>
  ).find('textarea');
  expect(textarea.length).toBe(1);
  expect(textarea.prop('name')).toBe('foo');
  expect(textarea.prop('value')).toBe('bar');
  expect(textarea.prop('onChange')).toBeTruthy();
});

it('should call onChange', () => {
  const onChange = jest.fn();
  const textarea = shallow(
      <InputForText
          name="foo"
          value="bar"
          isDefault={false}
          onChange={onChange}/>
  ).find('textarea');
  expect(textarea.length).toBe(1);
  expect(textarea.prop('onChange')).toBeTruthy();

  change(textarea, 'qux');

  expect(onChange).toBeCalledWith('qux');
});
