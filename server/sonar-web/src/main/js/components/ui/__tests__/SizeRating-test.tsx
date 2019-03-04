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
import SizeRating, { Props } from '../SizeRating';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ muted: true, small: true, value: 1000 })).toMatchSnapshot();
  expect(shallowRender({ value: 10000 })).toMatchSnapshot();
  expect(shallowRender({ value: 100000 })).toMatchSnapshot();
  expect(shallowRender({ value: 500000 })).toMatchSnapshot();
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(<SizeRating value={100} {...props} />);
}
