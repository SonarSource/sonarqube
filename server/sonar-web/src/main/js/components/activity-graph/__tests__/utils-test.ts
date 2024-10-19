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
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import * as dates from '../../../helpers/dates';
import { mockMeasureHistory, mockSerie } from '../../../helpers/mocks/project-activity';
import { get, save } from '../../../helpers/storage';
import { mockMetric } from '../../../helpers/testMocks';
import { GraphType } from '../../../types/project-activity';
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

jest.mock('../../../helpers/storage', () => ({
  save: jest.fn(),
  get: jest.fn(),
}));

const HISTORY = [
  mockMeasureHistory({
    metric: MetricKey.lines_to_cover,
    history: [
      { date: dates.parseDate('2017-04-27T08:21:32.000Z'), value: '100' },
      { date: dates.parseDate('2017-04-30T23:06:24.000Z'), value: '100' },
    ],
  }),
  mockMeasureHistory({
    metric: MetricKey.uncovered_lines,
    history: [
      { date: dates.parseDate('2017-04-27T08:21:32.000Z'), value: '12' },
      { date: dates.parseDate('2017-04-30T23:06:24.000Z'), value: '50' },
    ],
  }),
];

const METRICS = [
  mockMetric({ key: MetricKey.uncovered_lines, type: MetricType.Integer }),
  mockMetric({ key: MetricKey.lines_to_cover, type: MetricType.Percent }),
];

const SERIE = mockSerie({
  type: MetricType.Percent,
});

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
      ]),
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
      MetricKey.violations,
    ]);
    expect(utils.getDisplayedHistoryMetrics(GraphType.coverage, customMetrics)).toEqual([
      MetricKey.lines_to_cover,
      MetricKey.uncovered_lines,
    ]);
  });
  it('should return all custom metrics for the custom graph', () => {
    expect(utils.getDisplayedHistoryMetrics(GraphType.custom, customMetrics)).toEqual(
      customMetrics,
    );
  });
});

describe('getHistoryMetrics', () => {
  const customMetrics = ['foo', 'bar'];
  it('should return all metrics', () => {
    expect(utils.getHistoryMetrics(utils.DEFAULT_GRAPH, [])).toEqual([
      MetricKey.violations,
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
    expect(utils.hasHistoryData([mockSerie()])).toBe(true);
    expect(
      utils.hasHistoryData([
        mockSerie({
          data: [],
        }),
        mockSerie({
          name: 'bar',
          translatedName: 'bar',
        }),
      ]),
    ).toBe(true);
    expect(
      utils.hasHistoryData([
        mockSerie({
          name: 'bar',
          translatedName: 'bar',
          data: [{ x: dates.parseDate('2017-04-27T08:21:32.000Z'), y: 2 }],
        }),
      ]),
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
    expect(utils.getSeriesMetricType([SERIE])).toBe(MetricType.Percent);
    expect(utils.getSeriesMetricType([])).toBe(MetricType.Integer);
  });
});

describe('hasHistoryDataValue', () => {
  it('should return the correct type', () => {
    expect(utils.hasHistoryDataValue([SERIE])).toBe(true);
    expect(utils.hasHistoryDataValue([])).toBe(false);
  });
});

describe('saveActivityGraph', () => {
  it('should correctly store data for standard graph types', () => {
    utils.saveActivityGraph('foo', 'bar', GraphType.issues);
    expect(save).toHaveBeenCalledWith('foo', GraphType.issues, 'bar');
  });

  it.each([[[]], [[MetricKey.bugs, MetricKey.alert_status]]])(
    'should correctly store data for custom graph types',
    (metrics) => {
      utils.saveActivityGraph('foo', 'bar', GraphType.custom, metrics);
      expect(save).toHaveBeenCalledWith('foo', GraphType.custom, 'bar');
      // eslint-disable-next-line jest/no-conditional-in-test
      expect(save).toHaveBeenCalledWith('foo.custom', metrics.join(','), 'bar');
    },
  );
});

describe('getActivityGraph', () => {
  it('should correctly retrieve data for standard graph types', () => {
    jest.mocked(get).mockImplementation((key) => {
      // eslint-disable-next-line jest/no-conditional-in-test
      if (key.includes('.custom')) {
        return null;
      }
      return GraphType.coverage;
    });

    expect(utils.getActivityGraph('foo', 'bar')).toEqual({
      graph: GraphType.coverage,
      customGraphs: [],
    });
  });

  it.each([null, 'bugs,code_smells'])(
    'should correctly retrieve data for custom graph types',
    (data) => {
      jest.mocked(get).mockImplementation((key) => {
        // eslint-disable-next-line jest/no-conditional-in-test
        if (key.includes('.custom')) {
          return data;
        }
        return GraphType.custom;
      });

      expect(utils.getActivityGraph('foo', 'bar')).toEqual({
        graph: GraphType.custom,
        // eslint-disable-next-line jest/no-conditional-in-test
        customGraphs: data ? [MetricKey.bugs, MetricKey.code_smells] : [],
      });
    },
  );

  it('should correctly retrieve data for unknown graphs', () => {
    jest.mocked(get).mockReturnValue(null);

    expect(utils.getActivityGraph('foo', 'bar')).toEqual({
      graph: GraphType.issues,
      customGraphs: [],
    });
  });
});
