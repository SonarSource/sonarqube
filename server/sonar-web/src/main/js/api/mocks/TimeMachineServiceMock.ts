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
import { chunk, cloneDeep, times } from 'lodash';
import { parseDate } from '../../helpers/dates';
import { mockHistoryItem, mockMeasureHistory } from '../../helpers/mocks/project-activity';
import { BranchParameters } from '../../types/branch-like';
import { MetricKey } from '../../types/metrics';
import { MeasureHistory } from '../../types/project-activity';
import { getAllTimeMachineData, getTimeMachineData, TimeMachineResponse } from '../time-machine';

const PAGE_SIZE = 10;
const DEFAULT_PAGE = 0;
const HISTORY_COUNT = 10;
const START_DATE = '2016-01-01T00:00:00.000Z';

const defaultMeasureHistory = [
  MetricKey.bugs,
  MetricKey.code_smells,
  MetricKey.confirmed_issues,
  MetricKey.vulnerabilities,
  MetricKey.blocker_violations,
  MetricKey.lines_to_cover,
  MetricKey.uncovered_lines,
  MetricKey.security_hotspots_reviewed,
  MetricKey.coverage,
  MetricKey.duplicated_lines_density,
  MetricKey.test_success_density,
].map((metric) => {
  return mockMeasureHistory({
    metric,
    history: times(HISTORY_COUNT, (i) => {
      const date = parseDate(START_DATE);
      date.setDate(date.getDate() + i);
      return mockHistoryItem({ value: i.toString(), date });
    }),
  });
});

export class TimeMachineServiceMock {
  #measureHistory: MeasureHistory[];

  constructor() {
    this.#measureHistory = cloneDeep(defaultMeasureHistory);

    jest.mocked(getTimeMachineData).mockImplementation(this.handleGetTimeMachineData);
    jest.mocked(getAllTimeMachineData).mockImplementation(this.handleGetAllTimeMachineData);
  }

  handleGetTimeMachineData = (
    data: {
      component: string;
      from?: string;
      metrics: string;
      p?: number;
      ps?: number;
      to?: string;
    } & BranchParameters,
  ) => {
    const { ps = PAGE_SIZE, p = DEFAULT_PAGE } = data;

    const measureHistoryChunked = chunk(this.#measureHistory, ps);

    return this.reply({
      paging: { pageSize: ps, total: this.#measureHistory.length, pageIndex: p },
      measures: measureHistoryChunked[p - 1] ? this.map(measureHistoryChunked[p - 1]) : [],
    });
  };

  handleGetAllTimeMachineData = (
    data: {
      component: string;
      metrics: string;
      from?: string;
      p?: number;
      to?: string;
    } & BranchParameters,
    _prev?: TimeMachineResponse,
  ) => {
    const { p = DEFAULT_PAGE } = data;
    return this.reply({
      paging: { pageSize: PAGE_SIZE, total: this.#measureHistory.length, pageIndex: p },
      measures: this.map(this.#measureHistory),
    });
  };

  setMeasureHistory = (list: MeasureHistory[]) => {
    this.#measureHistory = list;
  };

  map = (list: MeasureHistory[]) => {
    return list.map((item) => ({
      ...item,
      history: item.history.map((h) => ({ ...h, date: h.date.toDateString() })),
    }));
  };

  reset = () => {
    this.#measureHistory = cloneDeep(defaultMeasureHistory);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
