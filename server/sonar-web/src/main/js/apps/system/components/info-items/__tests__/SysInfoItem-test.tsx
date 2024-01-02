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
import HealthItem from '../HealthItem';
import SysInfoItem, { Props } from '../SysInfoItem';

it('should render correctly', () => {
  expect(shallowRender('/some/path/as/an/example')).toMatchSnapshot('string');
  expect(shallowRender({ foo: 'Far', bar: { a: 1, b: 'b' }, baz: true })).toMatchSnapshot('object');
  expect(shallowRender(true)).toMatchSnapshot('true');
  expect(shallowRender(false)).toMatchSnapshot('false');
});

it('should render health item', () => {
  expect(shallowRender('GREEN', 'Health').find(HealthItem).prop('health')).toBe('GREEN');
});

function shallowRender(value: Props['value'], name: Props['name'] = 'foo') {
  return shallow(<SysInfoItem name={name} value={value} />);
}
