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
import BaselineSettingDays, { Props } from '../BaselineSettingDays';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ isChanged: true })).toMatchSnapshot();
  expect(shallowRender({ isChanged: true, isValid: false })).toMatchSnapshot();
});

it('should not display input when not selected', () => {
  const wrapper = shallowRender({ selected: false });
  expect(wrapper.find('ValidationInput')).toHaveLength(0);
});

it('should callback when clicked', () => {
  const onSelect = jest.fn();
  const wrapper = shallowRender({ onSelect, selected: false });

  wrapper.find('RadioCard').first().simulate('click');
  expect(onSelect).toHaveBeenCalledWith('NUMBER_OF_DAYS');
});

it('should callback when changing days', () => {
  const onChangeDays = jest.fn();
  const wrapper = shallowRender({ onChangeDays });

  wrapper
    .find('input')
    .first()
    .simulate('change', { currentTarget: { value: '23' } });
  expect(onChangeDays).toHaveBeenCalledWith('23');
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <BaselineSettingDays
      days="28"
      isChanged={false}
      isValid={true}
      onChangeDays={jest.fn()}
      onSelect={jest.fn()}
      selected={true}
      {...props}
    />
  );
}
