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
import MeasureHeader from '../MeasureHeader';

const METRIC = {
  id: '1',
  key: 'reliability_rating',
  type: 'RATING',
  name: 'Reliability Rating'
};

const LEAK_METRIC = {
  id: '2',
  key: 'new_reliability_rating',
  type: 'RATING',
  name: 'Reliability Rating on New Code'
};

const LEAK_MEASURE = '3.0';

const SECONDARY = {
  value: 'java=175123;js=26382',
  metric: 'ncloc_language_distribution'
};

const PROPS = {
  component: { key: 'foo', name: 'Foo', qualifier: 'TRK' },
  leakPeriod: {
    date: '2017-05-16T13:50:02+0200',
    index: 1,
    mode: 'previous_version',
    parameter: '6,4'
  } as T.Period,
  measureValue: '3.0',
  metric: METRIC
};

it('should render correctly', () => {
  expect(shallow(<MeasureHeader {...PROPS} />)).toMatchSnapshot();
});

it('should render correctly for leak', () => {
  expect(
    shallow(<MeasureHeader {...PROPS} measureValue={LEAK_MEASURE} metric={LEAK_METRIC} />)
  ).toMatchSnapshot();
});

it('should render with long living branch', () => {
  const longBranch = { isMain: false, name: 'branch-6.7', type: 'LONG' };
  expect(
    shallow(<MeasureHeader branchLike={longBranch} {...PROPS} />).find('Link')
  ).toMatchSnapshot();
});

it('should render with short living branch', () => {
  const shortBranch = { isMain: false, name: 'feature', mergeBranch: 'master', type: 'SHORT' };
  expect(
    shallow(
      <MeasureHeader
        {...PROPS}
        branchLike={shortBranch}
        measureValue={LEAK_MEASURE}
        metric={LEAK_METRIC}
      />
    )
  ).toMatchSnapshot();
});

it('should not render link to activity page for files', () => {
  expect(
    shallow(<MeasureHeader {...PROPS} />)
      .find('HistoryIcon')
      .exists()
  ).toBeTruthy();

  expect(
    shallow(<MeasureHeader {...PROPS} component={{ ...PROPS.component, qualifier: 'FIL' }} />)
      .find('HistoryIcon')
      .exists()
  ).toBeFalsy();
});

it('should display secondary measure too', () => {
  const wrapper = shallow(<MeasureHeader {...PROPS} secondaryMeasure={SECONDARY} />);
  expect(wrapper.find('Connect(LanguageDistribution)')).toHaveLength(1);
});

it('should work with measure without value', () => {
  expect(shallow(<MeasureHeader {...PROPS} measureValue={undefined} />)).toMatchSnapshot();
});
