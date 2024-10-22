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

import { cloneDeep } from 'lodash';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { mockMetric, mockPeriod } from '../../helpers/testMocks';
import { Metric, Period } from '../../types/types';
import { getMeasures, getMeasuresWithPeriodAndMetrics } from '../measures';
import { ComponentTree, mockFullComponentTree } from './data/components';
import { mockIssuesList } from './data/issues';
import { MeasureRecords, getMetricTypeFromKey, mockFullMeasureData } from './data/measures';

jest.mock('../measures');

const defaultComponents = mockFullComponentTree();
const defaultMeasures = mockFullMeasureData(defaultComponents, mockIssuesList());
const defaultPeriod = mockPeriod();

export class MeasuresServiceMock {
  #components: ComponentTree;
  #measures: MeasureRecords;
  #period: Period;
  reset: () => void;

  constructor(components?: ComponentTree, measures?: MeasureRecords, period?: Period) {
    this.#components = components ?? cloneDeep(defaultComponents);
    this.#measures = measures ?? cloneDeep(defaultMeasures);
    this.#period = period ?? cloneDeep(defaultPeriod);

    this.reset = () => {
      this.#components = components ?? cloneDeep(defaultComponents);
      this.#measures = measures ?? cloneDeep(defaultMeasures);
      this.#period = period ?? cloneDeep(defaultPeriod);
    };

    jest.mocked(getMeasures).mockImplementation(this.handleGetMeasures);
    jest
      .mocked(getMeasuresWithPeriodAndMetrics)
      .mockImplementation(this.handleGetMeasuresWithPeriodAndMetrics);
  }

  registerComponentMeasures = (measures: MeasureRecords) => {
    this.#measures = measures;
  };

  deleteComponentMeasure = (componentKey: string, measureKey: MetricKey) => {
    delete this.#measures[componentKey][measureKey];
  };

  getComponentMeasures = () => {
    return this.#measures;
  };

  setComponents = (components: ComponentTree) => {
    this.#components = components;
  };

  findComponentTree = (key: string, from?: ComponentTree): ComponentTree => {
    const recurse = (node: ComponentTree): ComponentTree | undefined => {
      if (node.component.key === key) {
        return node;
      }
      return node.children.find((child) => recurse(child));
    };

    const tree = recurse(from ?? this.#components);
    if (!tree) {
      throw new Error(`Couldn't find component tree for key ${key}`);
    }

    return tree;
  };

  filterMeasures = (componentKey: string, metricKeys: string[]) => {
    return this.#measures[componentKey]
      ? Object.values(this.#measures[componentKey]).filter(({ metric }) =>
          metricKeys.includes(metric),
        )
      : [];
  };

  handleGetMeasures = ({
    component,
    metricKeys,
  }: { component: string; metricKeys: string } & BranchParameters) => {
    const entry = this.findComponentTree(component);
    const measures = this.filterMeasures(entry.component.key, metricKeys.split(','));

    return this.reply(measures);
  };

  handleGetMeasuresWithPeriodAndMetrics = (componentKey: string, metricKeys: string[]) => {
    const { component } = this.findComponentTree(componentKey);
    const measures = this.filterMeasures(component.key, metricKeys);

    const metrics: Metric[] = measures.map((measure) =>
      mockMetric({
        key: measure.metric,
        name: measure.metric,
        type: getMetricTypeFromKey(measure.metric),
      }),
    );

    return this.reply({
      component: {
        ...component,
        measures,
      },
      period: this.#period,
      metrics,
    });
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
