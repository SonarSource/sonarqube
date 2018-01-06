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
import GraphsTooltips from '../GraphsTooltips';
import { DEFAULT_GRAPH } from '../../utils';
import { parseDate } from '../../../../helpers/dates';

const SERIES_ISSUES = [
  {
    name: 'bugs',
    translatedName: 'Bugs',
    data: [
      {
        x: '2011-10-01T22:01:00.000Z',
        y: 3
      },
      {
        x: '2011-10-25T10:27:41.000Z',
        y: 0
      }
    ]
  },
  {
    name: 'code_smells',
    translatedName: 'Code Smells',
    data: [
      {
        x: '2011-10-01T22:01:00.000Z',
        y: 18
      },
      {
        x: '2011-10-25T10:27:41.000Z',
        y: 15
      }
    ]
  },
  {
    name: 'vulnerabilities',
    translatedName: 'Vulnerabilities',
    data: [
      {
        x: '2011-10-01T22:01:00.000Z',
        y: 0
      },
      {
        x: '2011-10-25T10:27:41.000Z',
        y: 1
      }
    ]
  }
];

const DEFAULT_PROPS = {
  formatValue: val => 'Formated.' + val,
  graph: DEFAULT_GRAPH,
  graphWidth: 500,
  measuresHistory: [],
  selectedDate: parseDate('2011-10-01T22:01:00.000Z'),
  series: SERIES_ISSUES,
  tooltipIdx: 0,
  tooltipPos: 666
};

it('should render correctly for issues graphs', () => {
  expect(shallow(<GraphsTooltips {...DEFAULT_PROPS} />)).toMatchSnapshot();
});

it('should render correctly for random graphs', () => {
  expect(
    shallow(
      <GraphsTooltips
        {...DEFAULT_PROPS}
        graph="random"
        selectedDate={parseDate('2011-10-25T10:27:41.000Z')}
        tooltipIdx={1}
      />
    )
  ).toMatchSnapshot();
});

it('should not add separators if not needed', () => {
  expect(
    shallow(<GraphsTooltips {...DEFAULT_PROPS} graph="coverage" series={[]} />)
  ).toMatchSnapshot();
});
