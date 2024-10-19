/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { DEFAULT_GRAPH } from '../../../components/activity-graph/utils';
import * as dates from '../../../helpers/dates';
import { GraphType, ProjectAnalysisEventCategory } from '../../../types/project-activity';
import * as utils from '../utils';

jest.mock('date-fns', () => {
  const actual = jest.requireActual('date-fns');
  return {
    ...actual,
    startOfDay: jest.fn((date) => {
      const startDay = new Date(date);
      startDay.setUTCHours(0, 0, 0, 0);
      return startDay;
    }),
  };
});

const ANALYSES = [
  {
    key: 'AVyMjlK1HjR_PLDzRbB9',
    date: dates.parseDate('2017-06-09T13:06:10.000Z'),
    events: [
      {
        key: 'AVyM9oI1HjR_PLDzRciU',
        category: ProjectAnalysisEventCategory.Version,
        name: '1.1-SNAPSHOT',
      },
    ],
  },
  { key: 'AVyM9n3cHjR_PLDzRciT', date: dates.parseDate('2017-06-09T11:12:27.000Z'), events: [] },
  {
    key: 'AVyMjlK1HjR_PLDzRbB9',
    date: dates.parseDate('2017-06-09T11:12:27.000Z'),
    events: [
      { key: 'AVyM9oI1HjR_PLDzRciU', category: ProjectAnalysisEventCategory.Version, name: '1.1' },
    ],
  },
  {
    key: 'AVxZtCpH7841nF4RNEMI',
    date: dates.parseDate('2017-05-18T14:13:07.000Z'),
    events: [
      {
        key: 'AVxZtC-N7841nF4RNEMJ',
        category: ProjectAnalysisEventCategory.QualityProfile,
        name: 'Changes in "Default - SonarSource conventions" (Java)',
      },
    ],
  },
  { key: 'AVwaa1qkpbBde8B6UhYI', date: dates.parseDate('2017-05-18T07:17:32.000Z'), events: [] },
  {
    key: 'AVwQF7kwl-nNFgFWOJ3V',
    date: dates.parseDate('2017-05-16T07:09:59.000Z'),
    events: [
      { key: 'AVyM9oI1HjR_PLDzRciU', category: ProjectAnalysisEventCategory.Version, name: '1.0' },
      {
        key: 'AVwQF7zXl-nNFgFWOJ3W',
        category: ProjectAnalysisEventCategory.QualityProfile,
        name: 'Changes in "Default - SonarSource conventions" (Java)',
      },
    ],
  },
  { key: 'AVvtGF3IY6vCuQNDdwxI', date: dates.parseDate('2017-05-09T12:03:59.000Z'), events: [] },
];

const QUERY = {
  category: '',
  from: dates.parseDate('2017-04-27T08:21:32.000Z'),
  graph: DEFAULT_GRAPH,
  project: 'foo',
  to: undefined,
  selectedDate: undefined,
  customMetrics: ['foo', 'bar', 'baz'],
};

describe('getAnalysesByVersionByDay', () => {
  it('should correctly map analysis by versions and by days', () => {
    expect(
      utils.getAnalysesByVersionByDay(ANALYSES, {
        category: '',
      }),
    ).toMatchSnapshot();
  });
  it('should also filter analysis based on the query', () => {
    expect(
      utils.getAnalysesByVersionByDay(ANALYSES, {
        category: 'QUALITY_PROFILE',
      }),
    ).toMatchSnapshot();
    expect(
      utils.getAnalysesByVersionByDay(ANALYSES, {
        category: '',

        to: dates.parseDate('2017-06-09T11:12:27.000Z'),
        from: dates.parseDate('2017-05-18T14:13:07.000Z'),
      }),
    ).toMatchSnapshot();
  });
  it('should create fake version', () => {
    expect(
      utils.getAnalysesByVersionByDay(
        [
          {
            key: 'AVyMjlK1HjR_PLDzRbB9',
            date: dates.parseDate('2017-06-09T13:06:10.000Z'),
            events: [],
          },
          {
            key: 'AVyM9n3cHjR_PLDzRciT',
            date: dates.parseDate('2017-06-09T11:12:27.000Z'),
            events: [],
          },
          {
            key: 'AVyMjlK1HjR_PLDzRbB9',
            date: dates.parseDate('2017-06-09T11:12:27.000Z'),
            events: [],
          },
          {
            key: 'AVxZtCpH7841nF4RNEMI',
            date: dates.parseDate('2017-05-18T14:13:07.000Z'),
            events: [],
          },
        ],
        {
          category: '',
        },
      ),
    ).toMatchSnapshot();
  });
});

describe('parseQuery', () => {
  it('should parse query with default values', () => {
    expect(
      utils.parseQuery({
        from: '2017-04-27T08:21:32.000Z',
        custom_metrics: 'foo,bar,baz',
        id: 'foo',
      }),
    ).toEqual(QUERY);
  });
});

describe('serializeQuery', () => {
  it('should serialize query for api request', () => {
    expect(utils.serializeQuery(QUERY)).toEqual({
      project: 'foo',
    });
    expect(utils.serializeQuery({ ...QUERY, graph: GraphType.coverage, category: 'test' })).toEqual(
      {
        project: 'foo',
        category: 'test',
      },
    );
  });
});

describe('serializeUrlQuery', () => {
  it('should serialize query for url', () => {
    expect(utils.serializeUrlQuery(QUERY)).toEqual({
      from: '2017-04-27T08:21:32+0000',
      id: 'foo',
      custom_metrics: 'foo,bar,baz',
    });
    expect(
      utils.serializeUrlQuery({
        ...QUERY,
        graph: GraphType.coverage,
        category: 'test',
        customMetrics: [],
      }),
    ).toEqual({
      from: '2017-04-27T08:21:32+0000',
      id: 'foo',
      graph: GraphType.coverage,
      category: 'test',
    });
  });
});
