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
import { screen } from '@testing-library/react';
import * as React from 'react';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { Status } from '~sonar-aligned/types/common';
import { MetricKey } from '~sonar-aligned/types/metrics';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { mockQualityGate, mockQualityGateStatus } from '../../../../helpers/mocks/quality-gates';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { CaycStatus } from '../../../../types/types';
import { CurrentUser, NoticeType } from '../../../../types/users';
import QualityGatePanelSection, { QualityGatePanelSectionProps } from '../QualityGatePanelSection';

const failedConditions = [
  {
    level: 'ERROR' as Status,
    measure: {
      metric: {
        id: 'metricId1',
        key: MetricKey.new_coverage,
        name: 'metricName1',
        type: 'metricType1',
      },
    },
    metric: MetricKey.new_coverage,
    op: 'op1',
  },
  {
    level: 'ERROR' as Status,
    measure: {
      metric: {
        id: 'metricId2',
        key: MetricKey.security_hotspots,
        name: 'metricName2',
        type: 'metricType2',
      },
    },
    metric: MetricKey.security_hotspots,
    op: 'op2',
  },
  {
    level: 'ERROR' as Status,
    measure: {
      metric: {
        id: 'metricId2',
        key: MetricKey.new_violations,
        name: 'metricName2',
        type: 'metricType2',
      },
    },
    metric: MetricKey.new_violations,
    op: 'op2',
  },
];

const qgStatus = mockQualityGateStatus({
  caycStatus: CaycStatus.Compliant,
  failedConditions,
  key: 'qgStatusKey',
  name: 'qgStatusName',
  status: 'ERROR' as Status,
});

it('should render correctly for an application with 1 new code condition and 1 overall code condition', async () => {
  renderQualityGatePanelSection();

  expect(await screen.findByText('quality_gates.conditions.new_code_x.2')).toBeInTheDocument();
  expect(await screen.findByText('quality_gates.conditions.overall_code_1')).toBeInTheDocument();
});

it('should render correctly for a project with 1 new code condition', () => {
  renderQualityGatePanelSection({
    isApplication: false,
    qgStatus: { ...qgStatus, failedConditions: [failedConditions[0]] },
  });

  expect(screen.queryByText('quality_gates.conditions.new_code_1')).not.toBeInTheDocument();
  expect(screen.queryByText('quality_gates.conditions.overall_code_1')).not.toBeInTheDocument();
});

it('should render correctly 0 New issues onboarding', async () => {
  renderQualityGatePanelSection({
    isApplication: false,
    qgStatus: { ...qgStatus, failedConditions: [failedConditions[2]] },
    qualityGate: mockQualityGate({ isBuiltIn: true }),
  });

  expect(screen.queryByText('quality_gates.conditions.new_code_1')).not.toBeInTheDocument();
  expect(await byRole('alertdialog').find()).toBeInTheDocument();
});

it('should not render 0 New issues onboarding for user who dismissed it', async () => {
  renderQualityGatePanelSection(
    {
      isApplication: false,
      qgStatus: { ...qgStatus, failedConditions: [failedConditions[2]] },
      qualityGate: mockQualityGate({ isBuiltIn: true }),
    },
    mockLoggedInUser({
      dismissedNotices: { [NoticeType.OVERVIEW_ZERO_NEW_ISSUES_SIMPLIFICATION]: true },
    }),
  );

  expect(screen.queryByText('quality_gates.conditions.new_code_1')).not.toBeInTheDocument();
  expect(await byRole('alertdialog').query()).not.toBeInTheDocument();
});

function renderQualityGatePanelSection(
  props: Partial<QualityGatePanelSectionProps> = {},
  currentUser: CurrentUser = mockLoggedInUser(),
) {
  return renderComponent(
    <CurrentUserContextProvider currentUser={currentUser}>
      <QualityGatePanelSection isApplication qgStatus={qgStatus} {...props} />
    </CurrentUserContextProvider>,
  );
}
