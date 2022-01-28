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
import { shallow } from 'enzyme';
import * as React from 'react';
import { Link } from 'react-router';
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
});

it.each([
  [ComponentQualifier.Project, undefined],
  [ComponentQualifier.Project, 'develop'],
  [ComponentQualifier.Application, undefined],
  [ComponentQualifier.Application, 'develop'],
  [ComponentQualifier.Portfolio, undefined],
  [ComponentQualifier.Portfolio, 'develop']
])(
  'should render correctly for a "%s" root component and a component with branch "%s"',
  (rootComponentQualifier: ComponentQualifier, componentBranch: string | undefined) => {
    expect(
      shallowRender({
        rootComponent: mockComponentMeasure(false, { qualifier: rootComponentQualifier }),
        component: mockComponentMeasureEnhanced({ branch: componentBranch })
      })
    ).toMatchSnapshot();
  }
);

it('should properly deal with key and refKey', () => {
  expect(
    shallowRender({
      component: mockComponentMeasureEnhanced({
        qualifier: ComponentQualifier.SubPortfolio,
        refKey: 'port-key'
      })
    })
      .find(Link)
      .props().to
  ).toEqual(expect.objectContaining({ query: expect.objectContaining({ id: 'port-key' }) }));

  expect(
    shallowRender()
      .find(Link)
      .props().to
  ).toEqual(
    expect.objectContaining({
      query: expect.objectContaining({ id: 'foo', selected: 'foo:src/index.tsx' })
    })
  );
});

it.each([
  [
    ComponentQualifier.File,
    MetricKey.bugs,
    expect.objectContaining({
      pathname: '/component_measures',
      query: expect.objectContaining({ branch: 'develop' })
    })
  ],
  [
    ComponentQualifier.Directory,
    MetricKey.bugs,
    expect.objectContaining({
      pathname: '/component_measures',
      query: expect.objectContaining({ branch: 'develop' })
    })
  ],
  [
    ComponentQualifier.Project,
    MetricKey.projects,
    expect.objectContaining({
      pathname: '/dashboard',
      query: expect.objectContaining({ branch: 'develop' })
    })
  ],
  [
    ComponentQualifier.Application,
    MetricKey.releasability_rating,
    expect.objectContaining({
      pathname: '/dashboard',
      query: expect.objectContaining({ branch: 'develop' })
    })
  ],
  [
    ComponentQualifier.Project,
    MetricKey.releasability_rating,
    expect.objectContaining({
      pathname: '/dashboard',
      query: expect.objectContaining({ branch: 'develop' })
    })
  ],
  [
    ComponentQualifier.Application,
    MetricKey.alert_status,
    expect.objectContaining({
      pathname: '/dashboard',
      query: expect.objectContaining({ branch: 'develop' })
    })
  ],
  [
    ComponentQualifier.Project,
    MetricKey.alert_status,
    expect.objectContaining({
      pathname: '/dashboard',
      query: expect.objectContaining({ branch: 'develop' })
    })
  ]
])(
  'should display the proper link path for %s component qualifier and %s metric key',
  (componentQualifier: ComponentQualifier, metricKey: MetricKey, expectedTo: any) => {
    const wrapper = shallowRender(
      {
        component: mockComponentMeasureEnhanced({
          qualifier: componentQualifier,
          branch: 'develop'
        })
      },
      metricKey
    );

    expect(wrapper.find(Link).props().to).toEqual(expectedTo);
  }
);

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
