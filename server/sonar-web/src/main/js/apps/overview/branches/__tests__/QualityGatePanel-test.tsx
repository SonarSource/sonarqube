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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockComponent } from '../../../../helpers/mocks/component';
import {
  mockQualityGateStatus,
  mockQualityGateStatusConditionEnhanced,
} from '../../../../helpers/mocks/quality-gates';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { QualityGatePanel, QualityGatePanelProps } from '../QualityGatePanel';

it('should render correctly for projects', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({ qgStatuses: [mockQualityGateStatus({ status: 'OK', failedConditions: [] })] })
  ).toMatchSnapshot();

  const wrapper = shallowRender({
    qgStatuses: [mockQualityGateStatus({ ignoredConditions: true })],
  });
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for applications', () => {
  expect(
    shallowRender({
      component: mockComponent({ qualifier: ComponentQualifier.Application }),
      qgStatuses: [
        mockQualityGateStatus(),
        mockQualityGateStatus({
          failedConditions: [
            mockQualityGateStatusConditionEnhanced(),
            mockQualityGateStatusConditionEnhanced({
              measure: mockMeasureEnhanced({
                metric: mockMetric({ key: MetricKey.new_code_smells }),
              }),
              metric: MetricKey.new_code_smells,
            }),
          ],
        }),
      ],
    })
  ).toMatchSnapshot();

  const wrapper = shallowRender({
    component: mockComponent({ qualifier: ComponentQualifier.Application }),
    qgStatuses: [
      mockQualityGateStatus(),
      mockQualityGateStatus({
        status: 'OK',
        failedConditions: [],
      }),
    ],
  });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<QualityGatePanelProps> = {}) {
  return shallow(
    <QualityGatePanel
      component={mockComponent()}
      qgStatuses={[mockQualityGateStatus()]}
      {...props}
    />
  );
}
