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
import ProjectActivityAnalysesList from '../ProjectActivityAnalysesList';
import { DEFAULT_GRAPH } from '../../utils';
import * as dates from '../../../../helpers/dates';

const ANALYSES = [
  {
    key: 'A1',
    date: dates.parseDate('2016-10-27T16:33:50+0000'),
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
    date: dates.parseDate('2016-10-27T12:21:15+0000'),
    events: []
  },
  {
    key: 'A3',
    date: dates.parseDate('2016-10-26T12:17:29+0000'),
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
  },
  {
    key: 'A4',
    date: dates.parseDate('2016-10-24T16:33:50+0000'),
    events: [
      {
        key: 'E1',
        category: 'QUALITY_GATE',
        name: 'Quality gate changed to red...'
      }
    ]
  }
];

const DEFAULT_PROPS = {
  addCustomEvent: () => {},
  addVersion: () => {},
  analyses: ANALYSES,
  analysesLoading: false,
  canAdmin: false,
  changeEvent: () => {},
  deleteAnalysis: () => {},
  deleteEvent: () => {},
  inizializing: false,
  project: { qualifier: 'TRK' },
  query: { category: '', graph: DEFAULT_GRAPH, project: 'org.sonarsource.sonarqube:sonarqube' },
  updateQuery: () => {}
};

window.Number = val => val;

dates.startOfDay = jest.fn(date => {
  const startDay = new Date(date);
  startDay.setUTCHours(0, 0, 0, 0);
  return startDay;
});

dates.toShortNotSoISOString = date => 'ISO.' + date;

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
