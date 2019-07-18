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
import Measure from '../Measure';

jest.mock('../../../helpers/measures', () => {
  const measures = require.requireActual('../../../helpers/measures');
  measures.getRatingTooltip = jest.fn(() => 'tooltip');
  return measures;
});

it('renders trivial measure', () => {
  expect(
    shallow(<Measure metricKey="coverage" metricType="PERCENT" value="73.0" />)
  ).toMatchSnapshot();
});

it('renders leak measure', () => {
  expect(
    shallow(<Measure metricKey="new_coverage" metricType="PERCENT" value="36.0" />)
  ).toMatchSnapshot();
});

it('renders LEVEL', () => {
  expect(
    shallow(<Measure metricKey="quality_gate_status" metricType="LEVEL" value="ERROR" />)
  ).toMatchSnapshot();
});

it('renders known RATING', () => {
  expect(
    shallow(<Measure metricKey="sqale_rating" metricType="RATING" value="3" />)
  ).toMatchSnapshot();
});

it('renders unknown RATING', () => {
  expect(
    shallow(<Measure metricKey="foo_rating" metricType="RATING" value="4" />)
  ).toMatchSnapshot();
});

it('renders undefined measure', () => {
  expect(
    shallow(<Measure metricKey="foo" metricType="PERCENT" value={undefined} />)
  ).toMatchSnapshot();
});
