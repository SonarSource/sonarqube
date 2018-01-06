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
import InputForBoolean from '../InputForBoolean';
import Toggle from '../../../../../components/controls/Toggle';

it('should render Toggle', () => {
  const onChange = jest.fn();
  const toggle = shallow(
    <InputForBoolean name="foo" value={true} isDefault={false} onChange={onChange} />
  ).find(Toggle);
  expect(toggle.length).toBe(1);
  expect(toggle.prop('name')).toBe('foo');
  expect(toggle.prop('value')).toBe(true);
  expect(toggle.prop('onChange')).toBeTruthy();
});

it('should render Toggle without value', () => {
  const onChange = jest.fn();
  const input = shallow(<InputForBoolean name="foo" isDefault={false} onChange={onChange} />);
  const toggle = input.find(Toggle);
  expect(toggle.length).toBe(1);
  expect(toggle.prop('name')).toBe('foo');
  expect(toggle.prop('value')).toBe(false);
  expect(toggle.prop('onChange')).toBeTruthy();
  expect(input.find('.note').length).toBe(1);
});

it('should call onChange', () => {
  const onChange = jest.fn();
  const input = shallow(
    <InputForBoolean name="foo" value={true} isDefault={false} onChange={onChange} />
  );
  const toggle = input.find(Toggle);
  expect(toggle.length).toBe(1);
  expect(toggle.prop('onChange')).toBeTruthy();

  toggle.prop('onChange')(false);

  expect(onChange).toBeCalledWith(false);
});
