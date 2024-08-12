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
import { colors } from '../app/theme';
import { AlmKeys } from '../types/alm-settings';
import {
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../types/clean-code-taxonomy';
import {
  IssueResolution,
  IssueScope,
  IssueSeverity,
  IssueStatus,
  IssueType,
} from '../types/issues';
import { RuleType } from '../types/types';

export const SEVERITIES = Object.values(IssueSeverity);

export const IMPACT_SEVERITIES = Object.values(SoftwareImpactSeverity);

export const CLEAN_CODE_CATEGORIES = Object.values(CleanCodeAttributeCategory);

export const CLEAN_CODE_ATTRIBUTES_BY_CATEGORY = {
  [CleanCodeAttributeCategory.Consistent]: [
    CleanCodeAttribute.Conventional,
    CleanCodeAttribute.Identifiable,
    CleanCodeAttribute.Formatted,
  ],
  [CleanCodeAttributeCategory.Intentional]: [
    CleanCodeAttribute.Logical,
    CleanCodeAttribute.Clear,
    CleanCodeAttribute.Complete,
    CleanCodeAttribute.Efficient,
  ],
  [CleanCodeAttributeCategory.Adaptable]: [
    CleanCodeAttribute.Focused,
    CleanCodeAttribute.Distinct,
    CleanCodeAttribute.Modular,
    CleanCodeAttribute.Tested,
  ],
  [CleanCodeAttributeCategory.Responsible]: [
    CleanCodeAttribute.Trustworthy,
    CleanCodeAttribute.Lawful,
    CleanCodeAttribute.Respectful,
  ],
};

export const SOFTWARE_QUALITIES = Object.values(SoftwareQuality);

export const STATUSES = ['OPEN', 'CONFIRMED', 'REOPENED', 'RESOLVED', 'CLOSED'];

export const ISSUE_STATUSES = [
  IssueStatus.Open,
  IssueStatus.Accepted,
  IssueStatus.FalsePositive,
  IssueStatus.Confirmed,
  IssueStatus.Fixed,
];

export const ISSUE_TYPES: IssueType[] = [
  IssueType.Bug,
  IssueType.Vulnerability,
  IssueType.CodeSmell,
  IssueType.SecurityHotspot,
];

export const CCT_SOFTWARE_QUALITY_METRICS = [
  MetricKey.security_issues,
  MetricKey.reliability_issues,
  MetricKey.maintainability_issues,
];

export const LEAK_CCT_SOFTWARE_QUALITY_METRICS = [
  MetricKey.new_security_issues,
  MetricKey.new_reliability_issues,
  MetricKey.new_maintainability_issues,
];

export const OLD_TAXONOMY_METRICS = [
  MetricKey.vulnerabilities,
  MetricKey.bugs,
  MetricKey.code_smells,
];

export const LEAK_OLD_TAXONOMY_METRICS = [
  MetricKey.new_vulnerabilities,
  MetricKey.new_bugs,
  MetricKey.new_code_smells,
];

export const OLD_TAXONOMY_RATINGS = [
  MetricKey.releasability_rating,
  MetricKey.sqale_rating,
  MetricKey.security_rating,
  MetricKey.reliability_rating,
  MetricKey.security_review_rating,
  MetricKey.sqale_index,
  MetricKey.reliability_remediation_effort,
  MetricKey.security_remediation_effort,
  MetricKey.sqale_debt_ratio,
  MetricKey.effort_to_reach_maintainability_rating_a,
];

export const LEAK_OLD_TAXONOMY_RATINGS = [
  MetricKey.new_maintainability_rating,
  MetricKey.new_security_rating,
  MetricKey.new_reliability_rating,
  MetricKey.new_security_review_rating,
  MetricKey.new_technical_debt,
  MetricKey.new_security_remediation_effort,
  MetricKey.new_reliability_remediation_effort,
  MetricKey.new_sqale_debt_ratio,
];

export const OLD_TO_NEW_TAXONOMY_METRICS_MAP: { [key in MetricKey]?: MetricKey } = {
  [MetricKey.vulnerabilities]: MetricKey.security_issues,
  [MetricKey.bugs]: MetricKey.reliability_issues,
  [MetricKey.code_smells]: MetricKey.maintainability_issues,
};

export const RESOLUTIONS = [
  IssueResolution.Unresolved,
  IssueResolution.FalsePositive,
  IssueResolution.Fixed,
  IssueResolution.Removed,
  IssueResolution.WontFix,
];

export const SOURCE_SCOPES = [
  { scope: IssueScope.Main, qualifier: ComponentQualifier.File },
  { scope: IssueScope.Test, qualifier: ComponentQualifier.TestFile },
];

export const RULE_TYPES: RuleType[] = ['BUG', 'VULNERABILITY', 'CODE_SMELL', 'SECURITY_HOTSPOT'];

export const RULE_STATUSES = ['READY', 'BETA', 'DEPRECATED'];

export const RATING_COLORS = [
  { fill: colors.success300, fillTransparent: colors.success300a20, stroke: colors.success500 },
  {
    fill: colors.successVariant,
    fillTransparent: colors.successVarianta20,
    stroke: colors.successVariantDark,
  },
  {
    fill: colors.warningVariant,
    fillTransparent: colors.warningVarianta20,
    stroke: colors.warningVariantDark,
  },
  { fill: colors.warningAccent, fillTransparent: colors.warningAccenta20, stroke: colors.warning },
  { fill: colors.error400, fillTransparent: colors.error400a20, stroke: colors.error700 },
];

export const HIDDEN_METRICS = [
  MetricKey.open_issues,
  MetricKey.reopened_issues,
  MetricKey.high_impact_accepted_issues,
];

export const DEPRECATED_ACTIVITY_METRICS = [
  ...OLD_TAXONOMY_METRICS,
  MetricKey.blocker_violations,
  MetricKey.critical_violations,
  MetricKey.major_violations,
  MetricKey.minor_violations,
  MetricKey.info_violations,
  MetricKey.confirmed_issues,
];

export const SOFTWARE_QUALITY_RATING_METRICS_MAP: Record<string, MetricKey> = {
  [MetricKey.releasability_rating]: MetricKey.software_quality_releasability_rating,
  [MetricKey.sqale_rating]: MetricKey.software_quality_maintainability_rating,
  [MetricKey.security_rating]: MetricKey.software_quality_security_rating,
  [MetricKey.reliability_rating]: MetricKey.software_quality_reliability_rating,
  [MetricKey.security_review_rating]: MetricKey.software_quality_security_review_rating,
  [MetricKey.reliability_remediation_effort]:
    MetricKey.software_quality_reliability_remediation_effort,
  [MetricKey.security_remediation_effort]: MetricKey.software_quality_security_remediation_effort,
  [MetricKey.sqale_index]: MetricKey.software_quality_maintainability_remediation_effort,
  [MetricKey.sqale_debt_ratio]: MetricKey.software_quality_maintainability_debt_ratio,
  [MetricKey.effort_to_reach_maintainability_rating_a]:
    MetricKey.effort_to_reach_software_quality_maintainability_rating_a,
  [MetricKey.new_maintainability_rating]: MetricKey.new_software_quality_maintainability_rating,
  [MetricKey.new_security_rating]: MetricKey.new_software_quality_security_rating,
  [MetricKey.new_reliability_rating]: MetricKey.new_software_quality_reliability_rating,
  [MetricKey.new_security_review_rating]: MetricKey.new_software_quality_security_review_rating,
  [MetricKey.new_technical_debt]: MetricKey.new_software_quality_maintainability_remediation_effort,
  [MetricKey.new_reliability_remediation_effort]:
    MetricKey.new_software_quality_reliability_remediation_effort,
  [MetricKey.new_security_remediation_effort]:
    MetricKey.new_software_quality_security_remediation_effort,
  [MetricKey.new_sqale_debt_ratio]: MetricKey.new_software_quality_maintainability_debt_ratio,
};

export const SOFTWARE_QUALITY_RATING_METRICS = [
  MetricKey.software_quality_releasability_rating,
  MetricKey.software_quality_maintainability_rating,
  MetricKey.software_quality_security_rating,
  MetricKey.software_quality_reliability_rating,
  MetricKey.software_quality_security_review_rating,
  MetricKey.software_quality_maintainability_remediation_effort,
  MetricKey.software_quality_reliability_remediation_effort,
  MetricKey.software_quality_security_remediation_effort,
  MetricKey.software_quality_maintainability_debt_ratio,
  MetricKey.effort_to_reach_software_quality_maintainability_rating_a,
  MetricKey.new_software_quality_maintainability_rating,
  MetricKey.new_software_quality_security_rating,
  MetricKey.new_software_quality_reliability_rating,
  MetricKey.new_software_quality_security_review_rating,
  MetricKey.new_software_quality_maintainability_remediation_effort,
  MetricKey.new_software_quality_reliability_remediation_effort,
  MetricKey.new_software_quality_security_remediation_effort,
  MetricKey.new_software_quality_maintainability_debt_ratio,
];

export const PROJECT_KEY_MAX_LEN = 400;

export const IMPORT_COMPATIBLE_ALMS = [
  AlmKeys.Azure,
  AlmKeys.BitbucketServer,
  AlmKeys.BitbucketCloud,
  AlmKeys.GitHub,
  AlmKeys.GitLab,
];

export const GRADLE_SCANNER_VERSION = '5.0.0.4638';

export const ONE_SECOND = 1000;
