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
import { differenceInMilliseconds, isAfter, isBefore } from 'date-fns';
import { cloneDeep, groupBy, sortBy } from 'lodash';
import { PAGE_SIZE } from '../../apps/background-tasks/constants';
import { parseDate } from '../../helpers/dates';
import { mockTask } from '../../helpers/mocks/tasks';
import { ActivityRequestParameters, Task, TaskStatuses, TaskTypes } from '../../types/tasks';
import {
  cancelAllTasks,
  cancelTask,
  getActivity,
  getStatus,
  getTypes,
  getWorkers,
  setWorkerCount,
} from '../ce';

const RANDOM_RADIX = 36;
const RANDOM_PREFIX = 2;

const TASK_TYPES = [
  TaskTypes.Report,
  TaskTypes.IssueSync,
  TaskTypes.AuditPurge,
  TaskTypes.ProjectExport,
  TaskTypes.AppRefresh,
  TaskTypes.ProjectImport,
  TaskTypes.ViewRefresh,
  TaskTypes.ReportSubmit,
];

const DEFAULT_TASKS: Task[] = [mockTask()];
const DEFAULT_WORKERS = {
  canSetWorkerCount: true,
  value: 2,
};

const CANCELABLE_TASK_STATUSES = [TaskStatuses.Pending];

export default class ComputeEngineServiceMock {
  tasks: Task[];
  workers = { ...DEFAULT_WORKERS };

  constructor() {
    (cancelAllTasks as jest.Mock).mockImplementation(this.handleCancelAllTasks);
    (cancelTask as jest.Mock).mockImplementation(this.handleCancelTask);
    (getActivity as jest.Mock).mockImplementation(this.handleGetActivity);
    (getStatus as jest.Mock).mockImplementation(this.handleGetStatus);
    (getTypes as jest.Mock).mockImplementation(this.handleGetTypes);
    (getWorkers as jest.Mock).mockImplementation(this.handleGetWorkers);
    (setWorkerCount as jest.Mock).mockImplementation(this.handleSetWorkerCount);

    this.tasks = cloneDeep(DEFAULT_TASKS);
  }

  handleCancelAllTasks = () => {
    this.tasks.forEach((t) => {
      if (CANCELABLE_TASK_STATUSES.includes(t.status)) {
        t.status = TaskStatuses.Canceled;
      }
    });

    return Promise.resolve();
  };

  handleCancelTask = (id: string) => {
    const task = this.tasks.find((t) => t.id === id);

    if (task && CANCELABLE_TASK_STATUSES.includes(task.status)) {
      task.status = TaskStatuses.Canceled;
      return Promise.resolve(task);
    }

    return Promise.reject();
  };

  handleGetActivity = (data: ActivityRequestParameters) => {
    let results = cloneDeep(this.tasks);

    results = results.filter((task) => {
      return !(
        (data.component && task.componentKey !== data.component) ||
        (data.status && !data.status.split(',').includes(task.status)) ||
        (data.type && task.type !== data.type) ||
        (data.minSubmittedAt &&
          isBefore(parseDate(task.submittedAt), parseDate(data.minSubmittedAt))) ||
        (data.maxExecutedAt &&
          (!task.executedAt ||
            isAfter(parseDate(task.executedAt), parseDate(data.maxExecutedAt)))) ||
        (data.q &&
          !task.id.includes(data.q) &&
          !task.componentName?.includes(data.q) &&
          !task.componentKey?.includes(data.q))
      );
    });

    if (data.onlyCurrents) {
      /*
       *  This is more complex in real life, but it's a good enough approximation to suit tests
       */
      results = Object.values(groupBy(results, (t) => t.componentKey)).map(
        (tasks) => sortBy(tasks, (t) => t.executedAt).pop()!
      );
    }

    const page = data.p ?? 1;
    const paginationIndex = (page - 1) * PAGE_SIZE;

    return Promise.resolve({
      tasks: results.slice(paginationIndex, paginationIndex + PAGE_SIZE),
      paging: {
        pageIndex: page,
        pageSize: PAGE_SIZE,
        total: results.length,
      },
    });
  };

  handleGetStatus = (component?: string) => {
    return Promise.resolve(
      this.tasks
        .filter((task) => !component || task.componentKey === component)
        .reduce(
          (stats, task) => {
            switch (task.status) {
              case TaskStatuses.Failed:
                stats.failing += 1;
                break;
              case TaskStatuses.InProgress:
                stats.inProgress += 1;
                break;
              case TaskStatuses.Pending:
                stats.pendingTime = Math.max(
                  stats.pendingTime,
                  differenceInMilliseconds(parseDate(task.submittedAt), Date.now())
                );
                stats.pending += 1;
                break;
            }

            return stats;
          },
          { failing: 0, inProgress: 0, pending: 0, pendingTime: 0 }
        )
    );
  };

  handleGetTypes = () => Promise.resolve([...TASK_TYPES]);

  handleGetWorkers = () => Promise.resolve({ ...this.workers });

  handleSetWorkerCount = (count: number) => {
    this.workers.value = count;
    return Promise.resolve();
  };

  /*
   * Helpers
   */

  reset() {
    this.tasks = cloneDeep(DEFAULT_TASKS);
    this.workers = { ...DEFAULT_WORKERS };
  }

  toggleCanSetWorkerCount = (flag?: boolean) => {
    this.workers.canSetWorkerCount = flag ?? !this.workers.canSetWorkerCount;
  };

  addTask = (overrides: Partial<Task> = {}) => {
    const id = Math.random().toString(RANDOM_RADIX).slice(RANDOM_PREFIX);

    this.tasks.push(
      mockTask({
        id,
        ...overrides,
      })
    );
  };

  createTasks = (count: number, status = TaskStatuses.Success) => {
    for (let i = 0; i < count; i++) {
      this.addTask({ status });
    }
  };

  clearTasks = () => {
    this.tasks = [];
  };
}
