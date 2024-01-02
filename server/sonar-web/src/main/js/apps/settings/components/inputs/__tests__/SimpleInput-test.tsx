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
import { KeyboardKeys } from '../../../../../helpers/keycodes';
import { mockSetting } from '../../../../../helpers/mocks/settings';
import { change, mockEvent } from '../../../../../helpers/testUtils';
import SimpleInput, { SimpleInputProps } from '../SimpleInput';

it('should render input', () => {
  const input = shallowRender().find('input');
  expect(input.length).toBe(1);
  expect(input.prop('type')).toBe('text');
  expect(input.prop('className')).toContain('input-large');
  expect(input.prop('name')).toBe('foo');
  expect(input.prop('value')).toBe('bar');
  expect(input.prop('onChange')).toBeDefined();
});

it('should call onChange', () => {
  const onChange = jest.fn();
  const input = shallowRender({ onChange }).find('input');
  expect(input.length).toBe(1);
  expect(input.prop('onChange')).toBeDefined();

  change(input, 'qux');
  expect(onChange).toHaveBeenCalledWith('qux');
});

it('should handle enter', () => {
  const onSave = jest.fn();
  shallowRender({ onSave })
    .instance()
    .handleKeyDown(mockEvent({ nativeEvent: { key: KeyboardKeys.Enter } }));
  expect(onSave).toHaveBeenCalled();
});

it('should handle esc', () => {
  const onCancel = jest.fn();
  shallowRender({ onCancel })
    .instance()
    .handleKeyDown(mockEvent({ nativeEvent: { key: KeyboardKeys.Escape } }));
  expect(onCancel).toHaveBeenCalled();
});

it('should ignore other keys', () => {
  const onSave = jest.fn();
  const onCancel = jest.fn();
  shallowRender({ onCancel, onSave })
    .instance()
    .handleKeyDown(mockEvent({ nativeEvent: { key: KeyboardKeys.LeftArrow } }));
  expect(onSave).not.toHaveBeenCalled();
  expect(onCancel).not.toHaveBeenCalled();
});

function shallowRender(overrides: Partial<SimpleInputProps> = {}) {
  return shallow<SimpleInput>(
    <SimpleInput
      className="input-large"
      isDefault={false}
      name="foo"
      onChange={jest.fn()}
      type="text"
      setting={mockSetting()}
      value="bar"
      {...overrides}
    />
  );
}
