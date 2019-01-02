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
import ProjectActivityAnalysesList from '../ProjectActivityAnalysesList';
import { DEFAULT_GRAPH } from '../../utils';
import * as dates from '../../../../helpers/dates';

jest.mock('../../../../helpers/dates', () => {
  const actual = require.requireActual('../../../../helpers/dates');
  return Object.assign({}, actual, {
    startOfDay: (date: Date) => {
      const startDay = new Date(date);
      startDay.setUTCHours(0, 0, 0, 0);
      return startDay;
    },
    toShortNotSoISOString: (date: string) => 'ISO.' + date
  });
});

const ANALYSES = [
  {
    key: 'A1',
    date: dates.parseDate('2016-10-27T16:33:50+0000'),
    events: [{ key: 'E1', category: 'VERSION', name: '6.5-SNAPSHOT' }]
  },
  { key: 'A2', date: dates.parseDate('2016-10-27T12:21:15+0000'), events: [] },
  {
    key: 'A3',
    date: dates.parseDate('2016-10-26T12:17:29+0000'),
    events: [
      { key: 'E2', category: 'VERSION', name: '6.4' },
      { key: 'E3', category: 'OTHER', name: 'foo' }
    ]
  },
  {
    key: 'A4',
    date: dates.parseDate('2016-10-24T16:33:50+0000'),
    events: [{ key: 'E1', category: 'QUALITY_GATE', name: 'Quality gate changed to red...' }]
  }
];

const DEFAULT_PROPS: ProjectActivityAnalysesList['props'] = {
  addCustomEvent: jest.fn().mockResolvedValue(undefined),
  addVersion: jest.fn().mockResolvedValue(undefined),
  analyses: ANALYSES,
  analysesLoading: false,
  canAdmin: false,
  changeEvent: jest.fn().mockResolvedValue(undefined),
  deleteAnalysis: jest.fn().mockResolvedValue(undefined),
  deleteEvent: jest.fn().mockResolvedValue(undefined),
  initializing: false,
  project: { qualifier: 'TRK' },
  query: {
    category: '',
    customMetrics: [],
    graph: DEFAULT_GRAPH,
    project: 'org.sonarsource.sonarqube:sonarqube'
  },
  updateQuery: () => {}
};

it('should render correctly', () => {
  expect(shallow(<ProjectActivityAnalysesList {...DEFAULT_PROPS} />)).toMatchSnapshot();
});

it('should correctly filter analyses by category', () => {
  const wrapper = shallow(<ProjectActivityAnalysesList {...DEFAULT_PROPS} />);
  wrapper.setProps({ query: { ...DEFAULT_PROPS.query, category: 'QUALITY_GATE' } });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly filter analyses by date range', () => {
  const wrapper = shallow(<ProjectActivityAnalysesList {...DEFAULT_PROPS} />);
  wrapper.setProps({
    query: {
      ...DEFAULT_PROPS.query,
      from: dates.parseDate('2016-10-27T16:33:50+0000'),
      to: dates.parseDate('2016-10-27T16:33:50+0000')
    }
  });
  expect(wrapper).toMatchSnapshot();
});
