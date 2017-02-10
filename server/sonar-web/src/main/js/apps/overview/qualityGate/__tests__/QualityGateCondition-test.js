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
import QualityGateCondition from '../QualityGateCondition';

const mockRatingCondition = metric => ({
  actual: '3',
  error: '1',
  level: 'ERROR',
  measure: {
    metric: {
      key: metric,
      type: 'RATING',
      name: metric
    },
    value: '3'
  },
  op: 'GT',
  metric
});

it('open_issues', () => {
  const condition = {
    actual: '10',
    error: '0',
    level: 'ERROR',
    measure: {
      metric: {
        key: 'open_issues',
        type: 'INT',
        name: 'Open open_issues'
      },
      value: '10'
    },
    metric: 'open_issues',
    op: 'GT'
  };
  expect(shallow(
      <QualityGateCondition component={{ key: 'abcd-key' }} periods={[]} condition={condition}/>
  )).toMatchSnapshot();
});

it('new_open_issues', () => {
  const condition = {
    actual: '10',
    error: '0',
    level: 'ERROR',
    measure: {
      metric: {
        key: 'new_open_issues',
        type: 'INT',
        name: 'new_open_issues'
      },
      value: '10'
    },
    metric: 'new_open_issues',
    op: 'GT'
  };
  expect(shallow(
      <QualityGateCondition component={{ key: 'abcd-key' }} periods={[]} condition={condition}/>
  )).toMatchSnapshot();
});

it('reliability_rating', () => {
  const condition = mockRatingCondition('reliability_rating');
  expect(shallow(
      <QualityGateCondition component={{ key: 'abcd-key' }} periods={[]} condition={condition}/>
  )).toMatchSnapshot();
});

it('security_rating', () => {
  const condition = mockRatingCondition('security_rating');
  expect(shallow(
      <QualityGateCondition component={{ key: 'abcd-key' }} periods={[]} condition={condition}/>
  )).toMatchSnapshot();
});

it('sqale_rating', () => {
  const condition = mockRatingCondition('sqale_rating');
  expect(shallow(
      <QualityGateCondition component={{ key: 'abcd-key' }} periods={[]} condition={condition}/>
  )).toMatchSnapshot();
});

it('new_reliability_rating', () => {
  const condition = mockRatingCondition('new_reliability_rating');
  expect(shallow(
      <QualityGateCondition component={{ key: 'abcd-key' }} periods={[]} condition={condition}/>
  )).toMatchSnapshot();
});

it('new_security_rating', () => {
  const condition = mockRatingCondition('new_security_rating');
  expect(shallow(
      <QualityGateCondition component={{ key: 'abcd-key' }} periods={[]} condition={condition}/>
  )).toMatchSnapshot();
});

it('new_sqale_rating', () => {
  const condition = mockRatingCondition('new_sqale_rating');
  expect(shallow(
      <QualityGateCondition component={{ key: 'abcd-key' }} periods={[]} condition={condition}/>
  )).toMatchSnapshot();
});
