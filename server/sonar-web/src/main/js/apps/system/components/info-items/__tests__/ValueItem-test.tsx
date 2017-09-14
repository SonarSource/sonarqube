/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import ValueItem from '../ValueItem';

it('should render string', () => {
  const wrapper = shallow(<ValueItem name="foo" value="/some/path/as/an/example" />);
  expect(wrapper.find('code').text()).toBe('/some/path/as/an/example');
});

it('should render object', () => {
  const wrapper = shallow(<ValueItem name="foo" value={{ bar: 'baz' }} />);
  expect(wrapper.find('ObjectItem').prop('value')).toEqual({ bar: 'baz' });
});

it('should render boolean', () => {
  const wrapper = shallow(<ValueItem name="foo" value={true} />);
  expect(wrapper.find('BooleanItem').prop('value')).toBe(true);
});
