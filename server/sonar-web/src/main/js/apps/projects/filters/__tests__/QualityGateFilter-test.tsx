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
import { shallow } from 'enzyme';
import * as React from 'react';
import QualityGateFilter, { Props } from '../QualityGateFilter';

it('renders', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  const renderOption = wrapper.prop('renderOption');
  expect(renderOption(2, false)).toMatchSnapshot();

  const getFacetValueForOption = wrapper.prop('getFacetValueForOption');
  expect(getFacetValueForOption({ ERROR: 1, OK: 3 }, 'OK')).toBe(3);
});

it('should render with warning facet', () => {
  expect(
    shallowRender({ facet: { ERROR: 1, WARN: 2, OK: 3 } })
      .find('Filter')
      .prop('options')
  ).toEqual(['OK', 'WARN', 'ERROR']);
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(<QualityGateFilter onQueryChange={jest.fn()} query={{}} {...props} />);
}
