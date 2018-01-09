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
/* eslint-disable import/first */
jest.mock('../../../helpers/measures', () => {
  const measures = require.requireActual('../../../helpers/measures');
  measures.getRatingTooltip = jest.fn(() => 'tooltip');
  return measures;
});

import * as React from 'react';
import { shallow } from 'enzyme';
import Measure from '../Measure';

it('renders trivial measure', () => {
  const measure = { metric: { key: 'coverage', name: 'Coverage', type: 'PERCENT' }, value: '73.0' };
  expect(shallow(<Measure measure={measure} />)).toMatchSnapshot();
});

it('renders leak measure', () => {
  const measure = {
    metric: { key: 'new_coverage', name: 'Coverage on New Code', type: 'PERCENT' },
    leak: '36.0'
  };
  expect(shallow(<Measure measure={measure} />)).toMatchSnapshot();
});

it('renders LEVEL', () => {
  const measure = {
    metric: { key: 'quality_gate_status', name: 'Quality Gate', type: 'LEVEL' },
    value: 'ERROR'
  };
  expect(shallow(<Measure measure={measure} />)).toMatchSnapshot();
});

it('renders known RATING', () => {
  const measure = {
    metric: { key: 'sqale_rating', name: 'Maintainability Rating', type: 'RATING' },
    value: '3'
  };
  expect(shallow(<Measure measure={measure} />)).toMatchSnapshot();
});

it('renders unknown RATING', () => {
  const measure = {
    metric: { key: 'foo_rating', name: 'Foo Rating', type: 'RATING' },
    value: '4'
  };
  expect(shallow(<Measure measure={measure} />)).toMatchSnapshot();
});

it('renders undefined measure', () => {
  const measure = { metric: { key: 'foo', name: 'Foo', type: 'PERCENT' } };
  expect(shallow(<Measure measure={measure} />)).toMatchSnapshot();
});
