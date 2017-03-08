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
import React from 'react';
import { shallow } from 'enzyme';
import SortingFilter from '../SortingFilter';

it('should render with default parameters and empty query', () => {
  const wrapper = shallow(
    <SortingFilter
        property="foo"
        query={{}}/>
  );
  expect(wrapper).toMatchSnapshot();
  const sortingFilter = wrapper.instance();
  expect(sortingFilter.isSortActive('left')).toBeFalsy();
  expect(sortingFilter.isSortActive('right')).toBeFalsy();
});

it('should render with custom parameters', () => {
  const wrapper = shallow(
    <SortingFilter
        property="foo"
        query={{}}
        sortDesc="right"
        leftText="worst"
        rightText="best"/>
  );
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly with matching query', () => {
  const wrapper = shallow(
    <SortingFilter
      property="foo"
      query={{ sort: '-foo', languages: 'php,cpp' }}
      sortDesc="right"/>
  );
  expect(wrapper).toMatchSnapshot();
  const sortingFilter = wrapper.instance();
  expect(sortingFilter.isSortActive('left')).toBeFalsy();
  expect(sortingFilter.isSortActive('right')).toBeTruthy();
});

it('should render correctly with no matching query', () => {
  const wrapper = shallow(
    <SortingFilter
      property="foo"
      query={{ sort: 'bar' }}/>
  );
  expect(wrapper).toMatchSnapshot();
  const sortingFilter = wrapper.instance();
  expect(sortingFilter.isSortActive('left')).toBeFalsy();
  expect(sortingFilter.isSortActive('right')).toBeFalsy();
});
