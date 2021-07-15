/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { click } from '../../../helpers/testUtils';
import Radio from '../Radio';

it('should render properly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('not checked');

  wrapper.setProps({ checked: true });
  expect(wrapper).toMatchSnapshot('checked');
});

it('should invoke callback on click', () => {
  const onCheck = jest.fn();
  const value = 'value';
  const wrapper = shallowRender({ onCheck, value });

  click(wrapper);
  expect(onCheck).toHaveBeenCalled();
});

it('should not invoke callback on click when disabled', () => {
  const onCheck = jest.fn();
  const wrapper = shallowRender({ disabled: true, onCheck });

  click(wrapper);
  expect(onCheck).not.toHaveBeenCalled();
});

function shallowRender(props?: Partial<Radio['props']>) {
  return shallow<Radio>(<Radio checked={false} onCheck={jest.fn()} value="value" {...props} />);
}
