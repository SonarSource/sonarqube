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
import CoverageRating, { CoverageRatingProps } from '../CoverageRating';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render with muted style', () => {
  expect(shallowRender({ muted: true }).find('DonutChart').prop('data')).toEqual([
    { fill: '#b4b4b4', value: 25 },
    { fill: 'transparent', value: 75 },
  ]);
});

it('should render with small size', () => {
  expect(shallowRender({ size: 'small' }).find('DonutChart').props()).toMatchObject({
    height: 20,
    padAngle: 0.1,
    thickness: 3,
    width: 20,
  });
});

it('should correctly handle padAngle for 0% and 100% coverage', () => {
  const wrapper = shallowRender({ value: 0 });
  expect(wrapper.find('DonutChart').prop('padAngle')).toBe(0);

  wrapper.setProps({ value: 25 });
  expect(wrapper.find('DonutChart').prop('padAngle')).toBe(0.1);

  wrapper.setProps({ value: 100 });
  expect(wrapper.find('DonutChart').prop('padAngle')).toBe(0);
});

function shallowRender(props: Partial<CoverageRatingProps> = {}) {
  return shallow(<CoverageRating value={25} {...props} />);
}
