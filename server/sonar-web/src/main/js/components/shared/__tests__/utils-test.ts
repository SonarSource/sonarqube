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

import { MetricKey } from '~sonar-aligned/types/metrics';
import { SoftwareImpactSeverity, SoftwareQuality } from '../../../types/clean-code-taxonomy';
import { propsToIssueParams } from '../utils';

describe('propsToIssueParams', () => {
  it('should render correct default parameters', () => {
    expect(propsToIssueParams('other')).toEqual({ issueStatuses: 'OPEN,CONFIRMED' });
  });

  it(`should render correct params`, () => {
    expect(propsToIssueParams(MetricKey.false_positive_issues, true)).toEqual({
      inNewCodePeriod: true,
      issueStatuses: 'FALSE_POSITIVE',
    });
  });
  it.each([
    [MetricKey.software_quality_info_issues, { impactSeverities: SoftwareImpactSeverity.Info }],
    [MetricKey.software_quality_low_issues, { impactSeverities: SoftwareImpactSeverity.Low }],
    [MetricKey.software_quality_medium_issues, { impactSeverities: SoftwareImpactSeverity.Medium }],
    [MetricKey.software_quality_high_issues, { impactSeverities: SoftwareImpactSeverity.High }],
    [
      MetricKey.software_quality_blocker_issues,
      { impactSeverities: SoftwareImpactSeverity.Blocker },
    ],
    [MetricKey.new_software_quality_info_issues, { impactSeverities: SoftwareImpactSeverity.Info }],
    [MetricKey.new_software_quality_low_issues, { impactSeverities: SoftwareImpactSeverity.Low }],
    [
      MetricKey.new_software_quality_medium_issues,
      { impactSeverities: SoftwareImpactSeverity.Medium },
    ],
    [MetricKey.new_software_quality_high_issues, { impactSeverities: SoftwareImpactSeverity.High }],
    [
      MetricKey.new_software_quality_blocker_issues,
      { impactSeverities: SoftwareImpactSeverity.Blocker },
    ],
    [
      MetricKey.software_quality_reliability_issues,
      { impactSoftwareQualities: SoftwareQuality.Reliability },
    ],
    [
      MetricKey.software_quality_maintainability_issues,
      { impactSoftwareQualities: SoftwareQuality.Maintainability },
    ],
    [
      MetricKey.software_quality_security_issues,
      { impactSoftwareQualities: SoftwareQuality.Security },
    ],
    [
      MetricKey.new_software_quality_reliability_issues,
      { impactSoftwareQualities: SoftwareQuality.Reliability },
    ],
    [
      MetricKey.new_software_quality_maintainability_issues,
      { impactSoftwareQualities: SoftwareQuality.Maintainability },
    ],
    [
      MetricKey.new_software_quality_security_issues,
      { impactSoftwareQualities: SoftwareQuality.Security },
    ],
  ])(`should render correct params for %s`, (metricKey, result) => {
    expect(propsToIssueParams(metricKey)).toEqual({
      issueStatuses: 'OPEN,CONFIRMED',
      ...result,
    });
  });
});
