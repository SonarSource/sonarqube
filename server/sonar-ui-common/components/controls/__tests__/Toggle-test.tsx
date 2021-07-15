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
import { Button } from '../buttons';
import Toggle from '../Toggle';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('on');
  expect(shallowRender({ value: false })).toMatchSnapshot('off');
  expect(shallowRender({ disabled: true })).toMatchSnapshot('disabled');
});

it('should call onChange when clicked', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ disabled: false, onChange, value: true });
  click(wrapper.find(Button));
  expect(onChange).toBeCalledWith(false);
});

function shallowRender(props?: Partial<Toggle['props']>) {
  return shallow(
    <Toggle disabled={true} name="toggle-name" onChange={jest.fn()} value={true} {...props} />
  );
}
