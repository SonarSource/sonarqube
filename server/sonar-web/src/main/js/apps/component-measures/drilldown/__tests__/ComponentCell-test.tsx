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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockBranch, mockPullRequest } from '../../../../helpers/mocks/branch-like';
import {
  mockComponentMeasure,
  mockComponentMeasureEnhanced
} from '../../../../helpers/mocks/component';
import { mockMetric } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { enhanceComponent } from '../../utils';
import ComponentCell, { ComponentCellProps } from '../ComponentCell';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      rootComponent: mockComponentMeasure(false, { qualifier: ComponentQualifier.Application })
    })
  ).toMatchSnapshot('root component is application, component is on main branch');
  expect(
    shallowRender({
      rootComponent: mockComponentMeasure(false, { qualifier: ComponentQualifier.Application }),
      component: mockComponentMeasureEnhanced({ branch: 'develop' })
    })
  ).toMatchSnapshot('root component is application, component has branch');
  expect(
    shallowRender({ component: mockComponentMeasureEnhanced({ refKey: 'project-key' }) })
  ).toMatchSnapshot('ref project component');
  expect(
    shallowRender(
      {
        component: mockComponentMeasureEnhanced({
          refKey: 'project-key',
          qualifier: ComponentQualifier.Project
        }),
        branchLike: mockBranch()
      },
      MetricKey.releasability_rating
    )
  ).toMatchSnapshot('ref project component, releasability metric');
  expect(
    shallowRender(
      {
        component: mockComponentMeasureEnhanced({
          refKey: 'app-key',
          qualifier: ComponentQualifier.Application
        }),
        branchLike: mockBranch()
      },
      MetricKey.projects
    )
  ).toMatchSnapshot('ref application component, projects');
  expect(
    shallowRender(
      {
        component: mockComponentMeasureEnhanced({
          refKey: 'project-key',
          qualifier: ComponentQualifier.Project
        }),
        branchLike: mockBranch()
      },
      MetricKey.projects
    )
  ).toMatchSnapshot('ref project component, projects');
  expect(
    shallowRender(
      {
        component: mockComponentMeasureEnhanced({
          refKey: 'app-key',
          qualifier: ComponentQualifier.Application
        }),
        branchLike: mockPullRequest()
      },
      MetricKey.alert_status
    )
  ).toMatchSnapshot('ref application component, alert_status metric');
  expect(
    shallowRender(
      {
        component: mockComponentMeasureEnhanced({
          refKey: 'vw-key',
          qualifier: ComponentQualifier.Portfolio
        }),
        branchLike: mockPullRequest()
      },
      MetricKey.alert_status
    )
  ).toMatchSnapshot('ref portfolio component, alert_status metric');
  expect(
    shallowRender({
      component: mockComponentMeasureEnhanced({
        key: 'svw-bar',
        qualifier: ComponentQualifier.SubPortfolio
      })
    })
  ).toMatchSnapshot('sub-portfolio component');
});

function shallowRender(overrides: Partial<ComponentCellProps> = {}, metricKey = MetricKey.bugs) {
  const metric = mockMetric({ key: metricKey });
  const component = enhanceComponent(
    mockComponentMeasure(true, {
      measures: [{ metric: metric.key, value: '1', bestValue: false }]
    }),
    metric,
    { [metric.key]: metric }
  );

  return shallow<ComponentCellProps>(
    <ComponentCell
      component={component}
      metric={metric}
      rootComponent={mockComponentMeasure(false)}
      view="list"
      {...overrides}
    />
  );
}
