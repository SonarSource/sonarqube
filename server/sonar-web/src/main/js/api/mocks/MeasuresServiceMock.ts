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
import { mockPeriod } from '../../helpers/testMocks';
import { BranchParameters } from '../../types/branch-like';
import { Period } from '../../types/types';
import { getMeasures, getMeasuresWithPeriod } from '../measures';
import { ComponentTree, mockFullComponentTree } from './data/components';
import { mockIssuesList } from './data/issues';
import { MeasureRecords, mockFullMeasureData } from './data/measures';

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
    jest.mocked(getMeasuresWithPeriod).mockImplementation(this.handleGetMeasuresWithPeriod);
  }

  registerComponentMeasures = (measures: MeasureRecords) => {
    this.#measures = measures;
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

  handleGetMeasuresWithPeriod = (
    component: string,
    metrics: string[],
    _branchParameters?: BranchParameters,
  ) => {
    const entry = this.findComponentTree(component);
    const measures = this.filterMeasures(entry.component.key, metrics);

    return this.reply({
      component: {
        ...entry.component,
        measures,
      },
      period: this.#period,
    });
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
