/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { colors } from '../app/theme';
import { AlmKeys } from '../types/alm-settings';
import {
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../types/clean-code-taxonomy';
import { ComponentQualifier } from '../types/component';
import { IssueResolution, IssueScope, IssueSeverity, IssueType } from '../types/issues';
import { RuleType } from '../types/types';

export const SEVERITIES = Object.values(IssueSeverity);

export const IMPACT_SEVERITIES = Object.values(SoftwareImpactSeverity);

export const CLEAN_CODE_CATEGORIES = Object.values(CleanCodeAttributeCategory);

export const SOFTWARE_QUALITIES = Object.values(SoftwareQuality);

export const STATUSES = ['OPEN', 'CONFIRMED', 'REOPENED', 'RESOLVED', 'CLOSED'];

export const ISSUE_TYPES: IssueType[] = [
  IssueType.Bug,
  IssueType.Vulnerability,
  IssueType.CodeSmell,
  IssueType.SecurityHotspot,
];

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

export const PROJECT_KEY_MAX_LEN = 400;

export const ALM_DOCUMENTATION_PATHS = {
  [AlmKeys.Azure]: '/devops-platform-integration/azure-devops-integration/',
  [AlmKeys.BitbucketServer]:
    '/devops-platform-integration/bitbucket-integration/bitbucket-server-integration/',
  [AlmKeys.BitbucketCloud]:
    '/devops-platform-integration/bitbucket-integration/bitbucket-cloud-integration/',
  [AlmKeys.GitHub]: '/devops-platform-integration/github-integration/',
  [AlmKeys.GitLab]: '/devops-platform-integration/gitlab-integration/',
};

export const IMPORT_COMPATIBLE_ALMS = [
  AlmKeys.Azure,
  AlmKeys.BitbucketServer,
  AlmKeys.BitbucketCloud,
  AlmKeys.GitHub,
  AlmKeys.GitLab,
];

export const GRADLE_SCANNER_VERSION = '4.4.1.3373';

export const ONE_SECOND = 1000;
