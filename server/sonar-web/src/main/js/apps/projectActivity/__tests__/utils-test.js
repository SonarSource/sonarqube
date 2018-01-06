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
// @flow
import * as utils from '../utils';
import * as dates from '../../../helpers/dates';

const ANALYSES = [
  {
    key: 'AVyMjlK1HjR_PLDzRbB9',
    date: dates.parseDate('2017-06-09T13:06:10.000Z'),
    events: [{ key: 'AVyM9oI1HjR_PLDzRciU', category: 'VERSION', name: '1.1-SNAPSHOT' }]
  },
  { key: 'AVyM9n3cHjR_PLDzRciT', date: dates.parseDate('2017-06-09T11:12:27.000Z'), events: [] },
  {
    key: 'AVyMjlK1HjR_PLDzRbB9',
    date: dates.parseDate('2017-06-09T11:12:27.000Z'),
    events: [{ key: 'AVyM9oI1HjR_PLDzRciU', category: 'VERSION', name: '1.1' }]
  },
  {
    key: 'AVxZtCpH7841nF4RNEMI',
    date: dates.parseDate('2017-05-18T14:13:07.000Z'),
    events: [
      {
        key: 'AVxZtC-N7841nF4RNEMJ',
        category: 'QUALITY_PROFILE',
        name: 'Changes in "Default - SonarSource conventions" (Java)'
      }
    ]
  },
  { key: 'AVwaa1qkpbBde8B6UhYI', date: dates.parseDate('2017-05-18T07:17:32.000Z'), events: [] },
  {
    key: 'AVwQF7kwl-nNFgFWOJ3V',
    date: dates.parseDate('2017-05-16T07:09:59.000Z'),
    events: [
      { key: 'AVyM9oI1HjR_PLDzRciU', category: 'VERSION', name: '1.0' },
      {
        key: 'AVwQF7zXl-nNFgFWOJ3W',
        category: 'QUALITY_PROFILE',
        name: 'Changes in "Default - SonarSource conventions" (Java)'
      }
    ]
  },
  { key: 'AVvtGF3IY6vCuQNDdwxI', date: dates.parseDate('2017-05-09T12:03:59.000Z'), events: [] }
];

const HISTORY = [
  {
    metric: 'lines_to_cover',
    history: [
      { date: dates.parseDate('2017-04-27T08:21:32.000Z'), value: '100' },
      { date: dates.parseDate('2017-04-30T23:06:24.000Z'), value: '100' }
    ]
  },
  {
    metric: 'uncovered_lines',
    history: [
      { date: dates.parseDate('2017-04-27T08:21:32.000Z'), value: '12' },
      { date: dates.parseDate('2017-04-30T23:06:24.000Z'), value: '50' }
    ]
  }
];

const METRICS = [
  { key: 'uncovered_lines', name: 'Uncovered Lines', type: 'INT' },
  { key: 'lines_to_cover', name: 'Line to Cover', type: 'PERCENT' }
];

const QUERY = {
  category: '',
  from: dates.parseDate('2017-04-27T08:21:32.000Z'),
  graph: utils.DEFAULT_GRAPH,
  project: 'foo',
  to: undefined,
  selectedDate: undefined,
  customMetrics: ['foo', 'bar', 'baz']
};

describe('generateCoveredLinesMetric', () => {
  it('should correctly generate covered lines metric', () => {
    expect(utils.generateCoveredLinesMetric(HISTORY[1], HISTORY)).toMatchSnapshot();
  });
});

describe('generateSeries', () => {
  it('should correctly generate the series', () => {
    expect(
      utils.generateSeries(HISTORY, 'coverage', METRICS, ['uncovered_lines', 'lines_to_cover'])
    ).toMatchSnapshot();
  });
});

describe('getAnalysesByVersionByDay', () => {
  dates.startOfDay = jest.fn(date => {
    const startDay = new Date(date);
    startDay.setUTCHours(0, 0, 0, 0);
    return startDay;
  });
  it('should correctly map analysis by versions and by days', () => {
    expect(
      utils.getAnalysesByVersionByDay(ANALYSES, {
        category: '',
        customMetrics: [],
        graph: utils.DEFAULT_GRAPH,
        project: 'foo'
      })
    ).toMatchSnapshot();
  });
  it('should also filter analysis based on the query', () => {
    expect(
      utils.getAnalysesByVersionByDay(ANALYSES, {
        category: 'QUALITY_PROFILE',
        customMetrics: [],
        graph: utils.DEFAULT_GRAPH,
        project: 'foo'
      })
    ).toMatchSnapshot();
    expect(
      utils.getAnalysesByVersionByDay(ANALYSES, {
        category: '',
        customMetrics: [],
        graph: utils.DEFAULT_GRAPH,
        project: 'foo',
        to: dates.parseDate('2017-06-09T11:12:27.000Z'),
        from: dates.parseDate('2017-05-18T14:13:07.000Z')
      })
    ).toMatchSnapshot();
  });
  it('should create fake version', () => {
    expect(
      utils.getAnalysesByVersionByDay(
        [
          {
            key: 'AVyMjlK1HjR_PLDzRbB9',
            date: dates.parseDate('2017-06-09T13:06:10.000Z'),
            events: []
          },
          {
            key: 'AVyM9n3cHjR_PLDzRciT',
            date: dates.parseDate('2017-06-09T11:12:27.000Z'),
            events: []
          },
          {
            key: 'AVyMjlK1HjR_PLDzRbB9',
            date: dates.parseDate('2017-06-09T11:12:27.000Z'),
            events: []
          },
          {
            key: 'AVxZtCpH7841nF4RNEMI',
            date: dates.parseDate('2017-05-18T14:13:07.000Z'),
            events: []
          }
        ],
        {
          category: '',
          customMetrics: [],
          graph: utils.DEFAULT_GRAPH,
          project: 'foo'
        }
      )
    ).toMatchSnapshot();
  });
});

describe('getDisplayedHistoryMetrics', () => {
  const customMetrics = ['foo', 'bar'];
  it('should return only displayed metrics on the graph', () => {
    expect(utils.getDisplayedHistoryMetrics(utils.DEFAULT_GRAPH, [])).toEqual([
      'bugs',
      'code_smells',
      'vulnerabilities'
    ]);
    expect(utils.getDisplayedHistoryMetrics('coverage', customMetrics)).toEqual([
      'lines_to_cover',
      'uncovered_lines'
    ]);
  });
  it('should return all custom metrics for the custom graph', () => {
    expect(utils.getDisplayedHistoryMetrics('custom', customMetrics)).toEqual(customMetrics);
  });
});

describe('getHistoryMetrics', () => {
  const customMetrics = ['foo', 'bar'];
  it('should return all metrics', () => {
    expect(utils.getHistoryMetrics(utils.DEFAULT_GRAPH, [])).toEqual([
      'bugs',
      'code_smells',
      'vulnerabilities',
      'reliability_rating',
      'security_rating',
      'sqale_rating'
    ]);
    expect(utils.getHistoryMetrics('coverage', customMetrics)).toEqual([
      'lines_to_cover',
      'uncovered_lines',
      'coverage'
    ]);
    expect(utils.getHistoryMetrics('custom', customMetrics)).toEqual(customMetrics);
  });
});

describe('parseQuery', () => {
  it('should parse query with default values', () => {
    expect(
      utils.parseQuery({
        from: '2017-04-27T08:21:32.000Z',
        custom_metrics: 'foo,bar,baz',
        id: 'foo'
      })
    ).toEqual(QUERY);
  });
});

describe('serializeQuery', () => {
  it('should serialize query for api request', () => {
    expect(utils.serializeQuery(QUERY)).toEqual({
      from: '2017-04-27T08:21:32+0000',
      project: 'foo'
    });
    expect(utils.serializeQuery({ ...QUERY, graph: 'coverage', category: 'test' })).toEqual({
      from: '2017-04-27T08:21:32+0000',
      project: 'foo',
      category: 'test'
    });
  });
});

describe('serializeUrlQuery', () => {
  it('should serialize query for url', () => {
    expect(utils.serializeUrlQuery(QUERY)).toEqual({
      from: '2017-04-27T08:21:32+0000',
      id: 'foo',
      custom_metrics: 'foo,bar,baz'
    });
    expect(
      utils.serializeUrlQuery({ ...QUERY, graph: 'coverage', category: 'test', customMetrics: [] })
    ).toEqual({
      from: '2017-04-27T08:21:32+0000',
      id: 'foo',
      graph: 'coverage',
      category: 'test'
    });
  });
});

describe('hasHistoryData', () => {
  it('should correctly detect if there is history data', () => {
    expect(
      utils.hasHistoryData([
        {
          name: 'foo',
          type: 'INT',
          data: [
            { x: dates.parseDate('2017-04-27T08:21:32.000Z'), y: 2 },
            { x: dates.parseDate('2017-04-30T23:06:24.000Z'), y: 2 }
          ]
        }
      ])
    ).toBeTruthy();
    expect(
      utils.hasHistoryData([
        {
          name: 'foo',
          type: 'INT',
          data: []
        },
        {
          name: 'bar',
          type: 'INT',
          data: [
            { x: dates.parseDate('2017-04-27T08:21:32.000Z'), y: 2 },
            { x: dates.parseDate('2017-04-30T23:06:24.000Z'), y: 2 }
          ]
        }
      ])
    ).toBeTruthy();
    expect(
      utils.hasHistoryData([
        {
          name: 'bar',
          type: 'INT',
          data: [{ x: dates.parseDate('2017-04-27T08:21:32.000Z'), y: 2 }]
        }
      ])
    ).toBeFalsy();
  });
});
