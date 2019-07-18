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
import { shallow } from 'enzyme';
import * as React from 'react';
import { change, click, submit } from 'sonar-ui-common/helpers/testUtils';
import { DefaultSpecializedInputProps } from '../../../utils';
import InputForPassword from '../InputForPassword';

it('should render lock icon, but no form', () => {
  const onChange = jest.fn();
  const input = shallowRender({ onChange });

  expect(input.find('LockIcon').length).toBe(1);
  expect(input.find('form').length).toBe(0);
});

it('should open form', () => {
  const onChange = jest.fn();
  const input = shallowRender({ onChange });
  const button = input.find('Button');
  expect(button.length).toBe(1);

  click(button);
  expect(input.find('form').length).toBe(1);
});

it('should set value', () => {
  const onChange = jest.fn(() => Promise.resolve());
  const input = shallowRender({ onChange });

  click(input.find('Button'));
  change(input.find('.js-password-input'), 'secret');
  submit(input.find('form'));
  expect(onChange).toBeCalledWith('secret');
});

function shallowRender(props: Partial<DefaultSpecializedInputProps> = {}) {
  return shallow(
    <InputForPassword
      hasValueChanged={false}
      isDefault={false}
      name="foo"
      onChange={jest.fn()}
      value="bar"
      {...props}
    />
  );
}
