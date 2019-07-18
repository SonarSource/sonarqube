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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import Statistics, { StatisticCard } from '../Statistics';

const STATISTICS = { icon: 'stat-icon', text: 'my stat', value: 26666 };

it('should render', () => {
  expect(shallow(<Statistics statistics={[STATISTICS]} />)).toMatchSnapshot();
});

it('should render StatisticCard', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render big numbers correctly', () => {
  function checkCountUp(wrapper: ShallowWrapper, end: number, suffix: string) {
    expect(wrapper.find('CountUp').prop('end')).toBe(end);
    expect(wrapper.find('CountUp').prop('suffix')).toBe(suffix);
  }

  checkCountUp(
    shallowRender({ statistic: { ...STATISTICS, value: 999003632 } }),
    999,
    'short_number_suffix.m'
  );
  checkCountUp(
    shallowRender({ statistic: { ...STATISTICS, value: 999861538 } }),
    999,
    'short_number_suffix.m'
  );
  checkCountUp(shallowRender({ statistic: { ...STATISTICS, value: 1100021731 } }), 1.1, ' billion');
});

function shallowRender(props: Partial<StatisticCard['props']> = {}) {
  const wrapper = shallow(<StatisticCard statistic={STATISTICS} {...props} />);
  wrapper.setState({ viewable: true });
  return wrapper;
}
