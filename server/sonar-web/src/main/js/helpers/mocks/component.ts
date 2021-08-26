/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { ComponentQualifier, TreeComponent } from '../../types/component';
import { MetricKey } from '../../types/metrics';
import { mockMeasureEnhanced } from '../testMocks';

export function mockComponent(overrides: Partial<T.Component> = {}): T.Component {
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
        name: 'Sonar way'
      }
    ],
    tags: [],
    ...overrides
  };
}

export function mockTreeComponent(overrides: Partial<TreeComponent>): TreeComponent {
  return {
    key: 'my-key',
    qualifier: ComponentQualifier.Project,
    name: 'component',
    visibility: 'public',
    ...overrides
  };
}

export function mockComponentMeasure(
  file = false,
  overrides: Partial<T.ComponentMeasure> = {}
): T.ComponentMeasure {
  if (file) {
    return {
      key: 'foo:src/index.tsx',
      name: 'index.tsx',
      qualifier: ComponentQualifier.File,
      path: 'src/index.tsx',
      measures: [{ metric: MetricKey.bugs, value: '1', bestValue: false }],
      ...overrides
    };
  }
  return {
    key: 'foo',
    name: 'Foo',
    qualifier: ComponentQualifier.Project,
    measures: [{ metric: MetricKey.bugs, value: '12', bestValue: false }],
    ...overrides
  };
}

export function mockComponentMeasureEnhanced(
  overrides: Partial<T.ComponentMeasureEnhanced> = {}
): T.ComponentMeasureEnhanced {
  return {
    ...mockComponentMeasure(false, overrides as T.ComponentMeasure),
    leak: undefined,
    measures: [mockMeasureEnhanced()],
    value: undefined,
    ...overrides
  };
}
