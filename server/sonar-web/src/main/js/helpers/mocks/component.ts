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
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { TreeComponent, Visibility } from '../../types/component';
import { Component, ComponentMeasure, ComponentMeasureEnhanced } from '../../types/types';
import { mockMeasureEnhanced } from '../testMocks';

export function mockComponent(overrides: Partial<Component> = {}): Component {
  return {
    breadcrumbs: [],
    key: 'my-project',
    name: 'MyProject',
    qualifier: ComponentQualifier.Project,
    qualityGate: { isDefault: true, key: '30', name: 'Sonar way' },
    qualityProfiles: [
      {
        deleted: false,
        key: 'my-qp',
        language: 'ts',
        name: 'Sonar way',
      },
    ],
    tags: [],
    ...overrides,
  };
}

export function mockTreeComponent(overrides: Partial<TreeComponent>): TreeComponent {
  return {
    key: 'my-key',
    qualifier: ComponentQualifier.Project,
    name: 'component',
    visibility: Visibility.Public,
    ...overrides,
  };
}

export function mockComponentMeasure(
  file = false,
  overrides: Partial<ComponentMeasure> = {},
): ComponentMeasure {
  if (file) {
    return {
      key: 'foo:src/index.tsx',
      name: 'index.tsx',
      qualifier: ComponentQualifier.File,
      path: 'src/index.tsx',
      measures: [{ metric: MetricKey.bugs, value: '1', bestValue: false }],
      ...overrides,
    };
  }
  return {
    key: 'foo',
    name: 'Foo',
    qualifier: ComponentQualifier.Project,
    measures: [{ metric: MetricKey.bugs, value: '12', bestValue: false }],
    ...overrides,
  };
}

export function mockComponentMeasureEnhanced(
  overrides: Partial<ComponentMeasureEnhanced> = {},
): ComponentMeasureEnhanced {
  return {
    ...mockComponentMeasure(false, overrides as ComponentMeasure),
    leak: undefined,
    measures: [mockMeasureEnhanced()],
    value: undefined,
    ...overrides,
  };
}
