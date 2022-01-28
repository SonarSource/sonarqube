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
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLocation, mockMetric, mockRouter } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import ProjectActivityAppContainer from '../ProjectActivityAppContainer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should filter metric correctly', () => {
  const wrapper = shallowRender();
  let metrics = wrapper
    .instance()
    .filterMetrics(mockComponent({ qualifier: ComponentQualifier.Project }), [
      mockMetric({ key: MetricKey.bugs }),
      mockMetric({ key: MetricKey.security_review_rating })
    ]);
  expect(metrics).toHaveLength(1);
  metrics = wrapper
    .instance()
    .filterMetrics(mockComponent({ qualifier: ComponentQualifier.Portfolio }), [
      mockMetric({ key: MetricKey.bugs }),
      mockMetric({ key: MetricKey.security_hotspots_reviewed })
    ]);
  expect(metrics).toHaveLength(1);
});

function shallowRender(props: Partial<ProjectActivityAppContainer['props']> = {}) {
  return shallow<ProjectActivityAppContainer>(
    <ProjectActivityAppContainer
      component={mockComponent({ breadcrumbs: [mockComponent()] })}
      location={mockLocation()}
      router={mockRouter()}
      {...props}
    />
  );
}
