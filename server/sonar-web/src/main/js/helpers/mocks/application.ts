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
import { Application, ApplicationPeriod, ApplicationProject } from '../../types/application';
import { Visibility } from '../../types/component';
import { mockBranch } from './branch-like';

export function mockApplication(overrides: Partial<Application> = {}): Application {
  return {
    branches: [mockBranch()],
    key: 'foo',
    name: 'Foo',
    projects: [mockApplicationProject()],
    visibility: Visibility.Private,
    ...overrides
  };
}

export function mockApplicationPeriod(
  overrides: Partial<ApplicationPeriod> = {}
): ApplicationPeriod {
  return {
    date: '2017-10-01',
    project: 'foo',
    projectName: 'Foo',
    ...overrides
  };
}

export function mockApplicationProject(
  overrides: Partial<ApplicationProject> = {}
): ApplicationProject {
  return {
    branch: 'master',
    isMain: true,
    key: 'bar',
    name: 'Bar',
    ...overrides
  };
}
