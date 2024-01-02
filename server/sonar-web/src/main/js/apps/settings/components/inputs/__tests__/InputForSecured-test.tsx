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
import { change, click } from '../../../../../helpers/testUtils';
import InputForSecured from '../InputForSecured';
import InputForString from '../InputForString';

it('should render lock icon, but no form', () => {
  const onChange = jest.fn();
  const input = shallowRender({ onChange });

  expect(input.find('LockIcon').length).toBe(1);
  expect(input.find('input').length).toBe(0);
});

it('should open form', () => {
  const onChange = jest.fn();
  const input = shallowRender({ onChange });
  const button = input.find('Button');
  expect(button.length).toBe(1);

  click(button);
  expect(input.find('input').length).toBe(1);
});

it('should set value', () => {
  const onChange = jest.fn(() => Promise.resolve());
  const input = shallowRender({ onChange });

  click(input.find('Button'));
  change(input.find(InputForString), 'secret');
  expect(onChange).toHaveBeenCalledWith('secret');
});

it('should show input when empty, and enable handle typing', () => {
  const input = shallowRender({ setting: mockSetting({ hasValue: false }) });
  const onChange = (value: string) => input.setProps({ hasValueChanged: true, value });
  input.setProps({ onChange });

  expect(input.find('input').length).toBe(1);
  change(input.find(InputForString), 'hello');
  expect(input.find('input').length).toBe(1);
  expect(input.find(InputForString).prop('value')).toBe('hello');
});

it('should handle value reset', () => {
  const input = shallowRender({ hasValueChanged: true, value: 'whatever' });
  input.setState({ changing: true });

  // reset
  input.setProps({ hasValueChanged: false, value: 'original' });

  expect(input.state('changing')).toBe(false);
});

it('should handle value reset to empty', () => {
  const input = shallowRender({ hasValueChanged: true, value: 'whatever' });
  input.setState({ changing: true });

  // outside change
  input.setProps({ hasValueChanged: false, setting: mockSetting({ hasValue: false }) });

  expect(input.state('changing')).toBe(true);
});

function shallowRender(props: Partial<InputForSecured['props']> = {}) {
  return shallow<InputForSecured>(
    <InputForSecured
      input={InputForString}
      hasValueChanged={false}
      onChange={jest.fn()}
      setting={mockSetting()}
      value="bar"
      {...props}
    />
  );
}
