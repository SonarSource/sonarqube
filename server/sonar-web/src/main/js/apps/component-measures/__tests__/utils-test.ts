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
import { ComponentQualifier } from '../../../types/component';
import { MeasurePageView } from '../../../types/measures';
import { MetricKey } from '../../../types/metrics';
import { ComponentMeasure } from '../../../types/types';
import * as utils from '../utils';

const MEASURES = [
  {
    metric: {
      id: '1',
      key: MetricKey.lines_to_cover,
      type: 'INT',
      name: 'Lines to Cover',
      domain: 'Coverage',
    },
    value: '431',
    period: { index: 1, value: '70' },
    leak: '70',
  },
  {
    metric: {
      id: '2',
      key: MetricKey.coverage,
      type: 'PERCENT',
      name: 'Coverage',
      domain: 'Coverage',
    },
    value: '99.3',
    period: { index: 1, value: '0.0999999999999943' },
    leak: '0.0999999999999943',
  },
  {
    metric: {
      id: '3',
      key: MetricKey.duplicated_lines_density,
      type: 'PERCENT',
      name: 'Duplicated Lines (%)',
      domain: 'Duplications',
    },
    value: '3.2',
    period: { index: 1, value: '0.0' },
    leak: '0.0',
  },
];

describe('filterMeasures', () => {
  it('should exclude banned measures', () => {
    expect(
      utils.filterMeasures([
        { metric: { id: '1', key: MetricKey.open_issues, name: 'Bugs', type: 'INT' } },
        {
          metric: {
            id: '2',
            key: MetricKey.critical_violations,
            name: 'Critical Violations',
            type: 'INT',
          },
        },
      ]),
    ).toHaveLength(1);
  });
});

describe('sortMeasures', () => {
  it('should sort based on the config', () => {
    expect(
      utils.sortMeasures('Reliability', [
        {
          metric: {
            id: '1',
            key: MetricKey.reliability_remediation_effort,
            name: 'new_bugs',
            type: 'INT',
          },
        },
        {
          metric: {
            id: '2',
            key: MetricKey.new_reliability_remediation_effort,
            name: 'bugs',
            type: 'INT',
          },
        },
        { metric: { id: '3', key: MetricKey.new_bugs, name: 'new_bugs', type: 'INT' } },
        { metric: { id: '4', key: MetricKey.bugs, name: 'bugs', type: 'INT' } },
        'overall_category',
      ]),
    ).toMatchSnapshot();
  });
});

describe('groupByDomains', () => {
  it('should correctly group by domains', () => {
    expect(utils.groupByDomains(MEASURES)).toMatchSnapshot();
  });

  it('should be memoized', () => {
    expect(utils.groupByDomains(MEASURES)).toBe(utils.groupByDomains(MEASURES));
  });
});

describe('parseQuery', () => {
  it('should correctly parse the url query', () => {
    expect(utils.parseQuery({})).toEqual({
      metric: utils.DEFAULT_METRIC,
      selected: '',
      view: utils.DEFAULT_VIEW,
    });
    expect(
      utils.parseQuery({ metric: 'foo', selected: 'bar', view: 'tree', asc: 'false' }),
    ).toEqual({
      metric: 'foo',
      selected: 'bar',
      view: 'tree',
      asc: false,
    });
  });

  it('should be memoized', () => {
    const query = { metric: 'foo', selected: 'bar', view: 'tree' };
    expect(utils.parseQuery(query)).toBe(utils.parseQuery(query));
  });
});

describe('serializeQuery', () => {
  it('should correctly serialize the query', () => {
    expect(utils.serializeQuery({ metric: '', selected: '', view: MeasurePageView.list })).toEqual({
      view: 'list',
    });
    expect(
      utils.serializeQuery({ metric: 'foo', selected: 'bar', view: MeasurePageView.tree }),
    ).toEqual({
      metric: 'foo',
      selected: 'bar',
    });
  });

  it('should be memoized', () => {
    const query: utils.Query = { metric: 'foo', selected: 'bar', view: MeasurePageView.tree };
    expect(utils.serializeQuery(query)).toBe(utils.serializeQuery(query));
  });
});

describe('extract measure', () => {
  const componentBuilder = (qual: ComponentQualifier): ComponentMeasure => {
    return {
      qualifier: qual,
      key: '1',
      name: 'TEST',
      measures: [
        {
          metric: 'alert_status',
          value: '3.2',
          period: { index: 1, value: '0.0' },
        },
        {
          metric: 'releasability_rating',
          value: '3.2',
          period: { index: 1, value: '0.0' },
        },
        {
          metric: 'releasability_effort',
          value: '3.2',
          period: { index: 1, value: '0.0' },
        },
      ],
    };
  };
  it('should ban quality gate for app', () => {
    const measure = utils.banQualityGateMeasure(componentBuilder(ComponentQualifier.Application));
    expect(measure).toHaveLength(0);
  });

  it('should ban quality gate for portfolio', () => {
    const measure = utils.banQualityGateMeasure(componentBuilder(ComponentQualifier.Portfolio));
    expect(measure).toHaveLength(3);
  });

  it('should ban quality gate for file', () => {
    const measure = utils.banQualityGateMeasure(componentBuilder(ComponentQualifier.File));
    expect(measure).toHaveLength(2);
    measure.forEach(({ metric }) => {
      expect(['releasability_rating', 'releasability_effort']).toContain(metric);
    });
  });
});
