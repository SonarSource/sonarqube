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
import PerspectiveSelectOption from '../PerspectiveSelectOption';

it('should render correctly for a view', () => {
  expect(
    shallow(
      <PerspectiveSelectOption
        onFocus={jest.fn()}
        onSelect={jest.fn()}
        option={{ value: 'overall', type: 'view', label: 'Overall' }}>
        Overall
      </PerspectiveSelectOption>
    )
  ).toMatchSnapshot();
});

it('should render correctly for a visualization', () => {
  expect(
    shallow(
      <PerspectiveSelectOption
        onFocus={jest.fn()}
        onSelect={jest.fn()}
        option={{ value: 'coverage', type: 'visualization', label: 'Coverage' }}>
        Coverage
      </PerspectiveSelectOption>
    )
  ).toMatchSnapshot();
});

it('selects option', () => {
  const onSelect = jest.fn();
  const option = { value: 'coverage', type: 'visualization', label: 'Coverage' };
  const wrapper = shallow(
    <PerspectiveSelectOption onFocus={jest.fn()} onSelect={onSelect} option={option} />
  );
  const event = { stopPropagation() {}, preventDefault() {} };
  wrapper.simulate('mousedown', event);
  expect(onSelect).toBeCalledWith(option, event);
});

it('focuses option', () => {
  const onFocus = jest.fn();
  const option = { value: 'coverage', type: 'visualization', label: 'Coverage' };
  const wrapper = shallow(
    <PerspectiveSelectOption onFocus={onFocus} onSelect={jest.fn()} option={option} />
  );
  const event = { stopPropagation() {}, preventDefault() {} };

  wrapper.simulate('mouseenter', event);
  expect(onFocus).toBeCalledWith(option, event);

  onFocus.mockClear();
  wrapper.simulate('mousemove', event);
  expect(onFocus).toBeCalledWith(option, event);

  onFocus.mockClear();
  wrapper.setProps({ isFocused: true });
  wrapper.simulate('mousemove', event);
  expect(onFocus).not.toBeCalled();
});
