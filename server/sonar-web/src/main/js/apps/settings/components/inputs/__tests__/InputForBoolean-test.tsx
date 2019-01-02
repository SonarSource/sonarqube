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
import { shallow } from 'enzyme';
import InputForBoolean from '../InputForBoolean';
import { DefaultSpecializedInputProps } from '../../../utils';

it('should render Toggle', () => {
  const onChange = jest.fn();
  const toggle = shallowRender({ onChange }).find('Toggle');
  expect(toggle.length).toBe(1);
  expect(toggle.prop('name')).toBe('foo');
  expect(toggle.prop('value')).toBe(true);
  expect(toggle.prop('onChange')).toBeTruthy();
});

it('should render Toggle without value', () => {
  const onChange = jest.fn();
  const input = shallowRender({ onChange, value: undefined });
  const toggle = input.find('Toggle');
  expect(toggle.length).toBe(1);
  expect(toggle.prop('name')).toBe('foo');
  expect(toggle.prop('value')).toBe(false);
  expect(toggle.prop('onChange')).toBeTruthy();
  expect(input.find('.note').length).toBe(1);
});

it('should call onChange', () => {
  const onChange = jest.fn();

  const input = shallowRender({ onChange, value: true });
  const toggle = input.find('Toggle');
  expect(toggle.length).toBe(1);
  expect(toggle.prop('onChange')).toBeTruthy();

  toggle.prop<Function>('onChange')(false);

  expect(onChange).toBeCalledWith(false);
});

function shallowRender(props: Partial<DefaultSpecializedInputProps> = {}) {
  return shallow(
    <InputForBoolean isDefault={false} name="foo" onChange={jest.fn()} value={true} {...props} />
  );
}
