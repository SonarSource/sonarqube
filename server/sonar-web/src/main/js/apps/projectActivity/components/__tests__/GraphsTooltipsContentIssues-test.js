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
import GraphsTooltipsContentIssues from '../GraphsTooltipsContentIssues';

const MEASURES_ISSUES = [
  {
    metric: 'bugs',
    history: [
      {
        date: '2011-10-01T22:01:00.000Z',
        value: '500'
      },
      {
        date: '2011-10-25T10:27:41.000Z',
        value: '1.2k'
      }
    ]
  },
  {
    metric: 'reliability_rating',
    history: [
      {
        date: '2011-10-01T22:01:00.000Z'
      },
      {
        date: '2011-10-25T10:27:41.000Z',
        value: '5.0'
      }
    ]
  }
];

const DEFAULT_PROPS = {
  measuresHistory: MEASURES_ISSUES,
  name: 'bugs',
  style: '2',
  tooltipIdx: 1,
  translatedName: 'Bugs',
  value: '1.2k'
};

it('should render correctly', () => {
  expect(shallow(<GraphsTooltipsContentIssues {...DEFAULT_PROPS} />)).toMatchSnapshot();
});

it('should render correctly when rating data is missing', () => {
  expect(
    shallow(<GraphsTooltipsContentIssues {...DEFAULT_PROPS} tooltipIdx={0} value="500" />)
  ).toMatchSnapshot();
});
