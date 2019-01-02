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
import NewLinesFilter from '../NewLinesFilter';

it('renders', () => {
  const wrapper = shallow(<NewLinesFilter onQueryChange={jest.fn()} query={{}} />);
  expect(wrapper).toMatchSnapshot();

  const renderOption = wrapper.prop('renderOption');
  expect(renderOption(2, false)).toMatchSnapshot();

  const getFacetValueForOption = wrapper.prop('getFacetValueForOption');
  expect(
    getFacetValueForOption(
      {
        '*-1000.0': 1,
        '1000.0-10000.0': 2,
        '10000.0-100000.0': 3,
        '100000.0-500000.0': 4,
        '500000.0-*': 5
      },
      2
    )
  ).toBe(2);
});
