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
import * as React from 'react';
import { shallow, mount } from 'enzyme';
import SysInfoItem from '../SysInfoItem';

it('should render string', () => {
  const wrapper = shallow(<SysInfoItem name="foo" value="/some/path/as/an/example" />);
  expect(wrapper.find('code').text()).toBe('/some/path/as/an/example');
});

it('should render object', () => {
  const wrapper = shallow(<SysInfoItem name="foo" value={{ bar: 'baz' }} />);
  expect(wrapper.find('ObjectItem').prop('value')).toEqual({ bar: 'baz' });
});

it('should render boolean', () => {
  const wrapper = shallow(<SysInfoItem name="foo" value={true} />);
  expect(wrapper.find('BooleanItem').prop('value')).toBe(true);
});

it('should render health item', () => {
  const wrapper = shallow(<SysInfoItem name="Health" value="GREEN" />);
  expect(wrapper.find('HealthItem').prop('health')).toBe('GREEN');
});

it('should render object correctly', () => {
  expect(
    mount(
      <SysInfoItem name="test" value={{ foo: 'Far', bar: { a: 1, b: 'b' }, baz: true }} />
    ).find('ObjectItem')
  ).toMatchSnapshot();
});

it('should render `true`', () => {
  const wrapper = mount(<SysInfoItem name="test" value={true} />);
  expect(wrapper.find('CheckIcon').exists()).toBeTruthy();
});

it('should render `false`', () => {
  const wrapper = mount(<SysInfoItem name="test" value={false} />);
  expect(wrapper.find('ClearIcon').exists()).toBeTruthy();
});
