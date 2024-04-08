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

/* eslint-disable local-rules/use-metrickey-enum */

import { AlmKeys } from '../../../types/alm-settings';
import { DopSetting, ProjectBinding } from '../../../types/dop-translation';

export function mockDopSetting(overrides?: Partial<DopSetting>): DopSetting {
  return {
    id: overrides?.id ?? overrides?.key ?? 'dop-setting-test-id',
    key: 'Test/DopSetting',
    type: AlmKeys.GitHub,
    url: 'https://github.com',
    ...overrides,
  };
}

export function mockProjectBinding(overrides?: Partial<ProjectBinding>): ProjectBinding {
  return {
    dopSetting: 'dop-setting-test-id',
    id: 'project-binding-test-id',
    projectId: 'project-id',
    projectKey: 'project-key',
    repository: 'repository',
    slug: 'Slug/Project',
    ...overrides,
  };
}
