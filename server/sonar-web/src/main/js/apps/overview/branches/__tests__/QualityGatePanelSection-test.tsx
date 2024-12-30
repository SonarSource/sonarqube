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
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { Status } from '~sonar-aligned/types/common';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import {
  mockQualityGate,
  mockQualityGateStatus,
  mockQualityGateStatusConditionEnhanced,
} from '../../../../helpers/mocks/quality-gates';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { CaycStatus } from '../../../../types/types';
import { CurrentUser, NoticeType } from '../../../../types/users';
import QualityGatePanelSection, { QualityGatePanelSectionProps } from '../QualityGatePanelSection';

const mockCondition = (metric: MetricKey, type = MetricType.Rating) =>
  mockQualityGateStatusConditionEnhanced({
    level: 'ERROR',
    metric,
    measure: {
      metric: { id: metric, key: metric, name: metric, type },
    },
  });

const failedConditions = [
  mockCondition(MetricKey.new_coverage),
  mockCondition(MetricKey.security_hotspots),
  mockCondition(MetricKey.new_violations),
];

const qgStatus = mockQualityGateStatus({
  caycStatus: CaycStatus.Compliant,
  failedConditions,
  key: 'qgStatusKey',
  name: 'qgStatusName',
  status: 'ERROR' as Status,
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

it('should render correct links for ratings with "overall code" failed conditions', () => {
  renderQualityGatePanelSection(
    {
      isApplication: false,
      isNewCode: false,
      qgStatus: {
        ...qgStatus,
        failedConditions: [
          mockCondition(MetricKey.sqale_rating),
          mockCondition(MetricKey.reliability_rating),
          mockCondition(MetricKey.security_rating),
          mockCondition(MetricKey.software_quality_security_rating),
          mockCondition(MetricKey.software_quality_reliability_rating),
          mockCondition(MetricKey.software_quality_maintainability_rating),
        ],
      },
      qualityGate: mockQualityGate({ isBuiltIn: true }),
    },
    mockLoggedInUser({
      dismissedNotices: { [NoticeType.OVERVIEW_ZERO_NEW_ISSUES_SIMPLIFICATION]: true },
    }),
  );

  expect(byRole('link', { name: /sqale_rating/ }).get()).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&types=CODE_SMELL&id=qgStatusKey',
  );
  expect(byRole('link', { name: /reliability_rating/ }).getAt(0)).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&types=BUG&id=qgStatusKey',
  );
  expect(byRole('link', { name: /security_rating/ }).getAt(0)).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&types=VULNERABILITY&id=qgStatusKey',
  );
  expect(byRole('link', { name: /software_quality_security_rating/ }).get()).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&impactSoftwareQualities=SECURITY&id=qgStatusKey',
  );
  expect(byRole('link', { name: /software_quality_reliability_rating/ }).get()).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&impactSoftwareQualities=RELIABILITY&id=qgStatusKey',
  );
  expect(byRole('link', { name: /software_quality_maintainability_rating/ }).get()).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&impactSoftwareQualities=MAINTAINABILITY&id=qgStatusKey',
  );
});

it('should render correct links for ratings with "new code" failed conditions', () => {
  renderQualityGatePanelSection(
    {
      isApplication: false,
      qgStatus: {
        ...qgStatus,
        failedConditions: [
          mockCondition(MetricKey.new_maintainability_rating),
          mockCondition(MetricKey.new_security_rating),
          mockCondition(MetricKey.new_reliability_rating),
          mockCondition(MetricKey.new_software_quality_security_rating),
          mockCondition(MetricKey.new_software_quality_reliability_rating),
          mockCondition(MetricKey.new_software_quality_maintainability_rating),
        ],
      },
      qualityGate: mockQualityGate({ isBuiltIn: true }),
    },
    mockLoggedInUser({
      dismissedNotices: { [NoticeType.OVERVIEW_ZERO_NEW_ISSUES_SIMPLIFICATION]: true },
    }),
  );

  expect(byRole('link', { name: /new_maintainability_rating/ }).get()).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&types=CODE_SMELL&inNewCodePeriod=true&id=qgStatusKey',
  );
  expect(byRole('link', { name: /new_security_rating/ }).get()).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&types=VULNERABILITY&inNewCodePeriod=true&id=qgStatusKey',
  );
  expect(byRole('link', { name: /new_reliability_rating/ }).get()).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&types=BUG&inNewCodePeriod=true&id=qgStatusKey',
  );
  expect(byRole('link', { name: /new_software_quality_security_rating/ }).get()).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&impactSoftwareQualities=SECURITY&inNewCodePeriod=true&id=qgStatusKey',
  );
  expect(byRole('link', { name: /new_software_quality_reliability_rating/ }).get()).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&impactSoftwareQualities=RELIABILITY&inNewCodePeriod=true&id=qgStatusKey',
  );
  expect(
    byRole('link', { name: /new_software_quality_maintainability_rating/ }).get(),
  ).toHaveAttribute(
    'href',
    '/project/issues?issueStatuses=OPEN%2CCONFIRMED&impactSoftwareQualities=MAINTAINABILITY&inNewCodePeriod=true&id=qgStatusKey',
  );
});

function renderQualityGatePanelSection(
  props: Partial<QualityGatePanelSectionProps> = {},
  currentUser: CurrentUser = mockLoggedInUser(),
) {
  return renderComponent(
    <CurrentUserContextProvider currentUser={currentUser}>
      <QualityGatePanelSection isApplication qgStatus={qgStatus} isNewCode {...props} />
    </CurrentUserContextProvider>,
  );
}
