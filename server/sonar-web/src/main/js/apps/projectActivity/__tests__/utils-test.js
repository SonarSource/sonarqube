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
// @flow
import * as utils from '../utils';

const ANALYSES = [
  { key: 'AVyMjlK1HjR_PLDzRbB9', date: new Date('2017-06-09T13:06:10+0200'), events: [] },
  {
    key: 'AVyM9n3cHjR_PLDzRciT',
    date: new Date('2017-06-09T11:12:27+0200'),
    events: [{ key: 'AVyM9oI1HjR_PLDzRciU', category: 'VERSION', name: '1.1-SNAPSHOT' }]
  },
  { key: 'AVyMjlK1HjR_PLDzRbB9', date: new Date('2017-06-09T11:12:27+0200'), events: [] },
  {
    key: 'AVxZtCpH7841nF4RNEMI',
    date: new Date('2017-05-18T14:13:07+0200'),
    events: [
      {
        key: 'AVxZtC-N7841nF4RNEMJ',
        category: 'QUALITY_PROFILE',
        name: "Changes in 'Default - SonarSource conventions' (Java)"
      }
    ]
  },
  { key: 'AVwaa1qkpbBde8B6UhYI', date: new Date('2017-05-18T07:17:32+0200'), events: [] },
  {
    key: 'AVwQF7kwl-nNFgFWOJ3V',
    date: new Date('2017-05-16T07:09:59+0200'),
    events: [
      { key: 'AVyM9oI1HjR_PLDzRciU', category: 'VERSION', name: '1.0' },
      {
        key: 'AVwQF7zXl-nNFgFWOJ3W',
        category: 'QUALITY_PROFILE',
        name: "Changes in 'Default - SonarSource conventions' (Java)"
      }
    ]
  },
  { key: 'AVvtGF3IY6vCuQNDdwxI', date: new Date('2017-05-09T12:03:59+0200'), events: [] }
];

const HISTORY = [
  {
    metric: 'lines_to_cover',
    history: [
      { date: new Date('2017-04-27T08:21:32+0200'), value: '100' },
      { date: new Date('2017-04-30T23:06:24+0200'), value: '100' }
    ]
  },
  {
    metric: 'uncovered_lines',
    history: [
      { date: new Date('2017-04-27T08:21:32+0200'), value: '12' },
      { date: new Date('2017-04-30T23:06:24+0200'), value: '50' }
    ]
  }
];

const QUERY = {
  category: '',
  from: new Date('2017-04-27T08:21:32+0200'),
  graph: 'overview',
  project: 'foo',
  to: undefined,
  selectedDate: undefined,
  customMetrics: ['foo', 'bar', 'baz']
};

jest.mock('moment', () => date => ({
  startOf: () => {
    return {
      valueOf: () => `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
    };
  },
  toDate: () => new Date(date),
  format: format => `Formated.${format}:${date.valueOf()}`
}));

describe('generateCoveredLinesMetric', () => {
  it('should correctly generate covered lines metric', () => {
    expect(utils.generateCoveredLinesMetric(HISTORY[1], HISTORY, 'style')).toMatchSnapshot();
  });
});

describe('generateSeries', () => {
  it('should correctly generate the series', () => {
    expect(
      utils.generateSeries(HISTORY, 'coverage', 'INT', ['lines_to_cover', 'uncovered_lines'])
    ).toMatchSnapshot();
  });
});

describe('getAnalysesByVersionByDay', () => {
  it('should correctly map analysis by versions and by days', () => {
    expect(
      utils.getAnalysesByVersionByDay(ANALYSES, {
        category: '',
        customMetrics: [],
        graph: 'overview',
        project: 'foo'
      })
    ).toMatchSnapshot();
  });
  it('should also filter analysis based on the query', () => {
    expect(
      utils.getAnalysesByVersionByDay(ANALYSES, {
        category: 'QUALITY_PROFILE',
        customMetrics: [],
        graph: 'overview',
        project: 'foo'
      })
    ).toMatchSnapshot();
    expect(
      utils.getAnalysesByVersionByDay(ANALYSES, {
        category: '',
        customMetrics: [],
        graph: 'overview',
        project: 'foo',
        to: new Date('2017-06-09T11:12:27+0200'),
        from: new Date('2017-05-18T14:13:07+0200')
      })
    ).toMatchSnapshot();
  });
});

describe('getDisplayedHistoryMetrics', () => {
  const customMetrics = ['foo', 'bar'];
  it('should return only displayed metrics on the graph', () => {
    expect(utils.getDisplayedHistoryMetrics('overview', [])).toEqual([
      'bugs',
      'code_smells',
      'vulnerabilities'
    ]);
    expect(utils.getDisplayedHistoryMetrics('coverage', customMetrics)).toEqual([
      'uncovered_lines',
      'lines_to_cover'
    ]);
  });
  it('should return all custom metrics for the custom graph', () => {
    expect(utils.getDisplayedHistoryMetrics('custom', customMetrics)).toEqual(customMetrics);
  });
});

describe('getHistoryMetrics', () => {
  const customMetrics = ['foo', 'bar'];
  it('should return all metrics', () => {
    expect(utils.getHistoryMetrics('overview', [])).toEqual([
      'bugs',
      'code_smells',
      'vulnerabilities',
      'reliability_rating',
      'security_rating',
      'sqale_rating'
    ]);
    expect(utils.getHistoryMetrics('coverage', customMetrics)).toEqual([
      'uncovered_lines',
      'lines_to_cover',
      'coverage'
    ]);
    expect(utils.getHistoryMetrics('custom', customMetrics)).toEqual(customMetrics);
  });
});

describe('parseQuery', () => {
  it('should parse query with default values', () => {
    expect(
      utils.parseQuery({
        from: '2017-04-27T08:21:32+0200',
        id: 'foo',
        custom_metrics: 'foo,bar,baz'
      })
    ).toEqual(QUERY);
  });
});

describe('serializeQuery', () => {
  it('should serialize query for api request', () => {
    expect(utils.serializeQuery(QUERY)).toEqual({
      from: 'Formated.YYYY-MM-DDTHH:mm:ssZZ:1493274092000',
      project: 'foo'
    });
    expect(utils.serializeQuery({ ...QUERY, graph: 'coverage', category: 'test' })).toEqual({
      from: 'Formated.YYYY-MM-DDTHH:mm:ssZZ:1493274092000',
      project: 'foo',
      category: 'test'
    });
  });
});

describe('serializeUrlQuery', () => {
  it('should serialize query for url', () => {
    expect(utils.serializeUrlQuery(QUERY)).toEqual({
      from: 'Formated.YYYY-MM-DDTHH:mm:ssZZ:1493274092000',
      id: 'foo',
      custom_metrics: 'foo,bar,baz'
    });
    expect(
      utils.serializeUrlQuery({ ...QUERY, graph: 'coverage', category: 'test', customMetrics: [] })
    ).toEqual({
      from: 'Formated.YYYY-MM-DDTHH:mm:ssZZ:1493274092000',
      id: 'foo',
      graph: 'coverage',
      category: 'test'
    });
  });
});
