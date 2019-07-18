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
import MetricBox from '../MetricBox';

it('should render correctly', () => {
  const measures = {
    reliability_rating: '3',
    last_change_on_reliability_rating: '{"date":"2017-01-02T00:00:00.000Z","value":2}',
    reliability_rating_effort: '{"rating":3,"projects":1}'
  };
  expect(
    shallow(<MetricBox component="foo" measures={measures} metricKey="reliability" />)
  ).toMatchSnapshot();
});

it('should render correctly for releasability', () => {
  const measures = {
    releasability_rating: '2',
    last_change_on_releasability_rating: '{"date":"2017-01-02T00:00:00.000Z","value":2}',
    releasability_effort: '5'
  };
  expect(
    shallow(<MetricBox component="foo" measures={measures} metricKey="releasability" />)
  ).toMatchSnapshot();
});

it('should render correctly when no effort', () => {
  const measures = {
    releasability_rating: '2',
    last_change_on_releasability_rating: '{"date":"2017-01-02T00:00:00.000Z","value":2}',
    releasability_effort: '0'
  };

  expect(
    shallow(<MetricBox component="foo" measures={measures} metricKey="releasability" />)
  ).toMatchSnapshot();
});
