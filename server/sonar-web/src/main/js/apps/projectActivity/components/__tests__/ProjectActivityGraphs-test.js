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
import ProjectActivityGraphs from '../ProjectActivityGraphs';
import { DEFAULT_GRAPH } from '../../utils';
import { parseDate } from '../../../../helpers/dates';

const ANALYSES = [
  {
    key: 'A1',
    date: '2016-10-27T16:33:50+0200',
    events: [
      {
        key: 'E1',
        category: 'VERSION',
        name: '6.5-SNAPSHOT'
      }
    ]
  },
  {
    key: 'A2',
    date: '2016-10-27T12:21:15+0200',
    events: []
  },
  {
    key: 'A3',
    date: '2016-10-26T12:17:29+0200',
    events: [
      {
        key: 'E2',
        category: 'VERSION',
        name: '6.4'
      },
      {
        key: 'E3',
        category: 'OTHER',
        name: 'foo'
      }
    ]
  }
];

const METRICS = [{ key: 'code_smells', name: 'Code Smells', type: 'INT' }];

const DEFAULT_PROPS = {
  analyses: ANALYSES,
  leakPeriodDate: '2017-05-16T13:50:02+0200',
  loading: false,
  measuresHistory: [
    {
      metric: 'code_smells',
      history: [
        { date: parseDate('2016-10-26T12:17:29+0200'), value: '2286' },
        { date: parseDate('2016-10-27T12:21:15+0200'), value: '1749' },
        { date: parseDate('2016-10-27T16:33:50+0200'), value: '500' }
      ]
    }
  ],
  metrics: METRICS,
  query: { category: '', graph: DEFAULT_GRAPH, project: 'org.sonarsource.sonarqube:sonarqube' },
  updateQuery: () => {}
};

it('should render correctly the graph and legends', () => {
  expect(shallow(<ProjectActivityGraphs {...DEFAULT_PROPS} />)).toMatchSnapshot();
});

it('should render correctly with filter history on dates', () => {
  const wrapper = shallow(
    <ProjectActivityGraphs
      {...DEFAULT_PROPS}
      query={{ ...DEFAULT_PROPS.query, from: '2016-10-27T12:21:15+0200' }}
    />
  );
  expect(wrapper.state()).toMatchSnapshot();
});
