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
import React from 'react';
import { shallow } from 'enzyme';
import MeasureHeader from '../MeasureHeader';

const MEASURE = {
  value: '3.0',
  periods: [{ index: 1, value: '0.0' }],
  metric: {
    key: 'reliability_rating',
    type: 'RATING',
    name: 'Reliability Rating'
  },
  leak: '0.0'
};

const LEAK_MEASURE = {
  periods: [{ index: 1, value: '3.0' }],
  metric: {
    key: 'new_reliability_rating',
    type: 'RATING',
    name: 'Reliability Rating on New Code'
  },
  leak: '3.0'
};

const SECONDARY = {
  value: 'java=175123;js=26382',
  metric: {
    key: 'ncloc_language_distribution',
    type: 'DATA',
    name: 'Lines of Code Per Language'
  },
  leak: null
};

const PROPS = {
  component: { key: 'foo', qualifier: 'TRK' },
  components: [],
  handleSelect: () => {},
  leakPeriod: {
    date: '2017-05-16T13:50:02+0200',
    index: 1,
    mode: 'previous_version',
    parameter: '6,4'
  },
  measure: MEASURE,
  paging: null,
  secondaryMeasure: null,
  selectedIdx: null
};

it('should render correctly', () => {
  expect(shallow(<MeasureHeader {...PROPS} />)).toMatchSnapshot();
});

it('should render correctly for leak', () => {
  expect(shallow(<MeasureHeader {...PROPS} measure={LEAK_MEASURE} />)).toMatchSnapshot();
});

it('should render with branch', () => {
  expect(shallow(<MeasureHeader branch="feature" {...PROPS} />).find('Link')).toMatchSnapshot();
});

it('should display secondary measure too', () => {
  const wrapper = shallow(<MeasureHeader {...PROPS} secondaryMeasure={SECONDARY} />);
  expect(wrapper.find('Connect(LanguageDistribution)')).toHaveLength(1);
});

it('should display correctly for open file', () => {
  const wrapper = shallow(
    <MeasureHeader
      {...PROPS}
      component={{ key: 'bar', qualifier: 'FIL' }}
      components={[{ key: 'foo' }, { key: 'bar' }, { key: 'baz' }]}
      selectedIdx={1}
    />
  );
  expect(wrapper.find('.measure-details-primary-actions')).toMatchSnapshot();
  wrapper.setProps({ components: [{ key: 'foo' }, { key: 'bar' }] });
  expect(wrapper.find('.measure-details-primary-actions')).toMatchSnapshot();
});
