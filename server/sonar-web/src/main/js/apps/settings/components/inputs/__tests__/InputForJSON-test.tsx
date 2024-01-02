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
import { mockSetting } from '../../../../../helpers/mocks/settings';
import { change } from '../../../../../helpers/testUtils';
import { DefaultSpecializedInputProps } from '../../../utils';
import InputForJSON from '../InputForJSON';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should call onChange', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange });

  change(wrapper.find('textarea'), '{"a": 1}');
  expect(onChange).toHaveBeenCalledWith('{"a": 1}');
});

it('should handle formatting for invalid JSON', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange, value: '{"a": 1b}' });
  wrapper.instance().format();
  expect(onChange).not.toHaveBeenCalled();

  expect(wrapper.state().formatError).toBe(true);
  expect(wrapper).toMatchSnapshot();
});

it('should handle formatting for valid JSON', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange, value: '{"a": 1}' });
  wrapper.instance().format();
  expect(onChange).toHaveBeenCalledWith(`{
    "a": 1
}`);

  expect(wrapper.state().formatError).toBe(false);
});

it('should handle ignore formatting if empty', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange, value: '' });
  wrapper.instance().format();
  expect(onChange).not.toHaveBeenCalled();

  expect(wrapper.state().formatError).toBe(false);
});

function shallowRender(props: Partial<DefaultSpecializedInputProps> = {}) {
  return shallow<InputForJSON>(
    <InputForJSON
      isDefault={false}
      name="foo"
      onChange={jest.fn()}
      setting={mockSetting()}
      value=""
      {...props}
    />
  );
}
