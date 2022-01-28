/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getApplicationDetails, getApplicationLeak } from '../../../../api/application';
import { getMeasuresWithPeriodAndMetrics } from '../../../../api/measures';
import { getProjectActivity } from '../../../../api/projectActivity';
import {
  getApplicationQualityGate,
  getQualityGateProjectStatus
} from '../../../../api/quality-gates';
import { getAllTimeMachineData } from '../../../../api/time-machine';
import { getActivityGraph, saveActivityGraph } from '../../../../components/activity-graph/utils';
import { isDiffMetric } from '../../../../helpers/measures';
import { mockBranch, mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockAnalysis } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { GraphType } from '../../../../types/project-activity';
import { Measure, Metric } from '../../../../types/types';
import BranchOverview, { BRANCH_OVERVIEW_ACTIVITY_GRAPH, NO_CI_DETECTED } from '../BranchOverview';
import BranchOverviewRenderer from '../BranchOverviewRenderer';

jest.mock('../../../../helpers/dates', () => ({
  parseDate: jest.fn(date => `PARSED:${date}`),
  toNotSoISOString: jest.fn(date => date)
}));

jest.mock('../../../../api/measures', () => {
  const { mockMeasure, mockMetric } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getMeasuresWithPeriodAndMetrics: jest.fn((_, metricKeys: string[]) => {
      const metrics: Metric[] = [];
      const measures: Measure[] = [];
      metricKeys.forEach(key => {
        if (key === 'unknown_metric') {
          return;
        }

        let type;
        if (/(coverage|duplication)$/.test(key)) {
          type = 'PERCENT';
        } else if (/_rating$/.test(key)) {
          type = 'RATING';
        } else {
          type = 'INT';
        }
        metrics.push(mockMetric({ key, id: key, name: key, type }));
        measures.push(
          mockMeasure({
            metric: key,
            ...(isDiffMetric(key) ? { leak: '1' } : { period: undefined })
          })
        );
      });
      return Promise.resolve({
        component: {
          measures,
          name: 'foo'
        },
        metrics
      });
    })
  };
});

jest.mock('../../../../api/quality-gates', () => {
  const { mockQualityGateProjectStatus, mockQualityGateApplicationStatus } = jest.requireActual(
    '../../../../helpers/mocks/quality-gates'
  );
  const { MetricKey } = jest.requireActual('../../../../types/metrics');
  return {
    getQualityGateProjectStatus: jest.fn().mockResolvedValue(
      mockQualityGateProjectStatus({
        status: 'ERROR',
        conditions: [
          {
            actualValue: '2',
            comparator: 'GT',
            errorThreshold: '1.0',
            metricKey: MetricKey.new_bugs,
            periodIndex: 1,
            status: 'ERROR'
          },
          {
            actualValue: '5',
            comparator: 'GT',
            errorThreshold: '2.0',
            metricKey: MetricKey.bugs,
            periodIndex: 0,
            status: 'ERROR'
          },
          {
            actualValue: '2',
            comparator: 'GT',
            errorThreshold: '1.0',
            metricKey: 'unknown_metric',
            periodIndex: 0,
            status: 'ERROR'
          }
        ]
      })
    ),
    getApplicationQualityGate: jest.fn().mockResolvedValue(mockQualityGateApplicationStatus())
  };
});

jest.mock('../../../../api/time-machine', () => {
  const { MetricKey } = jest.requireActual('../../../../types/metrics');
  return {
    getAllTimeMachineData: jest.fn().mockResolvedValue({
      measures: [
        { metric: MetricKey.bugs, history: [{ date: '2019-01-05', value: '2.0' }] },
        { metric: MetricKey.vulnerabilities, history: [{ date: '2019-01-05', value: '0' }] },
        { metric: MetricKey.sqale_index, history: [{ date: '2019-01-01', value: '1.0' }] },
        {
          metric: MetricKey.duplicated_lines_density,
          history: [{ date: '2019-01-02', value: '1.0' }]
        },
        { metric: MetricKey.ncloc, history: [{ date: '2019-01-03', value: '10000' }] },
        { metric: MetricKey.coverage, history: [{ date: '2019-01-04', value: '95.5' }] }
      ]
    })
  };
});

jest.mock('../../../../api/projectActivity', () => {
  const { mockAnalysis } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getProjectActivity: jest.fn().mockResolvedValue({
      analyses: [
        mockAnalysis({ detectedCI: 'Cirrus CI' }),
        mockAnalysis(),
        mockAnalysis(),
        mockAnalysis(),
        mockAnalysis()
      ]
    })
  };
});

jest.mock('../../../../api/application', () => ({
  getApplicationDetails: jest.fn().mockResolvedValue({
    branches: [],
    key: 'key-1',
    name: 'app',
    projects: [
      {
        branch: 'foo',
        key: 'KEY-P1',
        name: 'P1'
      }
    ],
    visibility: 'Private'
  }),
  getApplicationLeak: jest.fn().mockResolvedValue([
    {
      date: '2017-01-05',
      project: 'foo',
      projectName: 'Foo'
    }
  ])
}));

jest.mock('../../../../components/activity-graph/utils', () => {
  const { MetricKey } = jest.requireActual('../../../../types/metrics');
  const { GraphType } = jest.requireActual('../../../../types/project-activity');
  return {
    getActivityGraph: jest.fn(() => ({ graph: GraphType.coverage })),
    saveActivityGraph: jest.fn(),
    getHistoryMetrics: jest.fn(() => [MetricKey.lines_to_cover, MetricKey.uncovered_lines])
  };
});

beforeEach(jest.clearAllMocks);

describe('project overview', () => {
  it('should render correctly', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    expect(wrapper).toMatchSnapshot();
  });

  it("should correctly load a project's status", async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    expect(getQualityGateProjectStatus).toBeCalled();
    expect(getMeasuresWithPeriodAndMetrics).toBeCalled();

    // Check the conditions got correctly enhanced with measure meta data.
    const { qgStatuses } = wrapper.state();
    expect(qgStatuses).toHaveLength(1);
    const [qgStatus] = qgStatuses!;

    expect(qgStatus).toEqual(
      expect.objectContaining({
        name: 'Foo',
        key: 'foo',
        status: 'ERROR'
      })
    );

    const { failedConditions } = qgStatus;
    expect(failedConditions).toHaveLength(2);
    expect(failedConditions[0]).toMatchObject({
      actual: '2',
      level: 'ERROR',
      metric: MetricKey.new_bugs,
      measure: expect.objectContaining({
        metric: expect.objectContaining({ key: MetricKey.new_bugs })
      })
    });
    expect(failedConditions[1]).toMatchObject({
      actual: '5',
      level: 'ERROR',
      metric: MetricKey.bugs,
      measure: expect.objectContaining({
        metric: expect.objectContaining({ key: MetricKey.bugs })
      })
    });
  });

  it('should correctly flag a project as empty', async () => {
    (getMeasuresWithPeriodAndMetrics as jest.Mock).mockResolvedValueOnce({ component: {} });

    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    expect(wrapper.find(BranchOverviewRenderer).props().projectIsEmpty).toBe(true);
  });
});

describe('application overview', () => {
  const component = mockComponent({
    breadcrumbs: [mockComponent({ key: 'foo', qualifier: ComponentQualifier.Application })],
    qualifier: ComponentQualifier.Application
  });

  it('should render correctly', async () => {
    const wrapper = shallowRender({ component });
    await waitAndUpdate(wrapper);
    expect(wrapper).toMatchSnapshot();
  });

  it('should fetch correctly other branch', async () => {
    const wrapper = shallowRender({ branch: mockBranch(), component });
    await waitAndUpdate(wrapper);
    expect(getApplicationDetails).toHaveBeenCalled();
    expect(wrapper).toMatchSnapshot();
  });

  it("should correctly load an application's status", async () => {
    const wrapper = shallowRender({ component });
    await waitAndUpdate(wrapper);
    expect(getApplicationQualityGate).toBeCalled();
    expect(getApplicationLeak).toBeCalled();
    expect(getMeasuresWithPeriodAndMetrics).toBeCalled();

    // Check the conditions got correctly enhanced with measure meta data.
    const { qgStatuses } = wrapper.state();
    expect(qgStatuses).toHaveLength(2);
    const [qgStatus1, qgStatus2] = qgStatuses!;

    expect(qgStatus1).toEqual(
      expect.objectContaining({
        name: 'Foo',
        key: 'foo',
        status: 'ERROR'
      })
    );

    const { failedConditions: failedConditions1 } = qgStatus1;
    expect(failedConditions1).toHaveLength(2);
    expect(failedConditions1[0]).toMatchObject({
      actual: '10',
      level: 'ERROR',
      metric: MetricKey.coverage,
      measure: expect.objectContaining({
        metric: expect.objectContaining({ key: MetricKey.coverage })
      })
    });
    expect(failedConditions1[1]).toMatchObject({
      actual: '5',
      level: 'ERROR',
      metric: MetricKey.new_bugs,
      measure: expect.objectContaining({
        metric: expect.objectContaining({ key: MetricKey.new_bugs })
      })
    });

    expect(qgStatus1).toEqual(
      expect.objectContaining({
        name: 'Foo',
        key: 'foo',
        status: 'ERROR'
      })
    );

    const { failedConditions: failedConditions2 } = qgStatus2;
    expect(failedConditions2).toHaveLength(1);
    expect(failedConditions2[0]).toMatchObject({
      actual: '15',
      level: 'ERROR',
      metric: MetricKey.new_bugs,
      measure: expect.objectContaining({
        metric: expect.objectContaining({ key: MetricKey.new_bugs })
      })
    });
  });

  it('should correctly flag an application as empty', async () => {
    (getMeasuresWithPeriodAndMetrics as jest.Mock).mockResolvedValueOnce({ component: {} });

    const wrapper = shallowRender({ component });
    await waitAndUpdate(wrapper);

    expect(wrapper.find(BranchOverviewRenderer).props().projectIsEmpty).toBe(true);
  });
});

it("should correctly load a component's history", async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getProjectActivity).toBeCalled();
  expect(getAllTimeMachineData).toBeCalled();

  const { measuresHistory } = wrapper.state();
  expect(measuresHistory).toHaveLength(6);
  expect(measuresHistory![0]).toEqual(
    expect.objectContaining({
      metric: MetricKey.bugs,
      history: [{ date: 'PARSED:2019-01-05', value: '2.0' }]
    })
  );
});

it.each([
  ['no analysis', [], undefined],
  ['1 analysis, no CI data', [mockAnalysis()], false],
  ['1 analysis, no CI detected', [mockAnalysis({ detectedCI: NO_CI_DETECTED })], false],
  ['1 analysis, CI detected', [mockAnalysis({ detectedCI: 'Cirrus CI' })], true]
])(
  "should correctly flag a project that wasn't analyzed using a CI (%s)",
  async (_, analyses, expected) => {
    (getProjectActivity as jest.Mock).mockResolvedValueOnce({ analyses });

    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    expect(wrapper.state().detectedCIOnLastAnalysis).toBe(expected);
  }
);

it('should correctly handle graph type storage', () => {
  const wrapper = shallowRender();
  expect(getActivityGraph).toBeCalledWith(BRANCH_OVERVIEW_ACTIVITY_GRAPH, 'foo');
  expect(wrapper.state().graph).toBe(GraphType.coverage);

  wrapper.instance().handleGraphChange(GraphType.issues);
  expect(saveActivityGraph).toBeCalledWith(BRANCH_OVERVIEW_ACTIVITY_GRAPH, 'foo', GraphType.issues);
  expect(wrapper.state().graph).toBe(GraphType.issues);
});

function shallowRender(props: Partial<BranchOverview['props']> = {}) {
  return shallow<BranchOverview>(
    <BranchOverview
      branch={mockMainBranch()}
      component={mockComponent({
        breadcrumbs: [mockComponent({ key: 'foo' })],
        key: 'foo',
        name: 'Foo'
      })}
      {...props}
    />
  );
}
