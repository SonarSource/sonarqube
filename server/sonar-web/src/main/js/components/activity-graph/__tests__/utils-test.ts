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
import * as dates from '../../../helpers/dates';
import { MetricKey } from '../../../types/metrics';
import { GraphType, Serie } from '../../../types/project-activity';
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

const HISTORY = [
  {
    metric: MetricKey.lines_to_cover,
    history: [
      { date: dates.parseDate('2017-04-27T08:21:32.000Z'), value: '100' },
      { date: dates.parseDate('2017-04-30T23:06:24.000Z'), value: '100' },
    ],
  },
  {
    metric: MetricKey.uncovered_lines,
    history: [
      { date: dates.parseDate('2017-04-27T08:21:32.000Z'), value: '12' },
      { date: dates.parseDate('2017-04-30T23:06:24.000Z'), value: '50' },
    ],
  },
];

const METRICS = [
  { id: '1', key: MetricKey.uncovered_lines, name: 'Uncovered Lines', type: 'INT' },
  { id: '2', key: MetricKey.lines_to_cover, name: 'Line to Cover', type: 'PERCENT' },
];

const SERIE: Serie = {
  data: [
    { x: dates.parseDate('2017-04-27T08:21:32.000Z'), y: 2 },
    { x: dates.parseDate('2017-04-28T08:21:32.000Z'), y: 2 },
  ],
  name: 'foo',
  translatedName: 'Foo',
  type: 'PERCENT',
};

describe('generateCoveredLinesMetric', () => {
  it('should correctly generate covered lines metric', () => {
    expect(utils.generateCoveredLinesMetric(HISTORY[1], HISTORY)).toMatchSnapshot('with data');
    expect(utils.generateCoveredLinesMetric(HISTORY[1], [])).toMatchSnapshot('empty data');
  });
});

describe('generateSeries', () => {
  it('should correctly generate the series', () => {
    expect(
      utils.generateSeries(HISTORY, GraphType.coverage, METRICS, [
        MetricKey.uncovered_lines,
        MetricKey.lines_to_cover,
      ])
    ).toMatchSnapshot();
  });
  it('should correctly handle non-existent data', () => {
    expect(utils.generateSeries(HISTORY, GraphType.coverage, METRICS, [])).toEqual([]);
  });
});

describe('getDisplayedHistoryMetrics', () => {
  const customMetrics = ['foo', 'bar'];
  it('should return only displayed metrics on the graph', () => {
    expect(utils.getDisplayedHistoryMetrics(utils.DEFAULT_GRAPH, [])).toEqual([
      MetricKey.bugs,
      MetricKey.code_smells,
      MetricKey.vulnerabilities,
    ]);
    expect(utils.getDisplayedHistoryMetrics(GraphType.coverage, customMetrics)).toEqual([
      MetricKey.lines_to_cover,
      MetricKey.uncovered_lines,
    ]);
  });
  it('should return all custom metrics for the custom graph', () => {
    expect(utils.getDisplayedHistoryMetrics(GraphType.custom, customMetrics)).toEqual(
      customMetrics
    );
  });
});

describe('getHistoryMetrics', () => {
  const customMetrics = ['foo', 'bar'];
  it('should return all metrics', () => {
    expect(utils.getHistoryMetrics(utils.DEFAULT_GRAPH, [])).toEqual([
      MetricKey.bugs,
      MetricKey.code_smells,
      MetricKey.vulnerabilities,
      MetricKey.reliability_rating,
      MetricKey.security_rating,
      MetricKey.sqale_rating,
    ]);
    expect(utils.getHistoryMetrics(GraphType.coverage, customMetrics)).toEqual([
      MetricKey.lines_to_cover,
      MetricKey.uncovered_lines,
      GraphType.coverage,
    ]);
    expect(utils.getHistoryMetrics(GraphType.custom, customMetrics)).toEqual(customMetrics);
  });
});

describe('hasHistoryData', () => {
  it('should correctly detect if there is history data', () => {
    expect(
      utils.hasHistoryData([
        {
          name: 'foo',
          translatedName: 'foo',
          type: 'INT',
          data: [
            { x: dates.parseDate('2017-04-27T08:21:32.000Z'), y: 2 },
            { x: dates.parseDate('2017-04-30T23:06:24.000Z'), y: 2 },
          ],
        },
      ])
    ).toBe(true);
    expect(
      utils.hasHistoryData([
        {
          name: 'foo',
          translatedName: 'foo',
          type: 'INT',
          data: [],
        },
        {
          name: 'bar',
          translatedName: 'bar',
          type: 'INT',
          data: [
            { x: dates.parseDate('2017-04-27T08:21:32.000Z'), y: 2 },
            { x: dates.parseDate('2017-04-30T23:06:24.000Z'), y: 2 },
          ],
        },
      ])
    ).toBe(true);
    expect(
      utils.hasHistoryData([
        {
          name: 'bar',
          translatedName: 'bar',
          type: 'INT',
          data: [{ x: dates.parseDate('2017-04-27T08:21:32.000Z'), y: 2 }],
        },
      ])
    ).toBe(false);
  });
});

describe('getGraphTypes', () => {
  it('should correctly return the graph types', () => {
    expect(utils.getGraphTypes()).toMatchSnapshot();
    expect(utils.getGraphTypes(true)).toMatchSnapshot();
  });
});

describe('hasDataValues', () => {
  it('should check for data value', () => {
    expect(utils.hasDataValues(SERIE)).toBe(true);
    expect(utils.hasDataValues({ ...SERIE, data: [] })).toBe(false);
  });
});

describe('getSeriesMetricType', () => {
  it('should return the correct type', () => {
    expect(utils.getSeriesMetricType([SERIE])).toBe('PERCENT');
    expect(utils.getSeriesMetricType([])).toBe('INT');
  });
});

describe('hasHistoryDataValue', () => {
  it('should return the correct type', () => {
    expect(utils.hasHistoryDataValue([SERIE])).toBe(true);
    expect(utils.hasHistoryDataValue([])).toBe(false);
  });
});
