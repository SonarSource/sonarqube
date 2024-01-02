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
import { ComponentQualifier } from '../../types/component';
import { Task, TaskStatuses, TaskTypes, TaskWarning } from '../../types/tasks';

export function mockTask(overrides: Partial<Task> = {}): Task {
  return {
    analysisId: 'x123',
    componentKey: 'foo',
    componentName: 'Foo',
    componentQualifier: ComponentQualifier.Project,
    id: 'AXR8jg_0mF2ZsYr8Wzs2',
    status: TaskStatuses.Pending,
    submittedAt: '2020-09-11T11:45:35+0200',
    type: TaskTypes.Report,
    ...overrides,
  };
}

export function mockTaskWarning(overrides: Partial<TaskWarning> = {}): TaskWarning {
  return {
    key: 'foo',
    message: 'Lorem ipsum',
    dismissable: false,
    ...overrides,
  };
}
