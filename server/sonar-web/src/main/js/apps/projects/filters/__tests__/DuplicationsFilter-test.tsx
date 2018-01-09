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
import { shallow } from 'enzyme';
import DuplicationsFilter from '../DuplicationsFilter';

it('renders', () => {
  const wrapper = shallow(<DuplicationsFilter onQueryChange={jest.fn()} query={{}} />);
  expect(wrapper).toMatchSnapshot();

  const renderOption = wrapper.prop('renderOption');
  expect(renderOption(2, false)).toMatchSnapshot();
  expect(renderOption(6, true)).toMatchSnapshot();

  const getFacetValueForOption = wrapper.prop('getFacetValueForOption');
  expect(
    getFacetValueForOption(
      { '*-3.0': 1, '3.0-5.0': 42, '5.0-10.0': 14, '10.0-20.0': 13, '20.0-*': 8, NO_DATA: 3 },
      2
    )
  ).toBe(42);
});
